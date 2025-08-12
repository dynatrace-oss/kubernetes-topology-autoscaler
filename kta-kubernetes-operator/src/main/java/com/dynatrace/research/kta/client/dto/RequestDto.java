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

import com.dynatrace.research.kta.common.Condition;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for the UDF request itself. Contains built-in, serializable Java types or
 * other {@link DataTransferObject}s.
 */
public class RequestDto implements DataTransferObject {

  @NotNull @NotEmpty @NotBlank @JsonProperty
  private final String id;

  @PositiveOrZero @JsonProperty
  private final long udfStartTimestampMillis;

  @NotNull @Size(min = 1) @JsonProperty
  private final List<@NotNull TopologyNodeDto> topology;

  @JsonProperty
  @Nullable private final Map<@NotNull String, Object> monitorResult;

  @JsonProperty
  @Nullable private final Map<@NotNull String, Object> analyzeResult;

  @NotNull @JsonProperty
  private final List<@NotNull ResultDto> resultHistory;

  private RequestDto(
      final String id,
      final long udfStartTimestampMillis,
      final List<TopologyNodeDto> topology,
      final Map<String, Object> monitorResult,
      final Map<String, Object> analyzeResult,
      final List<ResultDto> resultHistory) {
    this.id = id;
    this.udfStartTimestampMillis = udfStartTimestampMillis;
    this.topology = topology;
    this.monitorResult = monitorResult;
    this.analyzeResult = analyzeResult;
    this.resultHistory = resultHistory;
  }

  public static RequestDtoBuilder builder() {
    return new RequestDtoBuilder();
  }

  public static final class RequestDtoBuilder {
    private String id;
    private Long udfStartTimestampMillis;
    private List<TopologyNodeDto> scaleTargetRefs;
    private List<ResultDto> resultHistory = List.of();
    private Map<String, Object> monitorResult = Collections.emptyMap();
    private Map<String, Object> analyzeResult = Collections.emptyMap();

    private RequestDtoBuilder() {}

    private RequestDtoBuilder(final RequestDto requestDto) {
      this.id = requestDto.id;
      this.udfStartTimestampMillis = requestDto.udfStartTimestampMillis;
      this.scaleTargetRefs = requestDto.topology;
      this.monitorResult = requestDto.monitorResult;
      this.analyzeResult = requestDto.analyzeResult;
      this.resultHistory = requestDto.resultHistory;
    }

    public RequestDtoBuilder withId(final String id) {
      this.id = Condition.notNull(id);
      return this;
    }

    public RequestDtoBuilder withUdfStartTimestampMillis(final long udfStartTimestampMillis) {
      this.udfStartTimestampMillis = udfStartTimestampMillis;
      return this;
    }

    public RequestDtoBuilder withTopologyNodes(final List<TopologyNodeDto> scaleTargetRefs) {
      this.scaleTargetRefs = Collections.unmodifiableList(scaleTargetRefs);
      return this;
    }

    public RequestDtoBuilder withMonitorResult(final Map<String, Object> monitorResult) {
      this.monitorResult = Condition.notNull(monitorResult);
      return this;
    }

    public RequestDtoBuilder withAnalyzeResult(final Map<String, Object> analyzeResult) {
      this.analyzeResult = Condition.notNull(analyzeResult);
      return this;
    }

    public RequestDtoBuilder withResultHistory(final List<ResultDto> resultHistory) {
      this.resultHistory = Condition.notNull(resultHistory);
      return this;
    }

    public RequestDto build() {
      return new RequestDto(
          this.id,
          this.udfStartTimestampMillis,
          this.scaleTargetRefs,
          this.monitorResult,
          this.analyzeResult,
          this.resultHistory);
    }
  }
}
