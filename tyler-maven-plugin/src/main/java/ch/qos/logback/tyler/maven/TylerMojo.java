/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2026, QOS.ch. All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ch.qos.logback.tyler.maven;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.tyler.base.ModelToJava;
import ch.qos.logback.tyler.base.TranslationOptions;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Translates a logback.xml file into a custom configurator, i.e. a Java class implementing
 * <code>ch.qos.logback.classic.spi.Configurator</code>, and installs it as a service provider in
 * <code>META-INF/services/ch.qos.logback.classic.spi.Configurator</code>.
 *
 * <p>The generated class is added to the compile source roots of the project and the provider-configuration
 * file to its resources.</p>
 *
 * @since 1.0.7
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class TylerMojo extends AbstractMojo {

    static final String CONFIGURATOR_SERVICE_FILE = "META-INF/services/ch.qos.logback.classic.spi.Configurator";

    /**
     * The logback.xml file to translate.
     */
    @Parameter(property = "tyler.configurationFile", defaultValue = "${project.basedir}/src/main/resources/logback.xml", required = true)
    File configurationFile;

    /**
     * Fully qualified name of the generated configurator class.
     */
    @Parameter(property = "tyler.className", defaultValue = "com.example.TylerConfigurator", required = true)
    String className;

    /**
     * Fully qualified name of the superclass of the generated class. If not set,
     * <code>ch.qos.logback.classic.tyler.TylerConfiguratorBase</code> is used. The superclass must be
     * available on the compile class path of the project, i.e. in a dependency.
     */
    @Parameter(property = "tyler.superclassName")
    String superclassName;

    /**
     * Whether files referenced by &lt;include&gt; elements are inlined at translation time. Otherwise,
     * &lt;include&gt; elements are omitted with a warning.
     */
    @Parameter(property = "tyler.translationTimeInclusionEnabled", defaultValue = "false")
    boolean translationTimeInclusionEnabled;

    /**
     * Packages whose classes may be referenced by the configuration file, e.g. by appenders, encoders or
     * conditions, in addition to <code>ch.qos.logback</code> which is always allowed. As the configuration
     * file belongs to the project, all packages are allowed if this parameter is not set or empty.
     */
    @Parameter(property = "tyler.allowedPackages")
    List<String> allowedPackages;

    /**
     * Whether warnings issued during translation fail the build. Errors always do.
     */
    @Parameter(property = "tyler.failOnWarning", defaultValue = "false")
    boolean failOnWarning;

    /**
     * Directory where the generated configurator is written.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/tyler", required = true)
    File outputDirectory;

    /**
     * Directory where the provider-configuration file is written.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/tyler", required = true)
    File resourcesOutputDirectory;

    /**
     * Skips the execution of this goal.
     */
    @Parameter(property = "tyler.skip", defaultValue = "false")
    boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping logback-tyler translation");
            return;
        }
        if (!configurationFile.isFile()) {
            throw new MojoFailureException("Configuration file [" + configurationFile + "] does not exist");
        }

        getLog().info("Translating [" + configurationFile + "] into [" + className + "]");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String javaSource = translate(errors, warnings);

        warnings.forEach(w -> getLog().warn(w));
        errors.forEach(e -> getLog().error(e));
        if (!errors.isEmpty() || javaSource == null) {
            throw new MojoFailureException("Translation of [" + configurationFile + "] failed with " + errors.size() + " error(s)");
        }
        if (failOnWarning && !warnings.isEmpty()) {
            throw new MojoFailureException("Translation of [" + configurationFile + "] issued " + warnings.size() + " warning(s)");
        }

        Path javaFile = outputDirectory.toPath().resolve(className.replace('.', '/') + ".java");
        Path serviceFile = resourcesOutputDirectory.toPath().resolve(CONFIGURATOR_SERVICE_FILE);
        try {
            writeIfChanged(javaFile, javaSource);
            writeIfChanged(serviceFile, className + "\n");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write generated files", e);
        }

        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        addResourceDirectory();
    }

    /**
     * Translate the configuration file with a class loader which sees the compile class path and the
     * resource directories of the project, so that classes mentioned in the configuration file can be
     * analysed, and resources it includes can be found. As it delegates to the class loader of the plugin
     * first, logback and logback-tyler are those of the plugin.
     */
    String translate(List<String> errors, List<String> warnings) throws MojoExecutionException {
        try (URLClassLoader classLoader = new URLClassLoader(projectClassPath(), TylerMojo.class.getClassLoader())) {
            TranslationOptions.Builder builder = TranslationOptions.builder().generatedClassName(className)
                            .translationTimeInclusionEnabled(translationTimeInclusionEnabled).classLoader(classLoader);
            if (superclassName != null) {
                builder.generatedClassSuperclassName(superclassName);
            }
            // depending on the Maven version, an unset list parameter is either null or empty
            if (allowedPackages == null || allowedPackages.isEmpty()) {
                builder.allowAllPackages();
            } else {
                builder.allowedPackages(allowedPackages);
            }

            ContextBase context = new ContextBase();
            ModelToJava m2j = new ModelToJava(context, builder.build());

            String result;
            try (InputStream in = Files.newInputStream(configurationFile.toPath())) {
                InputSource inputSource = new InputSource(in);
                inputSource.setSystemId(configurationFile.toURI().toString());
                Model model = m2j.extractModel(inputSource);
                result = model == null ? null : m2j.toJava(model);
            }

            for (Status status : context.getStatusManager().getCopyOfStatusList()) {
                collect(status, errors, warnings);
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Invalid configuration of the plugin: " + e.getMessage(), e);
        } catch (JoranException | IOException e) {
            throw new MojoExecutionException("Failed to translate [" + configurationFile + "]", e);
        }
    }

    static void collect(Status status, List<String> errors, List<String> warnings) {
        String message = status.getMessage();
        if (status.getThrowable() != null) {
            message += " " + status.getThrowable();
        }
        if (status.getLevel() == Status.ERROR) {
            errors.add(message);
        } else if (status.getLevel() == Status.WARN) {
            warnings.add(message);
        }
        if (status.hasChildren()) {
            status.iterator().forEachRemaining(child -> collect(child, errors, warnings));
        }
    }

    URL[] projectClassPath() throws MojoExecutionException {
        Set<File> files = new LinkedHashSet<>();
        try {
            for (String element : project.getCompileClasspathElements()) {
                files.add(new File(element));
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Failed to resolve the compile class path", e);
        }
        for (Resource resource : project.getResources()) {
            files.add(new File(resource.getDirectory()));
        }

        List<URL> urls = new ArrayList<>();
        for (File file : files) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Invalid class path element [" + file + "]", e);
            }
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Project class path used for translation: " + urls);
        }
        return urls.toArray(new URL[0]);
    }

    void addResourceDirectory() {
        String directory = resourcesOutputDirectory.getAbsolutePath();
        for (Resource resource : project.getResources()) {
            if (directory.equals(new File(resource.getDirectory()).getAbsolutePath())) {
                return;
            }
        }
        Resource resource = new Resource();
        resource.setDirectory(directory);
        project.addResource(resource);
    }

    /**
     * Leaving unchanged files untouched avoids needless recompilation.
     */
    static void writeIfChanged(Path path, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (Files.isRegularFile(path) && Arrays.equals(Files.readAllBytes(path), bytes)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }
}
