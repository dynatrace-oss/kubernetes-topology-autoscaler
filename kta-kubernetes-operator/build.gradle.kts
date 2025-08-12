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
    id("io.quarkus")
    id("checkstyle")
    id("com.diffplug.spotless") version "7.0.4"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    val quarkusPlatformGroupId: String by project
    val quarkusPlatformArtifactId: String by project
    val quarkusPlatformVersion: String by project
    val assertjVersion: String by project
    val mockServerVersion: String by project
    val operatorFrameworkJunit5Version: String by project

    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation(enforcedPlatform("$quarkusPlatformGroupId:quarkus-operator-sdk-bom:$quarkusPlatformVersion"))
    implementation("io.quarkiverse.operatorsdk:quarkus-operator-sdk")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.fabric8:crd-generator-api-v2")
    implementation("io.quarkus:quarkus-container-image-jib")
    implementation("org.jboss.slf4j:slf4j-jboss-logmanager")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.fabric8:kube-api-test")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.mock-server:mockserver-netty-no-dependencies:$mockServerVersion")
    testImplementation("io.javaoperatorsdk:operator-framework-junit-5:$operatorFrameworkJunit5Version")
}

group = "com.dynatrace.research"
// TODO: Change on new release
version = project.findProperty("projVersion") as String? ?: "0.1.0-alpha.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
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
        target("*.gradle*")
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }

    java {
        removeUnusedImports()
        importOrder()
        cleanthat()
        palantirJavaFormat("2.69.0").style("GOOGLE").formatJavadoc(true)
        formatAnnotations()
    }
}

tasks.register("lint") {
    dependsOn("spotlessCheck", "checkstyleMain", "checkstyleTest")
}
