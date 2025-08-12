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

import com.dynatrace.research.kta.TestBase;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for {@link FlinkScaleDriver}. */
public class FlinkScaleDriverTest extends TestBase {

  private static ObjectMapper objectMapper;

  @BeforeAll
  static void beforeAll() {
    objectMapper = dependencyFactory.getObjectMapper();
  }

  @Test
  void testCreateRequestBody() throws JsonProcessingException {
    List<KtaPolicySpec.FlinkStreamingGraphNode> flinkStreamingGraphNodes = List.of(
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
        new KtaPolicySpec.FlinkStreamingGraphNode("id-2"));
    Map<KtaPolicySpec.TopologyNode, Integer> parallelism = Map.of(
        new KtaPolicySpec.FlinkStreamingGraphNode("id-2"),
        5,
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
        10);
    String json =
        FlinkScaleDriver.createRequestBody(objectMapper, flinkStreamingGraphNodes, parallelism);
    assertThat(json).startsWith("{");
    assertThat(json).endsWith("}");
    Map<String, Map<String, FlinkScaleDriver.FlinkResourceRequirements>> jsonDeserialized =
        objectMapper.readValue(
            json,
            new TypeReference<
                Map<String, Map<String, FlinkScaleDriver.FlinkResourceRequirements>>>() {});
    assertThat(jsonDeserialized.get("id-1").get("parallelism").getLowerBound()).isEqualTo(10);
    assertThat(jsonDeserialized.get("id-1").get("parallelism").getUpperBound()).isEqualTo(10);
    assertThat(jsonDeserialized.get("id-2").get("parallelism").getLowerBound()).isEqualTo(5);
    assertThat(jsonDeserialized.get("id-2").get("parallelism").getUpperBound()).isEqualTo(5);
  }
}
