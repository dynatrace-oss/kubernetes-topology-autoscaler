---
title: "User Guide :: Concepts"
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

# Glossary

## Deployment/Scaling Mode

Scaling can be configured in 2 modes.

- Operator-level scaling: Parallelism can be configured for each operator separately.
- Deployment-level scaling: All operators are configured with the same parallelism.

The application deployment must align with the selected scaling mode ([Scale Driver](#scale-driver)):

|                      | Operator-Level Scaling                                                                                               | Deployment-Level Scaling                                                 |
|----------------------|----------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| Apache Flink         | `scaleDriver.type: flink`, `flinkJobDeploymentType: [shared_task_slots|streaming_graph_node_per_task_slot]{0,1}`     | `scaleDriver.type: generic_kubernetes`, Flink `scheduler-mode: reactive` |
| Apache Kafka Streams | `scaleDriver.type: generic_kubernetes`, each subtopology in separate application                                     | `scaleDriver.type: generic_kubernetes`                                   |

## MAPE-K Feedback Loop

**M**onitor-**A**nalyze-**P**lan-**E**xecute over shared **K**nowledge feedback loop.
The MAPE-K feedback loop is the conceptual model used for autoscaling in KTA.

KTA implements MAPE-K as follows:

- Monitor: Collects metrics.
- Analyze (optional): Processes metrics, e.g., aggregation.
- Plan: Determines the desired parallelism of the streaming topology.
- Execute: Applies the scaling action, i.e., scales the application to the desired parallelism based on the Plan step.
- Knowledge: Stores historical data of previous MAPE-K loop iterations.

The Monitor, Analyze and Plan steps are implemented as [user-defined logic](#user-defined-autoscaling-logic) using the [KTA Python SDK](#kta-python-sdk).
The Execute step is performed by the [KTA Kubernetes Operator](#kta-kubernetes-operator) via a [Scale Driver](#scale-driver).
Knowledge is stored by the [KTA Kubernetes Operator](#kta-kubernetes-operator) in a [Result Store](#result-store).

## KTA Kubernetes Operator

The KTA Kubernetes Operator orchestrates the autoscaling process and continously reconciles the parallelism of the stream processing application.

This includes

- invoking the user-defined autoscaling logic (Monitor, Analyze, Plan) and providing historical data from the [Result Store](#result-store).
- executing scaling actions using a [Scale Driver](#scale-driver).
- storing results from previous MAPE-K loop evaluations in a [Result Store](#result-store).

## KTA Policy

A Kubernetes Custom Resource Definition (CRD) to configure the [KTA Kubernetes Operator](#kta-kubernetes-operator).

## KTA Python SDK

A Python SDK that helps users implement and deploy custom autoscaling algorithms.

## Result Store

A component of the [KTA Kubernetes Operator](#kta-kubernetes-operator) that stores historical data from pevious MAPE-K loop evaluations.

## Scale Driver

A component of the [KTA Kubernetes Operator](#kta-kubernetes-operator) responsible for executing the scaling action.
The choice of Scale Driver depends on the stream processing framework and its used deployment model.

## Topology (also Streaming Topology)

A logical representation of a stream processing application or query, defined as a topologically sorted list of operators.
In [deployment-level scaling mode](#deploymentscaling-mode), "operator" may refer to the entire stream processing application.

## User-Defined Autoscaling Logic

Custom logic for the Monitor, Analye and Plan steps of the [MAPE-K loop](#mape-k-feedback-loop), implemented and deployed using the [KTA Python SDK](#kta-python-sdk).
