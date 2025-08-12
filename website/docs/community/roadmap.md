---
title: "Community :: Roadmap"
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

# Roadmap

## Features

Below is a non-exhaustive list of potential _features_ for future releases (presented in no particular order):

- Provide state-of-the-art stream processing autoscaling algorithms out of the box. A good first candidate is {{ external_link("DS2", "https://www.usenix.org/system/files/osdi18-kalavri.pdf") }}.
- Provide simple autoscaling algorithms (e.g., {{ external_link("HPA", "https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale") }}) out of the box.
- Support additional native-Kubernetes Resources that implement the Scale Sub-resource. This would allow users to scale any native Kubernets framework, e.g., {{ external_link("Numaflow", "https://numaflow.numaproj.io/") }}.
- Integrate scale strategies into the KTA Kubernetes Operator (cf. {{ external_link("HPA workload stabilization", "https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/#flapping") }}).
- Monitor changes in stream processing application and reconcile state back to the desired state of KTA.

## Tasks

Below is a non-exhaustive list of potential _tasks_ (work items that do not introduce a new feature but, e.g., improve user experience) for future releases (presented in no particular order):

- Publish the KTA Python SDK to {{ external_link("PyPI", "https://pypi.org/") }}.
- Provide {{ external_link("helm charts", "https://helm.sh/") }}.
- Improve error handling (robustness).
- Simplify Flink deployment (e.g., automated job graph retrieval from task manager).
- Refine SDK and API design.
- Introduce website versioning.
