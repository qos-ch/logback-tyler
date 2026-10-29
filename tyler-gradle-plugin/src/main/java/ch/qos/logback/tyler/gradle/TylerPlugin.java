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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers the <code>tyler</code> extension and, for Java projects, the
 * <code>generateTylerConfigurator</code> task. The outputs of the task are added to the main source set,
 * so that the generated configurator is compiled and the provider-configuration file packaged with the
 * project.
 *
 * @since 1.0.7
 */
public class TylerPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "tyler";
    public static final String TASK_NAME = "generateTylerConfigurator";

    static final String DEFAULT_CONFIGURATION_FILE = "src/main/resources/logback.xml";
    static final String DEFAULT_CLASS_NAME = "com.example.TylerConfigurator";

    @Override
    public void apply(Project project) {
        TylerExtension extension = project.getExtensions().create(EXTENSION_NAME, TylerExtension.class);
        extension.getConfigurationFile().convention(project.getLayout().getProjectDirectory().file(DEFAULT_CONFIGURATION_FILE));
        extension.getClassName().convention(DEFAULT_CLASS_NAME);
        extension.getTranslationTimeInclusionEnabled().convention(false);
        extension.getFailOnWarning().convention(false);

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> registerTask(project, extension));
    }

    void registerTask(Project project, TylerExtension extension) {
        SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Provider<Directory> generatedDirectory = project.getLayout().getBuildDirectory().dir("generated/tyler");
        Provider<Directory> resourceOutputDirectory = generatedDirectory.map(d -> d.dir("resources"));

        // the resource directories of the main source set, except the output of the task itself
        Provider<Set<File>> resourceDirectories = project.provider(() -> {
            File output = resourceOutputDirectory.get().getAsFile();
            return main.getResources().getSrcDirs().stream().filter(dir -> !dir.equals(output)).collect(Collectors.toSet());
        });

        TaskProvider<GenerateTylerConfigurator> task = project.getTasks().register(TASK_NAME, GenerateTylerConfigurator.class, t -> {
            t.setGroup("build");
            t.setDescription("Translates a logback.xml file into a custom configurator.");
            t.getConfigurationFile().set(extension.getConfigurationFile());
            t.getClassName().set(extension.getClassName());
            t.getSuperclassName().set(extension.getSuperclassName());
            t.getTranslationTimeInclusionEnabled().set(extension.getTranslationTimeInclusionEnabled());
            t.getAllowedPackages().set(extension.getAllowedPackages());
            t.getFailOnWarning().set(extension.getFailOnWarning());
            t.getClasspath().from(main.getCompileClasspath());
            t.getResourceDirectories().from(resourceDirectories);
            t.getSourceOutputDirectory().set(generatedDirectory.map(d -> d.dir("sources")));
            t.getResourceOutputDirectory().set(resourceOutputDirectory);
        });

        // registering the outputs as source directories makes compilation and resource processing depend on the task
        main.getJava().srcDir(task.flatMap(GenerateTylerConfigurator::getSourceOutputDirectory));
        main.getResources().srcDir(task.flatMap(GenerateTylerConfigurator::getResourceOutputDirectory));
    }
}
