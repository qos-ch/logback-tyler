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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs builds applying the plugin with Gradle TestKit.
 */
public class TylerPluginFunctionalTest {

    static final String TASK_PATH = ":" + TylerPlugin.TASK_NAME;

    static final String LOGBACK_XML = """
                    <configuration>
                      <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
                        <encoder>
                          <pattern>TYLER %level %msg%n</pattern>
                        </encoder>
                      </appender>
                      <root level="INFO">
                        <appender-ref ref="CONSOLE"/>
                      </root>
                    </configuration>
                    """;

    static final String MAIN_JAVA = """
                    package demo;
                    public class Main {
                        public static void main(String[] args) {
                            org.slf4j.LoggerFactory.getLogger(Main.class).info("hello from tyler");
                        }
                    }
                    """;

    @TempDir
    Path projectDir;

    @BeforeEach
    void setUp() throws IOException {
        write("settings.gradle.kts", "rootProject.name = \"sample\"\n");
    }

    String buildScript(String tylerBlock) {
        return """
                        plugins {
                            application
                            id("ch.qos.logback.tyler")
                        }
                        repositories {
                            mavenLocal()
                            mavenCentral()
                        }
                        dependencies {
                            implementation("ch.qos.logback:logback-classic:1.6.6")
                        }
                        application {
                            mainClass = "demo.Main"
                        }
                        """ + tylerBlock;
    }

    @Test
    void generatedConfiguratorIsCompiledAndUsedAtRuntime() throws IOException {
        write("build.gradle.kts", buildScript("""
                        tyler {
                            className = "org.example.MyConfigurator"
                        }
                        """));
        write("src/main/resources/logback.xml", LOGBACK_XML);
        write("src/main/java/demo/Main.java", MAIN_JAVA);

        BuildResult result = run("run");
        assertEquals(TaskOutcome.SUCCESS, result.task(TASK_PATH).getOutcome());
        assertTrue(result.getOutput().contains("TYLER INFO hello from tyler"), result.getOutput());

        assertTrue(Files.isRegularFile(projectDir.resolve("build/classes/java/main/org/example/MyConfigurator.class")));
        Path serviceFile = projectDir.resolve("build/resources/main/META-INF/services/ch.qos.logback.classic.spi.Configurator");
        assertEquals("org.example.MyConfigurator\n", Files.readString(serviceFile));
    }

    @Test
    void upToDateAndStaleFiles() throws IOException {
        write("build.gradle.kts", buildScript(""));
        write("src/main/resources/logback.xml", LOGBACK_XML);

        assertEquals(TaskOutcome.SUCCESS, run("classes").task(TASK_PATH).getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, run("classes").task(TASK_PATH).getOutcome());

        // a new class name replaces the previously generated class
        write("build.gradle.kts", buildScript("""
                        tyler {
                            className = "org.example.Renamed"
                        }
                        """));
        assertEquals(TaskOutcome.SUCCESS, run("classes").task(TASK_PATH).getOutcome());
        Path sources = projectDir.resolve("build/generated/tyler/sources");
        assertTrue(Files.isRegularFile(sources.resolve("org/example/Renamed.java")));
        assertFalse(Files.exists(sources.resolve("com/example/TylerConfigurator.java")));
    }

    @Test
    void classesOfOtherProjectsAreVisible() throws IOException {
        write("settings.gradle.kts", """
                        rootProject.name = "sample"
                        include("lib", "app")
                        """);
        write("lib/build.gradle.kts", """
                        plugins {
                            `java-library`
                        }
                        repositories {
                            mavenLocal()
                            mavenCentral()
                        }
                        dependencies {
                            api("ch.qos.logback:logback-classic:1.6.6")
                        }
                        """);
        write("lib/src/main/java/org/example/lib/LibEncoder.java", """
                        package org.example.lib;
                        public class LibEncoder extends ch.qos.logback.classic.encoder.PatternLayoutEncoder {}
                        """);
        write("app/build.gradle.kts", """
                        plugins {
                            java
                            id("ch.qos.logback.tyler")
                        }
                        repositories {
                            mavenLocal()
                            mavenCentral()
                        }
                        dependencies {
                            implementation(project(":lib"))
                        }
                        tyler {
                            allowedPackages = listOf("org.example.lib")
                        }
                        """);
        write("app/src/main/resources/logback.xml", LOGBACK_XML.replace("<encoder>", "<encoder class=\"org.example.lib.LibEncoder\">"));

        BuildResult result = run(":app:classes");
        assertEquals(TaskOutcome.SUCCESS, result.task(":app" + TASK_PATH).getOutcome());
        String javaSource = Files.readString(projectDir.resolve("app/build/generated/tyler/sources/com/example/TylerConfigurator.java"));
        assertTrue(javaSource.contains("LibEncoder libEncoder = new LibEncoder();"), javaSource);
    }

    @Test
    void classOutsideAllowedPackagesFailsTheBuild() throws IOException {
        write("build.gradle.kts", buildScript("""
                        tyler {
                            allowedPackages = listOf("com.acme")
                        }
                        """));
        write("src/main/resources/logback.xml", LOGBACK_XML.replace("<encoder>", "<encoder class=\"org.example.CustomEncoder\">"));

        BuildResult result = runner("classes").buildAndFail();
        assertTrue(result.getOutput().contains("Class [org.example.CustomEncoder] is not in an allowed package"), result.getOutput());
        assertTrue(result.getOutput().contains("failed with 1 error(s)"), result.getOutput());
    }

    @Test
    void resourcesCanBeIncluded() throws IOException {
        write("build.gradle.kts", buildScript("""
                        tyler {
                            translationTimeInclusionEnabled = true
                            failOnWarning = true
                        }
                        """));
        write("src/main/resources/included.xml", """
                        <included>
                          <logger name="com.foo" level="WARN"/>
                        </included>
                        """);
        write("src/main/resources/logback.xml", """
                        <configuration>
                          <include resource="included.xml"/>
                          <root level="INFO"/>
                        </configuration>
                        """);

        assertEquals(TaskOutcome.SUCCESS, run("classes").task(TASK_PATH).getOutcome());
        String javaSource = Files.readString(projectDir.resolve("build/generated/tyler/sources/com/example/TylerConfigurator.java"));
        assertTrue(javaSource.contains("setupLogger(\"com.foo\", \"WARN\""), javaSource);
    }

    @Test
    void missingConfigurationFileFailsTheBuild() throws IOException {
        write("build.gradle.kts", buildScript(""));

        BuildResult result = runner("classes").buildAndFail();
        assertTrue(result.getOutput().contains("logback.xml"), result.getOutput());
    }

    BuildResult run(String task) {
        return runner(task).build();
    }

    GradleRunner runner(String task) {
        List<String> arguments = new ArrayList<>(List.of(task, "--configuration-cache", "--stacktrace"));
        return GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath().withArguments(arguments).forwardOutput();
    }

    void write(String path, String content) throws IOException {
        Path file = projectDir.resolve(path);
        Files.createDirectories(file.getParent() == null ? projectDir : file.getParent());
        Files.writeString(file, content);
    }
}
