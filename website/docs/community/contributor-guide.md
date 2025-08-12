---
title: "Community :: How to Contribute"
---

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

# Contributor Guide

We welcome contributions and suggestions to this project.

If you are unsure where to start, take a look at our [Roadmap](./roadmap.md).
Before implementing a feature or task, please open an {{ external_link("Issue", "https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/issues")}} to share your approach and allow us to support you throughout the process.

## Developer Setup

### Fork and Clone the KTA Repository

Like many other open source projects, we use {{ external_link("Pull Requests", "https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/about-pull-requests") }} to merge changes.

- Log in to your GitHub account.
- Go to {{ external_link("https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/", "https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/") }} and click `[Fork]` in the upper right corner to create a copy of the main repository under your account.
- Clone the forked repo to your local machine.

### Git Setup and Workflow

Ensure your Git username and email are correctly configured.
For details, please refer to {{ external_link("this guide", "https://docs.github.com/en/get-started/git-basics/setting-your-username-in-git") }}.

We use a simple Git workflow that consists of the `main` branch and feature branches.
When implementing a feature or task, create a new branch from `main`, implement your feature or task, push changes to your fork and then issue a {{ external_link("Pull Request", "https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/pulls")}}.

### IDE Setup

Below are IDE recommendations for working with specific KTA modules.

#### KTA Kubernetes Operator

We recommend using {{ external_link("IntelliJ IDEA", "https://www.jetbrains.com/idea/")}}.

- Go to `[File] > [Settings] > [Editor] > [Inspections]` and tick `Instance field access not qualified with 'this'` with `All scopes` and `Severity: Warning`.
- Each file must include a license header at the top of the file.

    1. To include it automatically go to `[File] > [Settings] > [Copyright] > [Copyright Profiles]`.
    2. Add a new profile and name it "Kubernetes Topology Autoscaler".
    3. Add the following `Copyright text` and click `[Apply]`.

    ```text
    Copyright (c) 2025 Dynatrace LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    ```

#### KTA Python SDK

We recommend using {{ external_link("PyCharm", "https://www.jetbrains.com/pycharm/")}}.

- After completing the {{ external_link("setup", "https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/tree/main/kta-python-sdk")}}, configure the virtual environment in PyCharm by following {{external_link("this guide", "https://www.jetbrains.com/help/pycharm/creating-virtual-environment.html")}}. This ensures that it is automatically activated in the Terminal inside PyCharm.
- Each file must include a license header at the top of the file. We recommend setting the license header manually, as we experienced some bugs in the past when setting it automatically using PyCharm.

    ```python
    #   Copyright (c) 2025 Dynatrace LLC
    #
    #   Licensed under the Apache License, Version 2.0 (the "License");
    #   you may not use this file except in compliance with the License.
    #   You may obtain a copy of the License at
    #
    #       http://www.apache.org/licenses/LICENSE-2.0
    #
    #   Unless required by applicable law or agreed to in writing, software
    #   distributed under the License is distributed on an "AS IS" BASIS,
    #   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    #   See the License for the specific language governing permissions and
    #   limitations under the License.
    ```

## Miscellaneous

- Markdown linting of the _entire project_ is handled using the `makefile` in the project root.
- For detailed information regarding dependencies, programming language versions, build instructions, etc., please refer to the README of each module.
