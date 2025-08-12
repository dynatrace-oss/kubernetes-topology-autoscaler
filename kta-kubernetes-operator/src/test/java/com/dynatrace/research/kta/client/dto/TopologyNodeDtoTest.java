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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dynatrace.research.kta.TestBase;
import com.dynatrace.research.kta.util.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Tests for {@link TopologyNodeDto}. */
public class TopologyNodeDtoTest extends TestBase {

  private static ObjectMapper objectMapper = dependencyFactory.getObjectMapper();
  private static TypeReference<Map<TopologyNodeDto, Object>> typeReference;

  @BeforeAll
  static void beforeAll() {
    objectMapper = dependencyFactory.getObjectMapper();
    typeReference = new TypeReference<Map<TopologyNodeDto, Object>>() {};
  }

  @Test
  void testSerialization() throws JsonProcessingException {
    TopologyNodeDto topologyNodeDTO;
    String json;

    // single key value pair
    topologyNodeDTO = new TopologyNodeDto(TopologyNodeDto.Type.SCALE_TARGET_REF, "key=value");
    json = objectMapper.writeValueAsString(topologyNodeDTO);
    assertThat(json).isEqualTo("{\"type\":\"scaleTargetRef\",\"fields\":\"key=value\"}");
    topologyNodeDTO =
        new TopologyNodeDto(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE, "key=value");
    json = objectMapper.writeValueAsString(topologyNodeDTO);
    assertThat(json).isEqualTo("{\"type\":\"flinkStreamingGraphNode\",\"fields\":\"key=value\"}");

    // multiple key value pairs
    topologyNodeDTO = new TopologyNodeDto(
        TopologyNodeDto.Type.SCALE_TARGET_REF, "key1=value1", "key2=value2", "key3=value3");
    json = objectMapper.writeValueAsString(topologyNodeDTO);
    assertThat(json)
        .isEqualTo(
            "{\"type\":\"scaleTargetRef\",\"fields\":\"key1=value1|key2=value2|key3=value3\"}");
    topologyNodeDTO = new TopologyNodeDto(
        TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE,
        "key1=value1",
        "key2=value2",
        "key3=value3");
    json = objectMapper.writeValueAsString(topologyNodeDTO);
    assertThat(json)
        .isEqualTo(
            "{\"type\":\"flinkStreamingGraphNode\",\"fields\":\"key1=value1|key2=value2|key3=value3\"}");
  }

  @Test
  void testDeserializationValue() throws JsonProcessingException {
    TopologyNodeDto topologyNodeDTO;
    topologyNodeDTO = objectMapper.readValue(
        "{\"type\":\"scaleTargetRef\",\"fields\":\"key1=value1|key2=value2|key3=value3\"}",
        TopologyNodeDto.class);
    assertThat(topologyNodeDTO.getType()).isEqualTo(TopologyNodeDto.Type.SCALE_TARGET_REF);
    assertThat(topologyNodeDTO.getFields()).isEqualTo("key1=value1|key2=value2|key3=value3");
    topologyNodeDTO = objectMapper.readValue(
        "{\"type\":\"flinkStreamingGraphNode\",\"fields\":\"key1=value1|key2=value2|key3=value3\"}",
        TopologyNodeDto.class);
    assertThat(topologyNodeDTO.getType())
        .isEqualTo(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE);
    assertThat(topologyNodeDTO.getFields()).isEqualTo("key1=value1|key2=value2|key3=value3");
  }

  @Test
  void testDeserializationKey() throws JsonProcessingException {
    String key;
    String json;
    Map<TopologyNodeDto, Object> map;

    // valid
    key =
        "{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"key1=value1|key2=value2|key3=value3\\\"}";
    json = "{\"" + key + "\":5}";
    System.out.println(json);
    map = objectMapper.readValue(json, typeReference);
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.keySet().iterator().next().getType())
        .isEqualTo(TopologyNodeDto.Type.SCALE_TARGET_REF);
    assertThat(map.keySet().iterator().next().getFields())
        .isEqualTo("key1=value1|key2=value2|key3=value3");
    assertThat(map.values().iterator().next()).isEqualTo(5);
    key =
        "{\\\"type\\\":\\\"flinkStreamingGraphNode\\\",\\\"fields\\\":\\\"key1=value1|key2=value2|key3=value3\\\"}";
    json = "{\"" + key + "\":5}";
    System.out.println(json);
    map = objectMapper.readValue(json, typeReference);
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.keySet().iterator().next().getType())
        .isEqualTo(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE);
    assertThat(map.keySet().iterator().next().getFields())
        .isEqualTo("key1=value1|key2=value2|key3=value3");
    assertThat(map.values().iterator().next()).isEqualTo(5);

    // invalid
    key =
        "{\\\"type\\\":\\\"invalid-type\\\",\\\"fields\\\":\\\"key1=value1|key2=value2|key3=value3\\\"}";
    final String jsonWithInvalidKey = "{\"" + key + "\":5}";
    assertThatThrownBy(() -> objectMapper.readValue(jsonWithInvalidKey, typeReference))
        .isInstanceOf(JsonProcessingException.class);
  }

  @ParameterizedTest
  @EnumSource(TopologyNodeDto.Type.class)
  void testValidationValidObject(TopologyNodeDto.Type type) {
    TopologyNodeDto topologyNodeDTO = new TopologyNodeDto(type, "key=value");
    TestUtils.assertThatNoConstraintIsViolated(topologyNodeDTO);
  }

  @Test
  void testValidationInvalidObject() {
    TopologyNodeDto topologyNodeDTO;
    topologyNodeDTO = new TopologyNodeDto(null, "key=value");
    TestUtils.assertThatAtLeastOneConstraintIsViolated(topologyNodeDTO);

    topologyNodeDTO = new TopologyNodeDto(TopologyNodeDto.Type.SCALE_TARGET_REF, "");
    TestUtils.assertThatAtLeastOneConstraintIsViolated(topologyNodeDTO);

    topologyNodeDTO = new TopologyNodeDto(TopologyNodeDto.Type.SCALE_TARGET_REF, "    ");
    TestUtils.assertThatAtLeastOneConstraintIsViolated(topologyNodeDTO);
  }
}
