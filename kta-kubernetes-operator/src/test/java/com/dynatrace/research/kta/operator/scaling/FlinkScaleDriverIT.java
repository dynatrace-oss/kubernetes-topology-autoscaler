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
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.dynatrace.research.kta.extensions.KubeAPITestBase;
import com.dynatrace.research.kta.extensions.KubeAPITestExtension;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubeapitest.junit.KubeConfig;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import shaded_package.io.netty.handler.codec.http.HttpMethod;

// TODO: Annotation needs be on child class until
// https://github.com/fabric8io/kubernetes-client/issues/7223 is fixed

/**
 * Tests for {@link FlinkScaleDriver}. Requires an up to date CRD definition in <code>
 * test/resources/META-INF.fabric8</code>.
 */
@EnableKubeAPIServer
@ExtendWith(KubeAPITestExtension.class)
public class FlinkScaleDriverIT extends KubeAPITestBase {

  private static final String SERVER_ADDRESS = "http://127.0.0.1:";
  private static final String FLINK_TM_DUMMY_DEPLOYMENT_NAME = "flink-tm-dummy-deployment-name";
  private static final String RESOURE_REQUIREMENTS_ENDPOINT_PATH_FORMAT_STRING =
      "/jobs/%s/resource-requirements";
  private static final String FLINK_JOB_ID = "flink-job-1";
  private static ClientAndServer server;

  private static final List<KtaPolicySpec.FlinkStreamingGraphNode> flinkStreamingGraphNodes =
      List.of(
          new KtaPolicySpec.FlinkStreamingGraphNode("flink-node-1"),
          new KtaPolicySpec.FlinkStreamingGraphNode("flink-node-2"));

  private final ObjectMapper objectMapper = dependencyFactory.getObjectMapper();
  private final HttpClient httpClient = dependencyFactory.getHttpClient();
  private KtaPolicySpec.ScaleDriver scaleDriverSpec;
  private ScaleDriver scaleDriver;
  private AtomicInteger numApiRequests;

  // TODO: Annotation needs be on child class until
  // https://github.com/fabric8io/kubernetes-client/issues/7223 is fixed
  @KubeConfig
  static String kubeConfigYaml;

  @Override
  public String getKubeConfig() {
    return kubeConfigYaml;
  }

  @BeforeEach
  void beforeEach() {
    server = startClientAndServer();
    final String serverBaseUrl = SERVER_ADDRESS + server.getLocalPort();

    this.scaleDriver = new FlinkScaleDriver(
        this.objectMapper, getKubernetesClient(), this.httpClient, Duration.ofSeconds(5));

    this.scaleDriverSpec = new KtaPolicySpec.ScaleDriver();
    this.scaleDriverSpec.setType(KtaPolicySpec.ScaleDriver.Type.Flink);
    this.scaleDriverSpec.setFlinkTopology(flinkStreamingGraphNodes);
    this.scaleDriverSpec.setFlinkJobManagerBaseUrl(serverBaseUrl);
    this.scaleDriverSpec.setFlinkJobId(FLINK_JOB_ID);
    this.scaleDriverSpec.setFlinkTaskManagerDeploymentName(FLINK_TM_DUMMY_DEPLOYMENT_NAME);

    this.numApiRequests = new AtomicInteger();
    setUpServer();
  }

  private void setUpServer() {
    // Mocks
    // https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-resource-requirements-1
    server
        .when(request()
            .withPath(String.format(RESOURE_REQUIREMENTS_ENDPOINT_PATH_FORMAT_STRING, FLINK_JOB_ID))
            .withMethod(HttpMethod.PUT.name()))
        .respond(httpRequest -> {
          this.numApiRequests.getAndIncrement();
          return response()
              .withStatusCode(200)
              .withHeader("Content-Type", "application/json")
              .withBody(String.format("{\"request-id\": \"%s\"}", this.numApiRequests));
        });
  }

