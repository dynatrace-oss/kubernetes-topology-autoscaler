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

package com.dynatrace.research.kta.operator.persistence;

import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.dynatrace.research.kta.operator.udf.UdfResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Result of a single, complete MAPE-K loop iteration. */
public final class Result {

  @JsonProperty
  private String id;

  @JsonProperty
  private long udfStartTimestampMillis;

  @JsonProperty
  private long udfEndTimestampMillis;

  @JsonProperty
  private Map<String, Object> monitorResult;

  @JsonProperty
  private Map<String, Object> analyzeResult;

  @JsonProperty
  private Map<KtaPolicySpec.TopologyNode, PlanResult> planResult;

  @JsonProperty
  private Map<KtaPolicySpec.TopologyNode, Integer> parallelism;

  public Result() {
    // jackson
  }

  public Result(
      final String id,
      final long udfStartTimestampMillis,
      final long udfEndTimestampMillis,
      final Map<String, Object> monitorResult,
      final Map<String, Object> analyzeResult,
      final Map<KtaPolicySpec.TopologyNode, PlanResult> planResult,
      final Map<KtaPolicySpec.TopologyNode, Integer> parallelism) {
    this.id = id;
    this.udfStartTimestampMillis = udfStartTimestampMillis;
    this.udfEndTimestampMillis = udfEndTimestampMillis;
    this.monitorResult = monitorResult;
    this.analyzeResult = analyzeResult;
    this.planResult = planResult;
    this.parallelism = parallelism;
  }

  public String getId() {
    return this.id;
  }

  public long getUdfStartTimestampMillis() {
    return this.udfStartTimestampMillis;
  }

  public long getUdfEndTimestampMillis() {
    return this.udfEndTimestampMillis;
  }

  public Map<String, Object> getMonitorResult() {
    return this.monitorResult;
  }

  public Map<String, Object> getAnalyzeResult() {
    return this.analyzeResult;
  }

  public Map<KtaPolicySpec.TopologyNode, PlanResult> getPlanResult() {
    return this.planResult;
  }

  public Map<KtaPolicySpec.TopologyNode, Integer> getParallelism() {
    return this.parallelism;
  }

  public static ResultBuilder builder() {
    return new ResultBuilder();
  }

  public static ResultBuilder builder(UdfResult udfResult) {
    return new ResultBuilder()
        .withId(udfResult.getId())
        .withUdfStartTimestampMillis(udfResult.getUdfStartTimestampMillis())
        .withUdfEndTimestampMillis(udfResult.getUdfEndTimestampMillis())
        .withMonitorResult(udfResult.getMonitorResult())
        .withAnalyzeResult(udfResult.getAnalyzeResult())
        .withPlanResult(udfResult.getPlanResult());
  }

  public static final class ResultBuilder {
    private String id;
    private long udfStartTimestampMillis;
    private long udfEndTimestampMillis;
    private Map<String, Object> monitorResult;
    private Map<String, Object> analyzeResult;
    private Map<KtaPolicySpec.TopologyNode, PlanResult> planResult;
    private Map<KtaPolicySpec.TopologyNode, Integer> parallelism;

    private ResultBuilder() {}

    public ResultBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public ResultBuilder withUdfStartTimestampMillis(long udfStartTimestampMillis) {
      this.udfStartTimestampMillis = udfStartTimestampMillis;
      return this;
    }

    public ResultBuilder withUdfEndTimestampMillis(long udfEndTimestampMillis) {
      this.udfEndTimestampMillis = udfEndTimestampMillis;
      return this;
    }

    public ResultBuilder withMonitorResult(Map<String, Object> monitorResult) {
      this.monitorResult = monitorResult;
      return this;
    }

    public ResultBuilder withAnalyzeResult(Map<String, Object> analyzeResult) {
      this.analyzeResult = analyzeResult;
      return this;
    }

    public ResultBuilder withPlanResult(Map<KtaPolicySpec.TopologyNode, PlanResult> planResult) {
      this.planResult = planResult;
      return this;
    }

    public ResultBuilder withParallelism(Map<KtaPolicySpec.TopologyNode, Integer> parallelism) {
      this.parallelism = parallelism;
      return this;
    }

    public Result build() {
      return new Result(
          this.id,
          this.udfStartTimestampMillis,
          this.udfEndTimestampMillis,
          this.monitorResult,
          this.analyzeResult,
          this.planResult,
          this.parallelism);
    }
  }
}
