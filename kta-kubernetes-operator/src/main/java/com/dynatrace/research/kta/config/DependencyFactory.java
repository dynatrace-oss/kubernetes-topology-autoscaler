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

package com.dynatrace.research.kta.config;

import com.dynatrace.research.kta.client.HttpUdfClient;
import com.dynatrace.research.kta.client.PayloadDeserializer;
import com.dynatrace.research.kta.client.UdfClient;
import com.dynatrace.research.kta.client.dto.PlanResultDto;
import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.common.Union;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.dynatrace.research.kta.operator.persistence.InMemoryKnowledgeStore;
import com.dynatrace.research.kta.operator.persistence.KnowledgeStore;
import com.dynatrace.research.kta.operator.persistence.Result;
import com.dynatrace.research.kta.operator.scaling.FlinkScaleDriver;
import com.dynatrace.research.kta.operator.scaling.GenericKubernetesScaleDriver;
import com.dynatrace.research.kta.operator.scaling.ScaleDriver;
import com.dynatrace.research.kta.operator.udf.UdfInvocationHandler;
import com.dynatrace.research.kta.operator.util.IdGenerator;
import com.dynatrace.research.kta.operator.util.UuidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.javaoperatorsdk.operator.Operator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Instantiates and creates all dependencies for the application.
 *
 * <p>This class is used instead creating objects via CDI to make object creation more transparent.
 * Together with {@link Operator} it is the only class that should be autowired with {@link Inject}.
 */
@ApplicationScoped
public final class DependencyFactory {

  private static final String DEFAULT_KUBERNETES_NAMESPACE = "default";

  private final org.eclipse.microprofile.config.Config config;

  @Inject
  public DependencyFactory(final org.eclipse.microprofile.config.Config config) {
    this.config = config;
  }

  private static KubernetesClient kubernetesClient;
  private static IdGenerator idGenerator;
  private static HttpClient httpClient;
  private static UdfClient udfClient;
  private static PayloadDeserializer<Map<String, Object>> transparentPayloadDeserializer;
  private static PayloadDeserializer<Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>>>
      planPayloadDeserializer;
  private static Clock clock;
  private static ObjectMapper objectMapper;
  private static UdfInvocationHandler udfInvocationHandler;
  private static KnowledgeStore<Result> knowledgeStore;
  private static ScaleDriver genericKubernetesScaleDriver;
  private static ScaleDriver flinkScaleDriver;
  private static Validator validator;

  public synchronized KubernetesClient getKubernetesClient() {
    if (kubernetesClient == null) {
      int maxConcurrentRequests = this.config
          .getOptionalValue(
              ConfigOptions.KTA_KUBERNETES_CLIENT_MAX_CONCURRENT_REQUESTS, Integer.class)
          .orElse(ConfigOptions.KTA_KUBERNETES_CLIENT_MAX_CONCURRENT_REQUESTS_DEFAULT);
      kubernetesClient = new KubernetesClientBuilder()
          .withConfig(new ConfigBuilder(Config.autoConfigure(null))
              .withMaxConcurrentRequests(maxConcurrentRequests)
              .withNamespace(DEFAULT_KUBERNETES_NAMESPACE)
              .build())
          .withKubernetesSerialization(new KubernetesSerialization(getObjectMapper(), true))
          .build();
    }
    return kubernetesClient;
  }

  public synchronized IdGenerator getIdGenerator() {
    if (idGenerator == null) {
      idGenerator = new UuidGenerator();
    }
    return idGenerator;
  }

  public synchronized HttpClient getHttpClient() {
    if (httpClient == null) {
      Duration connectionTimeout = this.config
          .getOptionalValue(ConfigOptions.HTTP_CLIENT_CONNECTION_TIMEOUT, Duration.class)
          .orElse(ConfigOptions.HTTP_CLIENT_CONNECTION_TIMEOUT_DEFAULT);
      httpClient = HttpClient.newBuilder()
          .connectTimeout(connectionTimeout)
          .version(HttpClient.Version.HTTP_1_1)
          .build();
    }
    return httpClient;
  }