  @Test
  void testStreamingGraphNodePerTaskSlotDeployment() {
    this.scaleDriverSpec.setFlinkJobDeploymentType(
        KtaPolicySpec.ScaleDriver.FlinkJobDeploymentType.StreamingGraphNodePerTaskSlot);
    KubernetesClient kubernetesClient = getKubernetesClient();
    ScalingTestUtils.createDeployment(FLINK_TM_DUMMY_DEPLOYMENT_NAME, 10, kubernetesClient);

    Map<KtaPolicySpec.TopologyNode, Integer> parallelism;

    // task managers should be scaled down
    parallelism = Map.of(flinkStreamingGraphNodes.get(0), 2, flinkStreamingGraphNodes.get(1), 4);
    this.scaleDriver.scale(this.scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment deployment = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName(FLINK_TM_DUMMY_DEPLOYMENT_NAME)
          .get();
      assertThat(deployment).isNotNull();
      assertThat(deployment.getSpec().getReplicas()).isEqualTo(6);
      assertThat(this.numApiRequests.get()).isEqualTo(1);
    });

    // task managers should stay the same
    parallelism = Map.of(flinkStreamingGraphNodes.get(0), 5, flinkStreamingGraphNodes.get(1), 1);
    this.scaleDriver.scale(this.scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment deployment = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName(FLINK_TM_DUMMY_DEPLOYMENT_NAME)
          .get();
      assertThat(deployment).isNotNull();
      assertThat(deployment.getSpec().getReplicas()).isEqualTo(6);
      assertThat(this.numApiRequests.get()).isEqualTo(2);
    });

    // task managers should be scaled up
    parallelism = Map.of(flinkStreamingGraphNodes.get(0), 10, flinkStreamingGraphNodes.get(1), 5);
    this.scaleDriver.scale(this.scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment deployment = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName(FLINK_TM_DUMMY_DEPLOYMENT_NAME)
          .get();
      assertThat(deployment).isNotNull();
      assertThat(deployment.getSpec().getReplicas()).isEqualTo(15);
      assertThat(this.numApiRequests.get()).isEqualTo(3);
    });
  }

  @Test
  void testSharedTaskSlotDeployment() {
    this.scaleDriverSpec.setFlinkJobDeploymentType(
        KtaPolicySpec.ScaleDriver.FlinkJobDeploymentType.SharedTaskSlots);
    KubernetesClient kubernetesClient = getKubernetesClient();
    ScalingTestUtils.createDeployment(FLINK_TM_DUMMY_DEPLOYMENT_NAME, 10, kubernetesClient);

    Map<KtaPolicySpec.TopologyNode, Integer> parallelism;

    // task managers should be scaled down
    parallelism = Map.of(flinkStreamingGraphNodes.get(0), 2, flinkStreamingGraphNodes.get(1), 4);
    this.scaleDriver.scale(this.scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment deployment = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName(FLINK_TM_DUMMY_DEPLOYMENT_NAME)
          .get();
      assertThat(deployment).isNotNull();
      assertThat(deployment.getSpec().getReplicas()).isEqualTo(4);
      assertThat(this.numApiRequests.get()).isEqualTo(1);
    });

    // task managers should stay the same
    parallelism = Map.of(flinkStreamingGraphNodes.get(0), 5, flinkStreamingGraphNodes.get(1), 1);
    this.scaleDriver.scale(this.scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment deployment = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName(FLINK_TM_DUMMY_DEPLOYMENT_NAME)
          .get();
      assertThat(deployment).isNotNull();
      assertThat(deployment.getSpec().getReplicas()).isEqualTo(5);
      assertThat(this.numApiRequests.get()).isEqualTo(2);
    });

    // task managers should be scaled up
    parallelism = Map.of(flinkStreamingGraphNodes.get(0), 10, flinkStreamingGraphNodes.get(1), 5);
    this.scaleDriver.scale(this.scaleDriverSpec, parallelism);

    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      Deployment deployment = kubernetesClient
          .apps()
          .deployments()
          .inNamespace(kubernetesClient.getNamespace())
          .withName(FLINK_TM_DUMMY_DEPLOYMENT_NAME)
          .get();
      assertThat(deployment).isNotNull();
      assertThat(deployment.getSpec().getReplicas()).isEqualTo(10);
      assertThat(this.numApiRequests.get()).isEqualTo(3);
    });
  }
}
