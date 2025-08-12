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

version = project.findProperty("projVersion") as String? ?: "0.1.0-alpha.1-SNAPSHOT"

tasks.register("cleanAll") {
    dependsOn(gradle.includedBuild("kta-quickstart-flink").task(":clean"))
    dependsOn(gradle.includedBuild("kta-quickstart-kstreams").task(":clean"))
}

tasks.register("buildAll") {
    dependsOn(gradle.includedBuild("kta-quickstart-flink").task(":build"))
    dependsOn(gradle.includedBuild("kta-quickstart-kstreams").task(":build"))
}

tasks.register("spotlessApplyAll") {
    dependsOn(gradle.includedBuild("kta-quickstart-flink").task(":spotlessApply"))
    dependsOn(gradle.includedBuild("kta-quickstart-kstreams").task(":spotlessApply"))
}

tasks.register("shadowJar") {
    dependsOn(gradle.includedBuild("kta-quickstart-kstreams").task(":shadowJar"))
}
