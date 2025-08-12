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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.dynatrace.research.kta.extensions.KubeAPITestBase;
import com.dynatrace.research.kta.extensions.KubeAPITestExtension;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubeapitest.junit.KubeConfig;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// TODO: Annotation needs be on child class until
// https://github.com/fabric8io/kubernetes-client/issues/7223 is fixed

/** Tests for {@link GenericKubernetesScaleDriver}. */
@EnableKubeAPIServer
@ExtendWith(KubeAPITestExtension.class)
public class GenericKubernetesScaleDriverIT extends KubeAPITestBase {

  private static List<KtaPolicySpec.ScaleTargetRef> scaleTargets;
  private static KtaPolicySpec.ScaleDriver scaleDriverSpec;
  private ScaleDriver scaleDriver;

  // TODO: Annotation needs be on child class until
  // https://github.com/fabric8io/kubernetes-client/issues/7223 is fixed
  @KubeConfig
  static String kubeConfigYaml;

  @Override
  public String getKubeConfig() {
    return kubeConfigYaml;
  }

  @BeforeAll
  static void beforeAll() {
    initializeTestFixtures();
  }

  private static void initializeTestFixtures() {
    scaleTargets = List.of(
        new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-1"),
        new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-2"));
    scaleDriverSpec = new KtaPolicySpec.ScaleDriver();
    scaleDriverSpec.setType(KtaPolicySpec.ScaleDriver.Type.GenericKubernetes);
    scaleDriverSpec.setGenericKubernetesTopology(scaleTargets);
  }

  @BeforeEach
  void beforeEach() {
    this.scaleDriver = new GenericKubernetesScaleDriver(getKubernetesClient());
  }

  @Test
  void testScaling() {
    KubernetesClient kubernetesClient = getKubernetesClient();
    ScalingTestUtils.createDeployment("scale-target-1", 5, kubernetesClient);
    ScalingTestUtils.createDeployment("scale-target-2", 7, kubernetesClient);

    Map<KtaPolicySpec.TopologyNode, Integer> parallelism;

    // both should be scaled down
    parallelism = Map.of(scaleTargets.get(0), 2, scaleTargets.get(1), 4);
    this.scaleDriver.scale(scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment scaleTarget1 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-1")
          .get();
      assertThat(scaleTarget1).isNotNull();
      assertThat(scaleTarget1.getSpec().getReplicas()).isEqualTo(2);

      Deployment scaleTarget2 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-2")
          .get();
      assertThat(scaleTarget2).isNotNull();
      assertThat(scaleTarget2.getSpec().getReplicas()).isEqualTo(4);
    });

    // scale-target-1 is scaled down, scale-target-2 is scaled up
    parallelism = Map.of(scaleTargets.get(0), 1, scaleTargets.get(1), 10);
    this.scaleDriver.scale(scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment scaleTarget1 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-1")
          .get();
      assertThat(scaleTarget1).isNotNull();
      assertThat(scaleTarget1.getSpec().getReplicas()).isEqualTo(1);

      Deployment scaleTarget2 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-2")
          .get();
      assertThat(scaleTarget2).isNotNull();
      assertThat(scaleTarget2.getSpec().getReplicas()).isEqualTo(10);
    });

    // nothing should happen, but we swap the order of the scale targets in the object
    parallelism = Map.of(scaleTargets.get(0), 1, scaleTargets.get(1), 10);
    this.scaleDriver.scale(scaleDriverSpec, parallelism);

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment scaleTarget1 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-1")
          .get();
      assertThat(scaleTarget1).isNotNull();
      assertThat(scaleTarget1.getSpec().getReplicas()).isEqualTo(1);

      Deployment scaleTarget2 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-2")
          .get();
      assertThat(scaleTarget2).isNotNull();
      assertThat(scaleTarget2.getSpec().getReplicas()).isEqualTo(10);
    });

    // finally, scale scale-target-1 to the same parallelism as scale-target-2
    parallelism = Map.of(scaleTargets.get(0), 10, scaleTargets.get(1), 10);
    this.scaleDriver.scale(scaleDriverSpec, parallelism);

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment scaleTarget1 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-1")
          .get();
      assertThat(scaleTarget1).isNotNull();
      assertThat(scaleTarget1.getSpec().getReplicas()).isEqualTo(10);

      Deployment scaleTarget2 = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName("scale-target-2")
          .get();
      assertThat(scaleTarget2).isNotNull();
      assertThat(scaleTarget2.getSpec().getReplicas()).isEqualTo(10);
    });
  }
}
