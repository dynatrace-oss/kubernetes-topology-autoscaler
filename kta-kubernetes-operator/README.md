<!--
   Copyright (c) 2024 Dynatrace LLC
  
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
       http://www.apache.org/licenses/LICENSE-2.0
  
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
  -->

# KTA Kubernetes Operator

This folder contains the source code of the KTA Kubernetes Operator.

---

## Contributor Guide

**Minimal Setup**

- Unix-like system (e.g., Linux, MacOS, Windows with WSL)
- JDK version: >= 17 (tested with 17)
- [Gradle](https://gradle.org/) (this directory contains a Gradle wrapper)

**Optional**: [Docker](https://docs.docker.com/engine/install/) (if you want to build the container image from source)

### Configuration

Configurations can be found in `gradle.properties`.

### Code Style and Static Code Analysis

We use [`spotless`](https://github.com/diffplug/spotless) and [`checkstyle`](https://checkstyle.sourceforge.io/) for code formatting and code style enforcement.

To reformat the code, use

```bash
./gradlew spotlessApply
```

To check for potential code style violations, use

```bash
./gradlew lint
```

### Testing

To run the test suite, use

```bash
./gradlew test
```

### Build from Source

#### Local Build

To build the Kubernetes Operator locally, use

```bash
./gradlew clean build
```

#### Container Image

To build a _JVM-based_ container image, use

```bash
./gradlew clean build -Dquarkus.container-image.build=true [-Dquarkus.container-image.image=<image>] [-Dquarkus.container-image.push=true]
```

To build a _native_ container image, use

```bash
./gradlew clean build -Dquarkus.package.jar.enabled=false -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true [-Dquarkus.container-image.image=<image>] [-Dquarkus.container-image.push=true]
```

Pushing to a registry may require credentials. Please refer to the respective documentation of [Docker](https://docs.docker.com/reference/cli/docker/login/) and your registry.
