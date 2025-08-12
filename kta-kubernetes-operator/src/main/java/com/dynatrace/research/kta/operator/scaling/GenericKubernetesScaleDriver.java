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

package com.dynatrace.research.kta.operator.scaling;

import com.dynatrace.research.kta.exception.InternalOperatorErrorException;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ScaleDriver} that scales native Kubernetes streaming topologies, i.e., streaming
 * topologies where each topology node is a Kubernetes Resource.
 *
 * <p>This scaled driver can be used for scaling on the
 *
 * <ul>
 *   <li><i>operator level</i>, e.g., when the topology nodes are different Kafka Streams instances,
 *       each containing a single operator.
 *   <li><i>deployment-level</i>, e.g., a Kafka Streams instance that contains the entire
 *       application or a Flink TaskManager instance with no slot sharing and reactive mode enabled.
 * </ul>
 */
public final class GenericKubernetesScaleDriver implements ScaleDriver {

  private static final Logger LOG = LoggerFactory.getLogger(GenericKubernetesScaleDriver.class);

  private final KubernetesClient kubernetesClient;

  public GenericKubernetesScaleDriver(final KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  @Override
  public void scale(
      final KtaPolicySpec.ScaleDriver scaleDriverSpec,
      final Map<KtaPolicySpec.TopologyNode, Integer> parallelism) {
    LOG.debug("Attempt scaling action using {}", GenericKubernetesScaleDriver.class);

    for (final KtaPolicySpec.ScaleTargetRef node : scaleDriverSpec.getGenericKubernetesTopology()) {
      if (node.getKind() != KtaPolicySpec.ScaleTargetRef.Kind.Deployment) {
        LOG.error("Unknown kind {}", node.getKind());
        throw new InternalOperatorErrorException("Kind " + node.getKind() + " not supported.");
      }

      LOG.debug("Node: {}", node);

      Deployment deployment = this.kubernetesClient
          .apps()
          .deployments()
          .inNamespace(this.kubernetesClient.getNamespace())
          .withName(node.getName())
          .get();

      int currentReplicas = deployment.getSpec().getReplicas();
      int desiredReplicas = parallelism.get(node);

      if (currentReplicas != desiredReplicas) {
        LOG.info("Updating replicas for {} from {} to {}", node, currentReplicas, desiredReplicas);
        this.kubernetesClient.apps().deployments().resource(deployment).scale(desiredReplicas);
      } else {
        LOG.info("Leaving replicas for {} unchanged ({})", node, currentReplicas);
      }
    }
  }
}
