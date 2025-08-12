/*
 *  Copyright (c) 2024 Dynatrace LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

plugins {
    java
    id("checkstyle")
    id("com.diffplug.spotless") version "7.0.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.dynatrace.research"
// TODO: Change on new release
version = project.findProperty("projVersion") as String? ?: "0.1.0-alpha.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val kafkaStreamsVersion: String by project

    implementation("org.apache.kafka:kafka-streams:$kafkaStreamsVersion")
    implementation("ch.qos.logback:logback-classic:1.5.18")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.compileJava {
    options.release.set(11)
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to "com.dynatrace.research.kta.example.kstreams.EntryPoint"))
    }
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}

checkstyle {
    toolVersion = "11.0.0"
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

spotless {
    format("misc") {
        target("*.gradle*", ".dockerignore")
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }

    java {
        removeUnusedImports()
        importOrder()
        cleanthat()
        palantirJavaFormat("2.69.0").apply {
            style("GOOGLE")
            formatJavadoc(true)
        }
        formatAnnotations()
    }
}
