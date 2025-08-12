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

import com.dynatrace.research.kta.operator.KtaPolicySpec.TopologyNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/** Data transfer object for {@link TopologyNode}. */
public class TopologyNodeDto implements DataTransferObject {

  private static final String FIELD_DELIMITER = "|";
  private static final String FIELD_DELIMITER_REGEX = "\\|";

  public enum Type {
    SCALE_TARGET_REF("scaleTargetRef"),
    FLINK_STREAMING_GRAPH_NODE("flinkStreamingGraphNode");

    private final String value;

    Type(final String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return this.value;
    }
  }

  @NotNull @JsonProperty
  private Type type;

  @NotNull @NotEmpty @NotBlank @JsonProperty
  private String fields;

  public TopologyNodeDto() {
    // Jackson
  }

  public TopologyNodeDto(final Type type, final String... keyValuePairs) {
    this.type = type;
    this.fields = String.join(FIELD_DELIMITER, keyValuePairs);
  }

  public Type getType() {
    return this.type;
  }

  public String getFields() {
    return this.fields;
  }

  public void setFields(final String fields) {
    this.fields = fields;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  @Override
  public final boolean equals(final Object o) {
    if (!(o instanceof final TopologyNodeDto that)) {
      return false;
    }

    return this.type == that.type && this.fields.equals(that.fields);
  }

  @Override
  public int hashCode() {
    int result = this.type.hashCode();
    result = 31 * result + this.fields.hashCode();
    return result;
  }

  public static Map<String, String> extractFieldComponents(String fields) {
    String[] keyValueArr = fields.split(FIELD_DELIMITER_REGEX);
    Map<String, String> components = new HashMap<>();
    for (final String s : keyValueArr) {
      String[] keyValue = s.split("=");
      components.put(keyValue[0], keyValue[1]);
    }
    return components;
  }
}
