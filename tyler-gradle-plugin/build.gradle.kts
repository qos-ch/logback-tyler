plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "ch.qos.logback.tyler"
description = "Translates a logback.xml file into a custom Configurator installed as a service provider"

repositories {
    mavenCentral()
    // snapshots of tyler-base installed by the Maven build, i.e. "mvn install" in the parent directory
    mavenLocal {
        content {
            includeGroup("ch.qos.logback.tyler")
        }
    }
}

dependencies {
    implementation("ch.qos.logback.tyler:tyler-base:${project.version}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    website = "https://github.com/qos-ch/logback-tyler"
    vcsUrl = "https://github.com/qos-ch/logback-tyler"
    plugins {
        create("tyler") {
            id = "ch.qos.logback.tyler"
            implementationClass = "ch.qos.logback.tyler.gradle.TylerPlugin"
            displayName = "logback-tyler"
            description = project.description
            tags = listOf("logback", "logging", "configuration", "graalvm")
        }
    }
}
