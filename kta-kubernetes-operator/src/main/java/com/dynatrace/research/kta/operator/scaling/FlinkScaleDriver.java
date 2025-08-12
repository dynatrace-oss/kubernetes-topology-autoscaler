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

import com.dynatrace.research.kta.annotation.VisibleForTesting;
import com.dynatrace.research.kta.exception.InternalOperatorErrorException;
import com.dynatrace.research.kta.exception.ReconciliationFailedException;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ScaleDriver} that scales Apache Flink topologies on the <i>operator level</i> and
 * <i>deployment level</i> (without reactive mode). Requires Apache Flink v1.18 or higher. Scaling
 * the Flink topology itself is done via calls to the FLink API in the jobmanager. Additionally,
 * this Scale Driver also scales the needed resources (task managers), if configured in
 * {@link KtaPolicySpec}.
 */
public final class FlinkScaleDriver implements ScaleDriver {

  private static final Logger LOG =
      LoggerFactory.getLogger(com.dynatrace.research.kta.operator.scaling.FlinkScaleDriver.class);
  private static final String RESOURE_REQUIREMENTS_ENDPOINT_FORMAT_STRING =
      "%s/jobs/%s/resource-requirements";

  private final ObjectMapper objectMapper;
  private final KubernetesClient kubernetesClient;
  private final HttpClient httpClient;
  private final Duration flinkApiRequestTimeout;

  public FlinkScaleDriver(
      final ObjectMapper objectMapper,
      final KubernetesClient kubernetesClient,
      final HttpClient httpClient,
      final Duration flinkApiRequestTimeout) {
    this.objectMapper = objectMapper;
    this.kubernetesClient = kubernetesClient;
    this.httpClient = httpClient;
    this.flinkApiRequestTimeout = flinkApiRequestTimeout;
  }

  @Override
  public void scale(
      final KtaPolicySpec.ScaleDriver scaleDriverSpec,
      final Map<KtaPolicySpec.TopologyNode, Integer> parallelism) {
    LOG.debug("Attempt scaling action using {}", FlinkScaleDriver.class);
    this.scaleTaskManagerDeployment(scaleDriverSpec, parallelism);
    this.scaleFlinkResourceRequirements(scaleDriverSpec, parallelism);
  }

  private void scaleTaskManagerDeployment(
      KtaPolicySpec.ScaleDriver scaleDriverSpec,
      Map<KtaPolicySpec.TopologyNode, Integer> parallelism) {
    String taskManagerDeploymentName = scaleDriverSpec.getFlinkTaskManagerDeploymentName();
    if (taskManagerDeploymentName == null || taskManagerDeploymentName.trim().isEmpty()) {
      LOG.info("Did not scale task manager deployment because no deployment was given.");
    } else {
      Deployment deployment = this.kubernetesClient
          .apps()
          .deployments()
          .inNamespace(this.kubernetesClient.getNamespace())
          .withName(taskManagerDeploymentName)
          .get();

      int currentReplicas = deployment.getSpec().getReplicas();
      int desiredReplicas =
          calculateRequiredTaskManagers(scaleDriverSpec.getFlinkJobDeploymentType(), parallelism);

      if (currentReplicas != desiredReplicas) {
        LOG.info(
            "Updating replicas for {} from {} to {}",
            taskManagerDeploymentName,
            currentReplicas,
            desiredReplicas);
        this.kubernetesClient.apps().deployments().resource(deployment).scale(desiredReplicas);
      } else {
        LOG.info(
            "Leaving replicas for {} unchanged ({})", taskManagerDeploymentName, currentReplicas);
      }
    }
  }

  private void scaleFlinkResourceRequirements(
      KtaPolicySpec.ScaleDriver scaleDriverSpec,
      Map<KtaPolicySpec.TopologyNode, Integer> parallelism) {
    String url = createResourceRequirementEndpointUrl(
        scaleDriverSpec.getFlinkJobManagerBaseUrl(), scaleDriverSpec.getFlinkJobId());
    HttpRequest httpRequest;
    String body;
    try {
      body = createRequestBody(this.objectMapper, scaleDriverSpec.getFlinkTopology(), parallelism);
      LOG.debug("Flink request {}: {}", url, body);
      httpRequest = HttpRequest.newBuilder(URI.create(url))
          .PUT(HttpRequest.BodyPublishers.ofString(body))
          .header("Content-Type", "application/json")
          .timeout(this.flinkApiRequestTimeout)
          .build();
    } catch (JsonProcessingException | IllegalArgumentException e) {
      throw new InternalOperatorErrorException("Error building HTTP request", e);
    }

    java.net.http.HttpResponse<String> httpResponse;
    try {
      httpResponse =
          this.httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new ReconciliationFailedException("Error during invocation of Flink API", e);
    } catch (IllegalArgumentException e) {
      throw new InternalOperatorErrorException("Error sending HTTP request.", e);
    }

    if (httpResponse.statusCode() / 100 != 2) {
      throw new ReconciliationFailedException(
          "Flink API non-successful status code. Details: " + httpResponse.body());
    }
  }

  private static int calculateRequiredTaskManagers(
      KtaPolicySpec.ScaleDriver.FlinkJobDeploymentType jobDeploymentType,
      Map<KtaPolicySpec.TopologyNode, Integer> parallelism) {
    return switch (jobDeploymentType) {
      case SharedTaskSlots ->
        parallelism.values().stream().max(Integer::compareTo).orElseThrow();
      case StreamingGraphNodePerTaskSlot ->
        parallelism.values().stream().mapToInt(Integer::intValue).sum();
    };
  }

  private static String createResourceRequirementEndpointUrl(
      final String jobManagerAddress, final String jobId) {
    String address = jobManagerAddress.substring(
        0,
        jobManagerAddress.endsWith("/")
            ? jobManagerAddress.length() - 1
            : jobManagerAddress.length());
    return String.format(RESOURE_REQUIREMENTS_ENDPOINT_FORMAT_STRING, address, jobId);
  }

  @VisibleForTesting
  static String createRequestBody(
      ObjectMapper objectMapper,
      List<KtaPolicySpec.FlinkStreamingGraphNode> flinkStreamingGraphNodes,
      Map<KtaPolicySpec.TopologyNode, Integer> parallelism)
      throws JsonProcessingException {
    Map<String, Map<String, FlinkResourceRequirements>> requestBody = new HashMap<>();
    for (final KtaPolicySpec.FlinkStreamingGraphNode node : flinkStreamingGraphNodes) {
      int p = parallelism.get(node);
      requestBody.put(node.getId(), Map.of("parallelism", new FlinkResourceRequirements(p, p)));
    }
    return objectMapper.writeValueAsString(requestBody);
  }

  @VisibleForTesting
  static class FlinkResourceRequirements {
    @JsonProperty
    private int lowerBound;

    @JsonProperty
    private int upperBound;

    public FlinkResourceRequirements() {
      // jackson
    }

    public FlinkResourceRequirements(final int lowerBound, final int upperBound) {
      this.lowerBound = lowerBound;
      this.upperBound = upperBound;
    }

    public int getLowerBound() {
      return this.lowerBound;
    }

    public int getUpperBound() {
      return this.upperBound;
    }
  }
}
