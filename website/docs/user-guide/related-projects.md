---
title: "User Guide :: Comparison to Related Projects"
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

# Comparison to Related Projects

This page compares KTA to other [horizontal](#horizontal-autoscaling) and [vertical or cluster autoscaling](#vertical-and-cluster-autoscaling) solutions.

## Horizontal Autoscaling

<style>
    #comparison th {
        text-align: center;
    }
    #comparison td {
        text-align: center;
    }
    #comparison td:nth-child(1) {
        text-align: left;
    }
    #comparison tr:nth-child(5) { background: #02D394; }
</style>

<div id="comparison">
    <table>
        <thead>
            <tr>
                <th></th>
                <th>Complex Algorithms</th>
                <th>Operator-Level Scaling on Streaming Topologies</th>
                <th>Framework-Agnostic</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td><a href="https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/" target="_blank">Horizontal Pod Autoscaler (HPA)</a></td>
                <td></td>
                <td></td>
                <td>✅</td>
            </tr>
            <tr>
                <td><a href="https://keda.sh/" target="_blank">Kubernetes Event-driven Autoscaling (KEDA)</a></td>
                <td></td>
                <td></td>
                <td>✅</td>
            </tr>
            <tr>
                <td><a href="https://custom-pod-autoscaler.readthedocs.io/en/latest/" target="_blank">Custom Pod Autoscaler (CPA)</a></td>
                <td>✅</td>
                <td></td>
                <td>✅</td>
            </tr>
            <tr>
                <td><a href="https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-stable/" target="_blank">Flink Kubernetes Operator Autoscaler</a></td>
                <td>✅</td>
                <td>✅</td>
                <td></td>
            </tr>
            <tr>
                <td style="color: black;">Kubernetes Topology Autoscaler (KTA)</td>
                <td>✅</td>
                <td>✅</td>
                <td>✅</td>
            </tr>
        </tbody>
    </table>
</div>

### KTA vs. Kubernetes Horizontal Pod Autoscaler (HPA)

HPA uses a simple formula to determine the scale out:

```text
desiredReplicas = ceil[currentReplicas * ( currentMetricValue / desiredMetricValue )]
```

This restricts HPA's ability to support state-of-the-art autoscaling algorithms for stream processing applications. Additionally, HPA cannot perform operator-level scaling on streaming topologies in a framework-agnostic manner.

However, HPA supports {{ external_link("declarative scaling policies", "https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/#configurable-scaling-behavior") }}, which KTA currently does not support.

### KTA vs. Kubernetes Event-Driven Autoscaling (KEDA)

KEDA, like [HPA](#kta-vs-kubernetes-horizontal-pod-autoscaler-hpa), use the same formula for determining the scale out:

```text
desiredReplicas = ceil[currentReplicas * ( currentMetricValue / desiredMetricValue )]
```

Similar to HPA, this restricts KEDA's ability to support state-of-the-art autoscaling algorithms for stream processing applications. Additionally, KEDA cannot perform operator-level scaling on streaming topologies in a framework-agnostic manner.

However, KEDA supports {{ external_link("declarative scaling policies and scaling to zero", "https://keda.sh/docs/2.14/concepts/scaling-deployments/") }}, which KTA currently does not support.

### KTA vs. Custom Pod Autoscaler (CPA)

KTA shares architectural similarities with CPA (Kubernetes Operator, algorithms implemented in a general purpose programming language).

However, CPA does not support operator-level scaling on streaming topologies in a framework-agnostic manner. On the other hand, CPA supports {{ external_link("scaling to zero", "https://custom-pod-autoscaler.readthedocs.io/en/latest/user-guide/scaling-to-and-from-zero/") }}, which KTA currently does not support.

### KTA vs. Flink Kubernetes Operator Autoscaler

Unlike the Flink Kubernetes Operator Autoscaler, KTA is framework-agnostic, making it ideal for research and rapid prototyping of autoscaling algorithms across different stream processing frameworks.

## Vertical and Cluster Autoscaling

Vertical and cluster autoscaling solutions complement KTA’s horizontal autoscaling capabilities. Popular solutions include:

- {{ external_link("Vertical Pod Autoscaler", "https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler") }}
- {{ external_link("Cluster Autoscaler", "https://github.com/kubernetes/autoscaler/tree/master/cluster-autoscaler") }}
- {{ external_link("Karpenter", "https://karpenter.sh/") }}
