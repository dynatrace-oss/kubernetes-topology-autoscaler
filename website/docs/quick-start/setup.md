---
title: "Quick Start :: Overview and Setup"
hide:
  - footer
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

# Quick Start: Overview and Setup

This quickstart guide walks you through the minimal required steps to get up and running with KTA.
It will show you how to

- [set up a local Kubernetes cluster with k3d](#setting-up-a-local-kubernetes-cluster-with-k3d),
- [deploy all KTA-related components](#deploy-kta-components), and
- apply a determinsitic demo sample policy to the [streaming processing system of your choice](#choose-your-stream-processing-system).

## Prerequisites

- Docker (tested with v28.0.1)
- `k3d` ([requirements](https://k3d.io/stable/#learning), [installation instructions](https://k3d.io/stable/#installation))
- [`kubectl`](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Setting Up a Local Kubernetes Cluster with `k3d`

This section shows you how to set up a local, lightweight Kubernetes cluster on your machine using [`k3d`](https://k3d.io/stable/).

First, create a `k3d` cluster with 1 node.

```bash
k3d cluster create kta-quickstart --servers 1 --image rancher/k3s:v1.29.15-k3s1
```

Check if the cluster is running as expected using

```bash
k3d node list
```

The output should be similar to

```text
NAME                                    ROLE           CLUSTER               STATUS
k3d-kta-quickstart-registry.localhost   registry                             running
k3d-kta-quickstart-server-0             server         kta-quickstart        running
k3d-kta-quickstart-serverlb             loadbalancer   kta-quickstart        running
```

Additionally, check if you can use `kubectl` to manage the created cluster, e.g., by executing

```bash
kubectl get nodes
```

## Deploy KTA Components

A KTA deployment has 2 main components: An instance of the [KTA Kubernetes Operator](#kta-kubernetes-operator) and an [autoscaling algorithm](#autoscaling-algorithm-user-defined-logic-implemented-using-the-kta-python-sdk) (user-defined logic).

### KTA Kubernetes Operator

The KTA Kubernetes Operator orchestrates the autoscaling reconciliation process.
It invokes the user-defined logic (user-defined functions) of the autoscaling algorithm, takes care of storing the result of each step in the algorithm for subsequent reconcilations, and also executes the scaling action.

KTA is configured by a so-called KTAPolicy, a Kubernetes Custom Resource Definition.
An instance of a KTAPolicy configures the autoscaling behavior of a single streaming application, this is, one KTAPolicy is responsible to scale a single streaming query.
It has to applied to the cluster _after_ the streaming application has been deployed, as you will see later on.

Deploy the operator using

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-kubernetes-operator/kubernetes/quickstart-install.yml
```

You can verify that everything is running as expected by using the commands below.

```bash
# Should show "deployment.apps/kta-kubernetes-operator condition met"
kubectl wait --for=condition=available --timeout=240s deployment/kta-kubernetes-operator
```

```bash
# Should show a single endpoint
kubectl get endpoints kta-kubernetes-operator
```

### Autoscaling Algorithm (User-Defined Logic Implemented Using the KTA Python SDK)

The KTA Python SDK assists you in implementing and deploying your custom autoscaling algorithms.
An autoscaling algorithm consists of up to 3 steps: Monitor, Analyze (optional), and Plan.
In this quickstart guide, you will use a [determinsitic demo sample algorithm](../../api-reference/python-sdk/algorithms/#kta_python_sdk.algorithm.dummy), which toggles between two states.
The sample algorithm also serves as reference how to implement your own autoscaling algorithms.

Deploy the autoscaling algorithm and a corresponding Kubernetes Service to the cluster using

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-python-sdk/kubernetes/quickstart-algorithm.yml
```

You can verify that everything is running as expected by using the commands below.

```bash
# Should show "deployment.apps/kta-quickstart-algorithm condition met"
kubectl wait --for=condition=available --timeout=240s deployment/kta-quickstart-algorithm
```

```bash
# Should show a single endpoint
kubectl get endpoints kta-quickstart-algorithm
```

The steps of the autoscaling algorithm are now exposed as individual endpoints under `http://kta-quickstart-algorithm.default.svc.cluster.local:8096/api/v1alpha1/[monitor|analyze|plan]`.
These endpoints will be invoked by the KTA Kubernetes Operator one after another during the reconciliation process.

## Choose Your Stream Processing System

Next, choose your desired stream processing system from the list below and follow the steps in the respective guide to see KTA in action.

- [Apache Flink](flink.md)
- [Apache Kafka Streams](kafka-streams.md)
