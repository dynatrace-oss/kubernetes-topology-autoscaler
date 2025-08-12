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

import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.dynatrace.research.kta.operator.persistence.PlanResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Result of the Monitor, Analyze and Plan step of the MAPE-K loop. */
public final class UdfResult {

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

  public UdfResult() {
    // jackson
  }

  public UdfResult(
      final String id,
      final long udfStartTimestampMillis,
      final long udfEndTimestampMillis,
      final Map<String, Object> monitorResult,
      final Map<String, Object> analyzeResult,
      final Map<KtaPolicySpec.TopologyNode, PlanResult> planResult) {
    this.id = id;
    this.udfStartTimestampMillis = udfStartTimestampMillis;
    this.udfEndTimestampMillis = udfEndTimestampMillis;
    this.monitorResult = monitorResult;
    this.analyzeResult = analyzeResult;
    this.planResult = planResult;
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

  public static UdfResultBuilder builder() {
    return new UdfResultBuilder();
  }

  public static UdfResultBuilder builder(UdfResult result) {
    return new UdfResultBuilder()
        .withId(result.getId())
        .withUdfStartTimestampMillis(result.getUdfStartTimestampMillis())
        .withUdfEndTimestampMillis(result.getUdfEndTimestampMillis())
        .withMonitorResult(result.getMonitorResult())
        .withAnalyzeResult(result.getAnalyzeResult())
        .withPlanResult(result.getPlanResult());
  }

  public static final class UdfResultBuilder {
    private String id;
    private long udfStartTimestampMillis;
    private long udfEndTimestampMillis;
    private Map<String, Object> monitorResult;
    private Map<String, Object> analyzeResult;
    private Map<KtaPolicySpec.TopologyNode, PlanResult> planResult;

    private UdfResultBuilder() {}

    public UdfResultBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public UdfResultBuilder withUdfStartTimestampMillis(long udfStartTimestampMillis) {
      this.udfStartTimestampMillis = udfStartTimestampMillis;
      return this;
    }

    public UdfResultBuilder withUdfEndTimestampMillis(long udfEndTimestampMillis) {
      this.udfEndTimestampMillis = udfEndTimestampMillis;
      return this;
    }

    public UdfResultBuilder withMonitorResult(Map<String, Object> monitorResult) {
      this.monitorResult = monitorResult;
      return this;
    }

    public UdfResultBuilder withAnalyzeResult(Map<String, Object> analyzeResult) {
      this.analyzeResult = analyzeResult;
      return this;
    }

    public UdfResultBuilder withPlanResult(Map<KtaPolicySpec.TopologyNode, PlanResult> planResult) {
      this.planResult = planResult;
      return this;
    }

    public UdfResult build() {
      return new UdfResult(
          this.id,
          this.udfStartTimestampMillis,
          this.udfEndTimestampMillis,
          this.monitorResult,
          this.analyzeResult,
          this.planResult);
    }
  }
}
