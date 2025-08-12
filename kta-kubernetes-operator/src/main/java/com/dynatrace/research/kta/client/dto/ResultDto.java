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

package com.dynatrace.research.kta.client.dto;

import com.dynatrace.research.kta.operator.persistence.Result;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;

/** Data transfer object for {@link Result}. */
public class ResultDto implements DataTransferObject {
  @JsonProperty
  @NotNull @NotEmpty @NotBlank private final String id;

  @PositiveOrZero @JsonProperty
  private final long udfStartTimestampMillis;

  @PositiveOrZero @JsonProperty
  private final long udfEndTimestampMillis;

  @JsonProperty
  @NotEmpty private final Map<@NotNull String, Object> monitorResult;

  @JsonProperty
  @NotEmpty private final Map<@NotNull String, Object> analyzeResult;

  @JsonProperty
  @NotEmpty private final Map<@NotNull TopologyNodeDto, PlanResultDto> planResult;

  @JsonProperty
  @NotEmpty private final Map<@NotNull TopologyNodeDto, Integer> parallelism;

  private ResultDto(
      final String id,
      final long udfStartTimestampMillis,
      final long udfEndTimestampMillis,
      final Map<String, Object> monitorResult,
      final Map<String, Object> analyzeResult,
      final Map<TopologyNodeDto, PlanResultDto> planResult,
      final Map<TopologyNodeDto, Integer> parallelism) {
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

  public Map<TopologyNodeDto, PlanResultDto> getPlanResult() {
    return this.planResult;
  }

  public Map<TopologyNodeDto, Integer> getParallelism() {
    return this.parallelism;
  }

  public static ResultDTOBuilder builder() {
    return new ResultDTOBuilder();
  }

  public static final class ResultDTOBuilder {
    private String id;
    private long udfStartTimestampMillis;
    private long udfEndTimestampMillis;
    private Map<String, Object> monitorResult;
    private Map<String, Object> analyzeResult;
    private Map<TopologyNodeDto, PlanResultDto> planResult;
    private Map<TopologyNodeDto, Integer> parallelism;

    private ResultDTOBuilder() {}

    public ResultDTOBuilder withId(final String id) {
      this.id = id;
      return this;
    }

    public ResultDTOBuilder withUdfStartTimestampMillis(final long udfStartTimestampMillis) {
      this.udfStartTimestampMillis = udfStartTimestampMillis;
      return this;
    }

    public ResultDTOBuilder withUdfEndTimestampMillis(final long udfEndTimestampMillis) {
      this.udfEndTimestampMillis = udfEndTimestampMillis;
      return this;
    }

    public ResultDTOBuilder withMonitorResult(final Map<String, Object> monitorResult) {
      this.monitorResult = monitorResult;
      return this;
    }

    public ResultDTOBuilder withAnalyzeResult(final Map<String, Object> analyzeResult) {
      this.analyzeResult = analyzeResult;
      return this;
    }

    public ResultDTOBuilder withPlanResult(final Map<TopologyNodeDto, PlanResultDto> planResult) {
      this.planResult = planResult;
      return this;
    }

    public ResultDTOBuilder withParallelism(final Map<TopologyNodeDto, Integer> parallelism) {
      this.parallelism = parallelism;
      return this;
    }

    public ResultDto build() {
      return new ResultDto(
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