  public synchronized UdfClient getUdfClient() {
    if (udfClient == null) {
      Duration requestTimeout = this.config
          .getOptionalValue(ConfigOptions.KTA_UDF_HANDLER_REQUEST_TIMEOUT, Duration.class)
          .orElse(ConfigOptions.KTA_UDF_HANDLER_REQUEST_TIMEOUT_DEFAULT);
      udfClient = new HttpUdfClient(
          getTransparentPayloadDeserializer(), getObjectMapper(), getHttpClient(), requestTimeout);
    }
    return udfClient;
  }

  public synchronized PayloadDeserializer<Map<String, Object>> getTransparentPayloadDeserializer() {
    if (transparentPayloadDeserializer == null) {
      transparentPayloadDeserializer =
          new PayloadDeserializer.TransparentPayloadDeserializer(getObjectMapper());
    }
    return transparentPayloadDeserializer;
  }

  public synchronized PayloadDeserializer<Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>>>
      getPlanPayloadDeserializer() {
    if (planPayloadDeserializer == null) {
      planPayloadDeserializer = new PayloadDeserializer.PlanPayloadDeserializer(getObjectMapper());
    }
    return planPayloadDeserializer;
  }

  public synchronized Clock getClock() {
    if (clock == null) {
      clock = Clock.systemDefaultZone();
    }
    return clock;
  }

  public synchronized ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper();

      objectMapper.registerModule(new Jdk8Module());

      SimpleModule topologyNodeDTOModule = new SimpleModule();
      topologyNodeDTOModule.addKeySerializer(
          TopologyNodeDto.class, new JsonSerde.TopologyNodeDTOSerializer());
      topologyNodeDTOModule.addKeyDeserializer(
          TopologyNodeDto.class, new JsonSerde.TopologyNodeDTOKeyDeserializer());
      objectMapper.registerModule(topologyNodeDTOModule);

      SimpleModule topologyNodeModule = new SimpleModule();
      topologyNodeModule.addKeySerializer(
          KtaPolicySpec.TopologyNode.class, new JsonSerde.TopologyNodeSerializer());
      topologyNodeModule.addKeyDeserializer(
          KtaPolicySpec.TopologyNode.class, new JsonSerde.TopologyNodeKeyDeserializer());
      objectMapper.registerModule(topologyNodeModule);
    }
    return objectMapper;
  }

  public synchronized UdfInvocationHandler getUdfInvocationHandler() {
    if (udfInvocationHandler == null) {
      udfInvocationHandler = new UdfInvocationHandler(getUdfClient(), getPlanPayloadDeserializer());
    }
    return udfInvocationHandler;
  }

  public synchronized KnowledgeStore<Result> getKnowledgeStore() {
    if (knowledgeStore == null) {
      knowledgeStore = new InMemoryKnowledgeStore<>();
    }
    return knowledgeStore;
  }

  public synchronized ScaleDriver getScaleDriver(KtaPolicySpec.ScaleDriver.Type type) {
    return switch (type) {
      case GenericKubernetes -> {
        if (genericKubernetesScaleDriver == null) {
          genericKubernetesScaleDriver = new GenericKubernetesScaleDriver(getKubernetesClient());
        }
        yield genericKubernetesScaleDriver;
      }
      case Flink -> {
        if (flinkScaleDriver == null) {
          Duration flinkApiRequestTimeout = this.config
              .getOptionalValue(
                  ConfigOptions.KTA_FLINK_SCALE_DRIVER_REQUEST_TIMEOUT, Duration.class)
              .orElse(ConfigOptions.KTA_FLINK_SCALE_DRIVER_REQUEST_DEFAULT);
          flinkScaleDriver = new FlinkScaleDriver(
              getObjectMapper(), getKubernetesClient(), getHttpClient(), flinkApiRequestTimeout);
        }
        yield flinkScaleDriver;
      }
    };
  }

  public synchronized Validator getValidator() {
    if (validator == null) {
      validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
    return validator;
  }
}
