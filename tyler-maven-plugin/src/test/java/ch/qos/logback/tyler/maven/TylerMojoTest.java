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

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TylerMojoTest {

    static final String INPUT_DIR = "src/test/input/";

    @TempDir
    Path tempDir;

    TylerMojo mojo = new TylerMojo();

    @BeforeEach
    void setUp() {
        Build build = new Build();
        build.setOutputDirectory(tempDir.resolve("classes").toString());
        MavenProject project = new MavenProject();
        project.getModel().setBuild(build);

        mojo.project = project;
        mojo.className = "org.example.MyConfigurator";
        mojo.outputDirectory = tempDir.resolve("generated-sources").toFile();
        mojo.resourcesOutputDirectory = tempDir.resolve("generated-resources").toFile();
    }

    @Test
    void generatesConfiguratorAndServiceFile() throws Exception {
        mojo.configurationFile = new File(INPUT_DIR + "logback.xml");
        mojo.execute();

        Path javaFile = mojo.outputDirectory.toPath().resolve("org/example/MyConfigurator.java");
        String javaSource = Files.readString(javaFile, StandardCharsets.UTF_8);
        assertTrue(javaSource.contains("package org.example;"), javaSource);
        assertTrue(javaSource.contains("public class MyConfigurator extends TylerConfiguratorBase implements Configurator"), javaSource);
        assertTrue(javaSource.contains("setupAppenderCONSOLE()"), javaSource);

        Path serviceFile = mojo.resourcesOutputDirectory.toPath().resolve(TylerMojo.CONFIGURATOR_SERVICE_FILE);
        assertEquals("org.example.MyConfigurator\n", Files.readString(serviceFile, StandardCharsets.UTF_8));

        assertTrue(mojo.project.getCompileSourceRoots().contains(mojo.outputDirectory.getAbsolutePath()));
        assertTrue(mojo.project.getResources().stream()
                        .anyMatch(r -> r.getDirectory().equals(mojo.resourcesOutputDirectory.getAbsolutePath())));
    }

    @Test
    void unchangedFilesAreLeftUntouched() throws Exception {
        mojo.configurationFile = new File(INPUT_DIR + "logback.xml");
        mojo.execute();
        Path javaFile = mojo.outputDirectory.toPath().resolve("org/example/MyConfigurator.java");
        long lastModified = 1_000_000_000_000L;
        Files.setLastModifiedTime(javaFile, FileTime.fromMillis(lastModified));

        mojo.execute();
        assertEquals(lastModified, Files.getLastModifiedTime(javaFile).toMillis());
        // the resource directory is registered once
        assertEquals(1, mojo.project.getResources().stream()
                        .filter(r -> r.getDirectory().equals(mojo.resourcesOutputDirectory.getAbsolutePath())).count());
    }

    @Test
    void unknownClassFailsTheBuild() {
        mojo.configurationFile = new File(INPUT_DIR + "unknownClass.xml");
        assertThrows(MojoFailureException.class, () -> mojo.execute());
        assertTrue(Files.notExists(mojo.outputDirectory.toPath().resolve("org/example/MyConfigurator.java")));
    }

    @Test
    void warningsFailTheBuildOnlyIfRequested() throws Exception {
        mojo.configurationFile = new File(INPUT_DIR + "undeclaredRef.xml");
        mojo.execute();

        mojo.failOnWarning = true;
        assertThrows(MojoFailureException.class, () -> mojo.execute());
    }

    @Test
    void allPackagesAreAllowedByDefault() throws Exception {
        mojo.configurationFile = new File(INPUT_DIR + "customEncoder.xml");
        mojo.execute();
        String javaSource = Files.readString(mojo.outputDirectory.toPath().resolve("org/example/MyConfigurator.java"), StandardCharsets.UTF_8);
        assertTrue(javaSource.contains("CustomEncoder customEncoder = new CustomEncoder();"), javaSource);

        // Maven 4 injects an empty list if the parameter is not set
        mojo.allowedPackages = List.of();
        mojo.execute();

        mojo.allowedPackages = List.of("org.example.tyler.maven");
        mojo.execute();

        mojo.allowedPackages = List.of("com.acme");
        assertThrows(MojoFailureException.class, () -> mojo.execute());
    }

    @Test
    void classesOfTheProjectAreVisible() throws Exception {
        // a class absent from the class path of the plugin, i.e. of this test, compiled into the project
        Path sourceDir = Files.createDirectories(tempDir.resolve("src/org/example/project"));
        Path source = Files.writeString(sourceDir.resolve("ProjectEncoder.java"), """
                        package org.example.project;
                        public class ProjectEncoder extends ch.qos.logback.classic.encoder.PatternLayoutEncoder {}
                        """);
        Path classesDir = Files.createDirectories(Path.of(mojo.project.getBuild().getOutputDirectory()));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int status = compiler.run(null, null, null, "-cp", System.getProperty("java.class.path"), "-d", classesDir.toString(),
                        source.toString());
        assertEquals(0, status);

        Path configurationFile = Files.writeString(tempDir.resolve("logback.xml"), """
                        <configuration>
                          <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
                            <encoder class="org.example.project.ProjectEncoder">
                              <pattern>%msg%n</pattern>
                            </encoder>
                          </appender>
                          <root level="INFO">
                            <appender-ref ref="CONSOLE"/>
                          </root>
                        </configuration>
                        """);
        mojo.configurationFile = configurationFile.toFile();
        mojo.execute();
        String javaSource = Files.readString(mojo.outputDirectory.toPath().resolve("org/example/MyConfigurator.java"), StandardCharsets.UTF_8);
        assertTrue(javaSource.contains("ProjectEncoder projectEncoder = new ProjectEncoder();"), javaSource);
    }

    @Test
    void resourcesOfTheProjectCanBeIncluded() throws Exception {
        Path resourcesDir = Files.createDirectories(tempDir.resolve("resources"));
        Files.writeString(resourcesDir.resolve("tyler-mojo-test-included.xml"), """
                        <included>
                          <logger name="com.foo" level="WARN"/>
                        </included>
                        """);
        Resource resource = new Resource();
        resource.setDirectory(resourcesDir.toString());
        mojo.project.addResource(resource);

        Path configurationFile = Files.writeString(tempDir.resolve("logback.xml"), """
                        <configuration>
                          <include resource="tyler-mojo-test-included.xml"/>
                          <root level="INFO"/>
                        </configuration>
                        """);
        mojo.configurationFile = configurationFile.toFile();
        mojo.translationTimeInclusionEnabled = true;
        mojo.failOnWarning = true;
        mojo.execute();
        String javaSource = Files.readString(mojo.outputDirectory.toPath().resolve("org/example/MyConfigurator.java"), StandardCharsets.UTF_8);
        assertTrue(javaSource.contains("setupLogger(\"com.foo\", \"WARN\""), javaSource);
    }

    @Test
    void missingConfigurationFileFailsTheBuild() {
        mojo.configurationFile = new File(INPUT_DIR + "nonExistent.xml");
        assertThrows(MojoFailureException.class, () -> mojo.execute());
    }

    @Test
    void skip() throws Exception {
        mojo.configurationFile = new File(INPUT_DIR + "nonExistent.xml");
        mojo.skip = true;
        mojo.execute();
        assertTrue(Files.notExists(mojo.outputDirectory.toPath()));
    }
}
