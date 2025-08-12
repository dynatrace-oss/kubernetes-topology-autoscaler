---
title: "User Guide :: Tutorials"
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

# Tutorials

## Installing the KTA Python SDK
<!-- markdownlint-disable MD046 -->
!!! note

    The KTA Python SDK will soon be available for download via {{ external_link("PyPi", "https://pypi.org/") }}.

Download the [KTA Python SDK wheel file](https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/releases/tag/v0.1.0-alpha.1) and place it in your project directory.
You can install the KTA Python SDK using pip (`pip install kta_python_sdk-<version>-py3-none-any.whl[http-backend]`) or, alternatively, add it as a dependency to your `pyproject.toml`. In the latter case, make sure to include the `http-backend` extra.
For futher details how to install a Python dependency from a file, please refer to the respective documentation.

## Implementing Custom Autoscaling Algorithms with the Python SDK

Custom autoscaling algorithms are stateless, user-defined Python functions[^1].
To assist you in the implementation and deployment process, KTA comes with a Python SDK.

An autoscaling algorithm consists of up to 3 steps: Monitor, Analyze (optional) and Plan.
Each step returns a result object that is accessible in subsequent steps.
For example, the result of the Monitor step is available in the Analyze step of the current MAPE-K loop evaluation.
Historical results, i.e., those of previously completed MAPE-K loop evaluations, are also accessible in each step.

### Result Structure

#### Monitor and Analyze Step

The result structure of the Monitor and Analyze step is user-defined.
It can be either a [Python `dict` or a class that extends the respective Pydantic model](../../api-reference/python-sdk/common/#kta_python_sdk.common.model.UdfResultBound).
Using a Pydantic model provides a typed, structured result, which is compatible with static type checkers like {{external_link("mypy", "https://mypy-lang.org/")}}

#### Plan Step

The result structure of the [Plan step](../../pi-reference/python-sdk/common/#kta_python_sdk.common.model.UdfPlanResultBound) is pre-defined.
The result must include the entire topology, this is, the parallelism of each topology node must be explicitly given, even if unchanged.
This requirement is also enforced by the KTA Kubernetes Operator.
If your topology consists of a single node, you may directly return an instance of [`PlanUdfResult`](../../api-reference/python-sdk/common/#kta_python_sdk.common.model.PlanUdfResult).

### Autoscaling Algorithm Implementation Options

There are 2 ways to implement an autoscaling algorithm.

1. **Single-class implementation**: Implement all steps in 1 class using [AutoscalingAlgorithmUDFs](../../api-reference/python-sdk/core/#kta_python_sdk.core.udf.AutoscalingAlgorithmUDFs).
1. **Multi-class implementation**: Implement each step separately ([AutoscalingAlgorithmMonitorUDF](../../api-reference/python-sdk/core/#kta_python_sdk.core.udf.AutoscalingAlgorithmMonitorUDF), [AutoscalingAlgorithmAnalyzeUDF](../../api-reference/python-sdk/core/#kta_python_sdk.core.udf.AutoscalingAlgorithmAnalyzeUDF), [AutoscalingAlgorithmPlanUDF](../../api-reference/python-sdk/core/#kta_python_sdk.core.udf.AutoscalingAlgorithmPlanUDF)).

The latter is useful when each algorithm step should run in a separate container.

For an example, please refer to the [sample algorithm](../../api-reference/python-sdk/algorithms/#kta_python_sdk.algorithm.dummy).

## Bootstrapping and Deploying an Autoscaling Algorithms

Each of the 3 user-defined autoscaling algorithms steps (Monitor, Analyze, Plan) must be exposed as a separate endpoint.

The KTA Python SDK provides [helper functions for application bootstrapping](../../api-reference/python-sdk/core/#kta_python_sdk.core.bootstrap) that simplify deployment.
<!-- markdownlint-disable MD034 -->
For an example how to deploy an algorithm, please refer to the corresponding [quickstart files](https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-python-sdk).
<!-- markdownlint-enable MD034 -->
Additionally, you also need to configure the [endpoints in the KTAPolicy](../../api-reference/kubernetes-operator/kta-policy-crd/#udfs).

## Configuring the KTA Kubernetes Operator

Configurations related to the autoscaling process are defined in a [KTAPolicy](../../api-reference/kubernetes-operator/kta-policy-crd).
<!-- markdownlint-disable MD034 -->
For an example, please refer to the [quickstart policies](https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-kubernetes-operator).
<!-- markdownlint-enable MD034 -->

## Using Out-of-the-Box Stream Processing Algorithms

Currently, KTA only includes a dummy algorithm for testing purposes. You can find a complete example how to use it in the [Quick Start](../quick-start/setup.md).

More built-in algorithms are on our roadmap -- stay tuned! ✨

[^1]: Actually, they can be written in any programming language, as long as there is a way to handle HTTP requests. However, currently you would have to handle the low-level interactions with the KTA Kubernetes Operator for other programming languages than Python yourself.
