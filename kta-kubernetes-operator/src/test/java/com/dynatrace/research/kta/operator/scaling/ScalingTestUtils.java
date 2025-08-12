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

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

/** Utility methods for {@link ScaleDriver} tests. */
public class ScalingTestUtils {
  private static final String LABEL_KEY = "app";
  private static final String LABEL_VALUE = "integration-test";
  private static final String CONTAINER_NAME = "integration-test-container";

  public static void createDeployment(
      String name, int initialReplicas, final KubernetesClient kubernetesClient) {
    Deployment deployment = new DeploymentBuilder()
        .withNewMetadata()
        .withNamespace(kubernetesClient.getNamespace())
        .withName(name)
        .addToLabels(LABEL_KEY, LABEL_VALUE)
        .endMetadata()
        .withNewSpec()
        .withReplicas(initialReplicas)
        .withNewSelector()
        .addToMatchLabels(LABEL_KEY, LABEL_VALUE)
        .endSelector()
        .withNewTemplate()
        .withNewMetadata()
        .addToLabels(LABEL_KEY, LABEL_VALUE)
        .endMetadata()
        .withNewSpec()
        .addNewContainer()
        .withName(CONTAINER_NAME)
        .withImage("busybox")
        .withCommand("sleep", "3600")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();

    kubernetesClient
        .apps()
        .deployments()
        .inNamespace(kubernetesClient.getNamespace())
        .resource(deployment)
        .create();
  }
}
