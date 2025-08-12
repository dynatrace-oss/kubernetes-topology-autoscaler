---
title: "Quick Start :: Scale Apache Flink"
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

# Quick Start: Scale Apache Flink
<!-- markdownlint-disable MD024 -->
!!! warning
    Make sure you have first followed the steps in [Quick Start: Overview and Setup](setup.md).

This guide shows you how to scale a simple Apache Flink application on the

- [operator level using the KTA Flink Scale Driver](#scaling-apache-flink-on-the-operator-level)
- [deployment level using Flink's reactive mode and the KTA Generic Kubernetes Scale Driver](#scaling-apache-flink-on-the-deployment-level).

The guide uses a simple `WordCount` streaming query, which consists of two operators, a tokenizer and a counter.
The application is deployed in {{ external_link("Flink's application mode", "https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/deployment/overview/#application-mode") }}.
Each task manager has a single task slot and is deployed as a separate pod using a Kubernetes Deployment.
KTA will adjust the parallelism based on the output of the sample algorithm, while also taking care of scaling the number of required task managers up and down.

<!-- markdownlint-disable MD046 -->
!!! note

    When using **Flink 1.18+**, [operator-level scaling using the KTA Flink Scale Driver](#scaling-apache-flink-on-the-operator-level) can also be used for scaling Flink on the **deployment level** in a less disruptive way than using reactive mode.
    To do so, the Plan step of the autoscaling algorithm just needs to return the same parallelism for each operator.
<!-- markdownlint-enable MD046 -->

## Scaling Apache Flink on the Operator Level

To deploy the application on the operator level, you can choose from 2 different deployment options.

- **Operators running in shared task slots (Flink default)**

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-flink/kubernetes/operator-level-shared-task-slots.yml
```

- **Operators running in dedicated task slots (streaming graph node per task slot)**

Each parallel operator instance runs in its own pod. This deployment option is useful when your algorithm is based on pod-level metrics such as CPU utilization, e.g., as done in {{ external_link('G. Siachamis et al. "Evaluating Stream Processing Autoscalers" (DEBS 2024)', "https://dl.acm.org/doi/pdf/10.1145/3629104.3666036") }}.

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-flink/kubernetes/operator-level-streaming-graph-node-per-task-slot.yml
```

Next, run the command below to make the Flink WebUI accessible in the browser on the host device.

```bash
kubectl proxy
```

<!-- markdownlint-disable MD046 -->
!!! info

    It may take a short time until the Web UI becomes accessible.
<!-- markdownlint-enable MD046 -->

When you go to {{ external_link("http://localhost:8001/api/v1/namespaces/default/services/kta-quickstart-flink-jobmanager:webui/proxy", "http://localhost:8001/api/v1/namespaces/default/services/kta-quickstart-flink-jobmanager:webui/proxy") }}, you should see that there are

- 2 task managers and 2 task slots in total (all used) for the deployment option with shared task slots.
- 4 task managers and 4 task slots in total (all used) for the deployment option with a dedicated task slot per operator instance.

If you look at the running job, the operator parallelism for both operators should be 2.

### Apply a KTAPolicy, Behold and See: Observe KTA's Scaling Behavior

Finally, to activate KTA, apply the corresponding KTAPolicy for your deployment option.

- **Operators runing in shared task slots (Flink default)**

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-kubernetes-operator/kubernetes/quickstart-policy-flink-operator-level-shared-task-slots.yml
```

- **Operators running in dedicated task slots (streaming graph node per task slot)**

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-kubernetes-operator/kubernetes/quickstart-policy-flink-operator-level-streaming-graph-node-per-task-slot.yml
```

The KTAPolicy uses KTA's Flink Scale Driver and a reconciliation interval of 30 seconds.

By default, the sample algorithm toggles the state of an operator (parallelism) on every second invocation, with possible states being a parallelism of 2 or 4.
When you look at the job in the Flink Web UI in detail, you should see that the operators changed their parallelism immedetialy upon applying the policy and then changes occur approximately every 60 seconds.

Since the application is deployed on the operator level, each operator can run with a different parallelism.
If the first operator is running with parallelism 4, the second one should run with parallelism 2 and vice versa.
The number of task managers should also been scaled from the initial 2 to 4 (shared task slots) and 6 (dedicated task slots).

### Clean Up and Next Steps

Congratulations! 🥳 You have successfully deployed KTA to scale an Apache Flink application on the operator level.

If you don't use the cluster anymore, you can tear it down using

```bash
k3d cluster delete kta-quickstart
```

To learn more about KTA and how to implement your own autoscaling algorithms, check out the [User Guide](../../user-guide) and the [API Reference](../../api-reference/).

## Scaling Apache Flink on the Deployment Level

To deploy the application on the deployment level, use

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-flink/kubernetes/deployment-level.yml
```

Next, run the command below to make the Flink WebUI accessible in the browser on the host device.

```bash
kubectl proxy
```

<!-- markdownlint-disable MD046 -->
!!! info

    It may take a short time until the Web UI becomes accessible.
<!-- markdownlint-enable MD046 -->

When you go to {{ external_link("http://localhost:8001/api/v1/namespaces/default/services/kta-quickstart-flink-jobmanager:webui/proxy", "http://localhost:8001/api/v1/namespaces/default/services/kta-quickstart-flink-jobmanager:webui/proxy") }}, you should see that there are 2 task managers and 2 task slots in total (all used).
If you look at the running job, the operator parallelism for both operators should be 2.

### Apply a KTAPolicy, Behold and See: Observe KTA's Scaling Behavior

Finally, to activate KTA, apply the corresponding KTAPolicy.

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-kubernetes-operator/kubernetes/quickstart-policy-flink-deployment-level.yml
```

The KTAPolicy uses KTA's Generic Kubernetes Scale Driver and a reconciliation interval of 30 seconds.

By default, the sample algorithm toggles the state of an operator (parallelism) on every second invocation, with possible states being a parallelism of 2 or 4.
When you look at the job in the Flink Web UI in detail, you should see that the operators changed their parallelism immedetialy upon applying the policy and then changes occur approximately every 60 seconds.

Since the application is deployed on the deployment level, both operators should always run with the same parallelism (either 2 or 4).
The number of task managers should also scale to 2 and 4 accordingly.

### Clean Up and Next Steps

Congratulations! 🥳 You have successfully deployed KTA to scale an Apache Flink application on the deployment level using Apache Flink's reactive mode.

If you don't use the cluster anymore, you can tear it down using

```bash
k3d cluster delete kta-quickstart
```

To learn more about KTA and how to implement your own autoscaling algorithms, check out the [User Guide](../../user-guide) and the [API Reference](../../api-reference/).
