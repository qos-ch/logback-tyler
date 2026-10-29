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

package ch.qos.logback.tyler.gradle;

import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.tyler.base.ConfigurationFileTranslator;
import ch.qos.logback.tyler.base.ConfigurationFileTranslator.TranslationResult;
import ch.qos.logback.tyler.base.TranslationOptions;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Translates a logback.xml file into a custom configurator, i.e. a Java class implementing
 * <code>ch.qos.logback.classic.spi.Configurator</code>, and writes the provider-configuration file
 * <code>META-INF/services/ch.qos.logback.classic.spi.Configurator</code> installing it as a service provider.
 *
 * @since 1.0.7
 */
@CacheableTask
public abstract class GenerateTylerConfigurator extends DefaultTask {

    /**
     * @see TylerExtension#getConfigurationFile()
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getConfigurationFile();

    /**
     * @see TylerExtension#getClassName()
     */
    @Input
    public abstract Property<String> getClassName();

    /**
     * @see TylerExtension#getSuperclassName()
     */
    @Input
    @Optional
    public abstract Property<String> getSuperclassName();

    /**
     * @see TylerExtension#getTranslationTimeInclusionEnabled()
     */
    @Input
    public abstract Property<Boolean> getTranslationTimeInclusionEnabled();

    /**
     * @see TylerExtension#getAllowedPackages()
     */
    @Input
    public abstract ListProperty<String> getAllowedPackages();

    /**
     * @see TylerExtension#getFailOnWarning()
     */
    @Input
    public abstract Property<Boolean> getFailOnWarning();

    /**
     * The class path in which classes referenced by the configuration file are looked up, usually the
     * compile class path of the main source set.
     */
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * Directories in which resources referenced by &lt;include resource="..."&gt; elements are looked up,
     * usually the resource directories of the main source set.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getResourceDirectories();

    /**
     * Directory where the generated configurator is written.
     */
    @OutputDirectory
    public abstract DirectoryProperty getSourceOutputDirectory();

    /**
     * Directory where the provider-configuration file is written.
     */
    @OutputDirectory
    public abstract DirectoryProperty getResourceOutputDirectory();

    @TaskAction
    public void generate() throws IOException {
        File configurationFile = getConfigurationFile().get().getAsFile();
        String className = getClassName().get();
        getLogger().info("Translating [{}] into [{}]", configurationFile, className);

        TranslationResult result = translate(configurationFile, className);

        result.warnings().forEach(w -> getLogger().warn("warning: {}", w));
        result.errors().forEach(e -> getLogger().error("error: {}", e));
        if (!result.isSuccessful()) {
            throw new GradleException("Translation of [" + configurationFile + "] failed with " + result.errors().size() + " error(s)");
        }
        if (getFailOnWarning().get() && !result.warnings().isEmpty()) {
            throw new GradleException("Translation of [" + configurationFile + "] issued " + result.warnings().size() + " warning(s)");
        }

        // files generated for a previous class name are stale
        Path sourceOutputDirectory = clean(getSourceOutputDirectory().get().getAsFile().toPath());
        Path resourceOutputDirectory = clean(getResourceOutputDirectory().get().getAsFile().toPath());
        write(sourceOutputDirectory.resolve(className.replace('.', '/') + ".java"), result.javaSource());
        write(resourceOutputDirectory.resolve(ConfigurationFileTranslator.CONFIGURATOR_SERVICE_FILE), className + "\n");
    }

    /**
     * Translate the configuration file with a class loader which sees the class path and the resource
     * directories of this task. As it delegates to the class loader of the plugin first, logback and
     * logback-tyler are those of the plugin.
     */
    TranslationResult translate(File configurationFile, String className) {
        try (URLClassLoader classLoader = new URLClassLoader(translationClassPath(), GenerateTylerConfigurator.class.getClassLoader())) {
            TranslationOptions.Builder builder = TranslationOptions.builder().generatedClassName(className)
                            .translationTimeInclusionEnabled(getTranslationTimeInclusionEnabled().get()).classLoader(classLoader);
            if (getSuperclassName().isPresent()) {
                builder.generatedClassSuperclassName(getSuperclassName().get());
            }
            List<String> allowedPackages = getAllowedPackages().get();
            if (allowedPackages.isEmpty()) {
                builder.allowAllPackages();
            } else {
                builder.allowedPackages(allowedPackages);
            }
            return new ConfigurationFileTranslator(builder.build()).translate(configurationFile);
        } catch (IllegalArgumentException e) {
            throw new GradleException("Invalid configuration of the tyler extension: " + e.getMessage(), e);
        } catch (JoranException | IOException e) {
            throw new GradleException("Failed to translate [" + configurationFile + "]", e);
        }
    }

    URL[] translationClassPath() throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        for (File file : getClasspath().plus(getResourceDirectories())) {
            urls.add(file.toURI().toURL());
        }
        return urls.toArray(new URL[0]);
    }

    static Path clean(Path directory) throws IOException {
        deleteRecursively(directory);
        return Files.createDirectories(directory);
    }

    static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
