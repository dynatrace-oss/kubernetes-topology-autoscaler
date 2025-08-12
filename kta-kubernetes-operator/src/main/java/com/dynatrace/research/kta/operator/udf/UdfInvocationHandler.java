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

package com.dynatrace.research.kta.operator.udf;

import com.dynatrace.research.kta.client.PayloadDeserializer;
import com.dynatrace.research.kta.client.Response;
import com.dynatrace.research.kta.client.UdfClient;
import com.dynatrace.research.kta.client.dto.PlanResultDto;
import com.dynatrace.research.kta.client.dto.RequestDto;
import com.dynatrace.research.kta.client.dto.ResultDto;
import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.common.Mapper;
import com.dynatrace.research.kta.common.Union;
import com.dynatrace.research.kta.exception.ReconciliationFailedException;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.dynatrace.research.kta.operator.persistence.PlanResult;
import com.dynatrace.research.kta.operator.persistence.Result;
import io.smallrye.mutiny.tuples.Tuple2;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invokes the autoscaling algorithm (UDFs) for the Monitor, Analyze and Plan step of the MAPE-K
 * loop.
 */
public final class UdfInvocationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UdfInvocationHandler.class);

  private final UdfClient udfClient;
  private final PayloadDeserializer<Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>>>
      planPayloadDeserializer;

  public UdfInvocationHandler(
      final UdfClient udfClient,
      final PayloadDeserializer<Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>>>
          planPayloadDeserializer) {
    this.udfClient = udfClient;
    this.planPayloadDeserializer = planPayloadDeserializer;
  }

  public UdfResult invoke(
      final Clock clock,
      final String id,
      final List<? extends KtaPolicySpec.TopologyNode> topology,
      final List<Result> resultHistory,
      final KtaPolicySpec.Behavior.UserDefinedFunctions userDefinedFunctions) {
    long startTimestamp = clock.millis();
    final Supplier<RequestDto.RequestDtoBuilder> requestDtoBuilderSupplier =
        UdfInvocationHandler.createRequestDtoBuilderSupplier(
            id, startTimestamp, topology, resultHistory);
    final UdfResult.UdfResultBuilder resultBuilder =
        UdfResult.builder().withId(id).withUdfStartTimestampMillis(startTimestamp);

    CompletableFuture<Void> s = CompletableFuture.supplyAsync(() -> {
          {
            Map<String, Object> monitorResult =
                this.monitor(userDefinedFunctions.getMonitorEndpoint(), requestDtoBuilderSupplier);
            resultBuilder.withMonitorResult(monitorResult);
            return monitorResult;
          }
        })
        .thenApply(monitorResult -> {
          Map<String, Object> analyzeResult = this.analyze(
              userDefinedFunctions.getAnalyzeEndpoint(), requestDtoBuilderSupplier, monitorResult);
          resultBuilder.withAnalyzeResult(analyzeResult);
          return Tuple2.of(monitorResult, analyzeResult);
        })
        .thenApply(monitorResultAnalyzeResult -> this.plan(
            userDefinedFunctions.getPlanEndpoint(),
            requestDtoBuilderSupplier,
            monitorResultAnalyzeResult.getItem1(),
            monitorResultAnalyzeResult.getItem2()))
        .thenApply(Mapper::fromDTO)
        .thenAccept(u -> {
          Map<KtaPolicySpec.TopologyNode, PlanResult> planResult =
              Mapper.toCanonicalFormatOrElseThrow(u, topology);
          resultBuilder.withPlanResult(planResult);
          long endTimestamp = clock.millis();
          LOG.info("Invoking UDFs took {}ms", endTimestamp - startTimestamp);
          resultBuilder.withUdfEndTimestampMillis(endTimestamp);
        });

    try {
      s.get();
    } catch (ExecutionException | InterruptedException e) {
      LOG.error("Error during UDF invocation.", e);
      throw new ReconciliationFailedException("Error during UDF invocation.", e);
    }

    return resultBuilder.build();
  }

  private Map<String, Object> monitor(
      String endpoint, Supplier<RequestDto.RequestDtoBuilder> request) {
    RequestDto requestDto = request.get().build();
    Response<Map<String, Object>> response = this.udfClient.invoke(endpoint, requestDto);
    response.raiseForStatus();
    return Collections.unmodifiableMap(response.getPayload());
  }

  private Map<String, Object> analyze(
      String endpoint,
      Supplier<RequestDto.RequestDtoBuilder> request,
      Map<String, Object> monitorResult) {
    if (endpoint == null) {
      return Map.of();
    }
    RequestDto requestDto = request.get().withMonitorResult(monitorResult).build();
    Response<Map<String, Object>> response = this.udfClient.invoke(endpoint, requestDto);
    response.raiseForStatus();
    return Collections.unmodifiableMap(response.getPayload());
  }

  private Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>> plan(
      String endpoint,
      Supplier<RequestDto.RequestDtoBuilder> request,
      Map<String, Object> monitorResult,
      Map<String, Object> analyzeResult) {
    RequestDto requestDto = request
        .get()
        .withMonitorResult(monitorResult)
        .withAnalyzeResult(analyzeResult)
        .build();
    Response<Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>>> response =
        this.udfClient.invoke(endpoint, requestDto, this.planPayloadDeserializer);
    response.raiseForStatus();
    Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>> payload = response.getPayload();
    return Union.of(
        payload.first(),
        payload.second() == null ? null : Collections.unmodifiableMap(payload.second()));
  }

  @SuppressWarnings("unchecked")
  private static Supplier<RequestDto.RequestDtoBuilder> createRequestDtoBuilderSupplier(
      String id,
      long udfStartTimestampMillis,
      List<? extends KtaPolicySpec.TopologyNode> topologyNodes,
      List<Result> resultHistory) {
    return () -> RequestDto.builder()
        .withId(id)
        .withUdfStartTimestampMillis(udfStartTimestampMillis)
        .withTopologyNodes((List<TopologyNodeDto>) Mapper.toDto(topologyNodes))
        .withResultHistory((List<ResultDto>) Mapper.toDto(resultHistory));
  }
}
