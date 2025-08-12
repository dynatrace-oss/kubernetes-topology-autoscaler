---
title: "Quick Start :: Scale Apache Kafka Streams"
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

# Quick Start: Scale Apache Kafka Streams
<!-- markdownlint-disable MD024 -->
!!! warning
    Make sure you have first followed the steps in [Quick Start: Overview and Setup](setup.md).

This guide shows you how to scale a simple Apache Kafka Streams application on the [operator-level](#scaling-apache-kafka-streams-on-the-operator-level) and on the [deployment-level](#scaling-apache-kafka-streams-on-the-deployment-level-kafka-streams-default) using the KTA Generic Kubernetes Scale Driver.

The guide uses a simple `WordCount` streaming query, which consists of two subtopologies, a tokenizer and a counter.
To deploy Apache Kafka (which is needed to run Kafka Streams), you will use {{ external_link("Strimzi", "https://strimzi.io/")}}.
KTA will adjust the parallelism (= number of replicas of the deployments) based on the output of the sample algorithm.

## Set up a Kafka Cluster

Install Strimzi using

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-kstreams/kubernetes/strimzi.yml
```

Next, deploy the Kafka Cluster using

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-kstreams/kubernetes/kafka-cluster.yml
```

You can verify that everything is running as expected by using the command below.

```bash
kubectl wait kafka/kta-kafka-cluster --for=condition=Ready --timeout=240s
```

Then, create a source topic and a sink topic.

```bash
# Source topic
cat << EOF | kubectl create -f -
apiVersion: kafka.strimzi.io/v1beta1
kind: KafkaTopic
metadata:
  name: kta-quickstart-source-topic
  labels:
    strimzi.io/cluster: kta-kafka-cluster
spec:
  partitions: 10
  replicas: 1
EOF
```

```bash
# Sink topic
cat << EOF | kubectl create -f -
apiVersion: kafka.strimzi.io/v1beta1
kind: KafkaTopic
metadata:
  name: kta-quickstart-sink-topic
  labels:
    strimzi.io/cluster: kta-kafka-cluster
spec:
  partitions: 10
  replicas: 1
EOF
```

Finally, start the producer.

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-kstreams/kubernetes/producer.yml
```

The producer writes to the source topic.
The Kafka Streams application will use this topic as input to the streaming query.
You can verify that everything is running as expected by using the command below.

```bash
kubectl run kafka-consumer -it --rm=true --restart=Never --image=quay.io/strimzi/kafka:0.47.0-kafka-4.0.0 -- bin/kafka-console-consumer.sh --bootstrap-server kta-kafka-cluster-kafka-bootstrap:9092 --topic kta-quickstart-source-topic --from-beginning
```

You should see text fragments from {{ external_link('Franz Kafka\'s "Der Prozess"', "https://www.gutenberg.org/cache/epub/69327/pg69327.txt") }}, displayed line by line.

Close the Kafka console consumer using `[CTRL]` + `[C]` and **wait until the application exits automatically**.

## Scaling Apache Kafka Streams on the Operator Level

To deploy the application on the operator level, you need to

- explicitly create intermediate topics between subtopologies and
- manually split the subtopologies of your Apache Kafka Streams application into separate applications.

!!! info
    To simplify deployment, we are currently evaluating the feasibility of using a {{ external_link("custom task assignor logic", "https://cwiki.apache.org/confluence/display/KAFKA/KIP-924%3A+customizable+task+assignment+for+Streams") }} instead of manually splitting the application.

Create the intermediate topic using

```bash
cat << EOF | kubectl create -f -
apiVersion: kafka.strimzi.io/v1beta1
kind: KafkaTopic
metadata:
  name: kta-quickstart-intermediate-topic
  labels:
    strimzi.io/cluster: kta-kafka-cluster
spec:
  partitions: 10
  replicas: 1
EOF
```

Then deploy the tokenizer and the counter as separate applications using

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-kstreams/kubernetes/operator-level.yml
```

You can verify that everything is running as expected by using the commands below.

```bash
kubectl run kafka-consumer -it --rm=true --restart=Never --image=quay.io/strimzi/kafka:0.47.0-kafka-4.0.0 -- bin/kafka-console-consumer.sh --bootstrap-server kta-kafka-cluster-kafka-bootstrap:9092 --topic kta-quickstart-intermediate-topic --from-beginning --property value.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer --property print.key=true --property key.separator=","
```

```bash
kubectl run kafka-consumer -it --rm=true --restart=Never --image=quay.io/strimzi/kafka:0.47.0-kafka-4.0.0 -- bin/kafka-console-consumer.sh --bootstrap-server kta-kafka-cluster-kafka-bootstrap:9092 --topic kta-quickstart-sink-topic --from-beginning --property value.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer --property print.key=true --property key.separator=","
```

For the first Kafka console consumer, you should see key-value pairs of a string and an integer, where the values are always 1. This is the output of the tokenizer.
For the second Kafka console consumer, you should see key value pairs of a string and an integer, where the values become eventually greater than 1. This is the (final) output of the counter.

When you run

```bash
kubectl get deployment kta-quickstart-kstreams-tokenizer kta-quickstart-kstreams-counter
```

you should see that both, the tokenizer and the counter run with a parallelism of 2.

### Apply a KTAPolicy, Behold and See: Observe KTA's Scaling Behavior

Finally, to activate KTA, apply the corresponding KTAPolicy.

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-kubernetes-operator/kubernetes/quickstart-policy-kstreams-operator-level.yml
```

The KTAPolicy uses KTA's Generic Kubernetes Scale Driver and a reconciliation interval of 30 seconds.

By default, the sample algorithm toggles the state of an operator (parallelism) on every second invocation, with possible states being a parallelism of 2 or 4.

When you run

```bash
watch -n 2 'kubectl get deployment kta-quickstart-kstreams-tokenizer kta-quickstart-kstreams-counter'
```

you should see that the operators changed their parallelism immedetialy upon applying the policy and then changes occur approximately every 60 seconds.

Since the application is deployed on the operator level, each operator can run with a different parallelism.
If the first operator is running with parallelism 4, the second one should run with parallelism 2 and vice versa.

### Clean Up and Next Steps

Congratulations! 🥳 You have successfully deployed KTA to scale an Apache Kafka Streams application on the operator level.

If you don't use the cluster anymore, you can tear it down using

```bash
k3d cluster delete kta-quickstart
```

To learn more about KTA and how to implement your own autoscaling algorithms, check out the [User Guide](../../user-guide) and the [API Reference](../../api-reference/).

## Scaling Apache Kafka Streams on the Deployment Level (Kafka Streams Default)

To deploy the application on the deployment level use

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-stream-processing-systems/kta-quickstart-kstreams/kubernetes/deployment-level.yml
```

You can verify that everything is running as expected by using the command below.

```bash
kubectl run kafka-consumer -it --rm=true --restart=Never --image=quay.io/strimzi/kafka:0.47.0-kafka-4.0.0 -- bin/kafka-console-consumer.sh --bootstrap-server kta-kafka-cluster-kafka-bootstrap:9092 --topic kta-quickstart-sink-topic --from-beginning --property value.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer --property print.key=true --property key.separator=","
```

You should see key value pairs of a string and an integer, where the values become eventually greater than 1. This is the (final) output of the counter.

When you run

```bash
kubectl get deployment kta-quickstart-kstreams-tokenizer-counter
```

you should see that application is running with a parallelsim of 2.

### Apply a KTAPolicy, Behold and See: Observe KTA's Scaling Behavior

Finally, to activate KTA, apply the corresponding KTAPolicy.

```bash
kubectl apply -f https://raw.githubusercontent.com/dynatrace-oss/kubernetes-topology-autoscaler/refs/tags/{{ version }}/quickstart-examples/kta-quickstart-kubernetes-operator/kubernetes/quickstart-policy-kstreams-deployment-level.yml
```

The KTAPolicy uses KTA's Generic Kubernetes Scale Driver and a reconciliation interval of 30 seconds.

By default, the sample algorithm toggles the state of an operator (parallelism) on every second invocation, with possible states being a parallelism of 2 or 4.

When you run

```bash
watch -n 2 'kubectl get deployment kta-quickstart-kstreams-tokenizer kta-quickstart-kstreams-counter'
```

you should see that the application changed its parallelism immedetialy upon applying the policy and then changes occur approximately every 60 seconds.

Since the application is deployed on the deployment level, both operators run with the same parallelism (either 2 or 4).

### Clean Up and Next Steps

Congratulations! 🥳 You have successfully deployed KTA to scale an Apache Kafka Streams application on the deployment level.

If you don't use the cluster anymore, you can tear it down using

```bash
k3d cluster delete kta-quickstart
```

To learn more about KTA and how to implement your own autoscaling algorithms, check out the [User Guide](../../user-guide) and the [API Reference](../../api-reference/).
