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

import com.dynatrace.research.kta.TestBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for {@link RequestDto}. */
public class RequestDtoTest extends TestBase {

  private static ObjectMapper objectMapper;
  private static List<TopologyNodeDto> topologyNodes;
  private static List<ResultDto> resultHistory;

  @BeforeAll
  static void beforeAll() {
    objectMapper = dependencyFactory.getObjectMapper();
    initializeTestFixtures();
  }

  private static void initializeTestFixtures() {
    topologyNodes = List.of(
        new TopologyNodeDto(TopologyNodeDto.Type.SCALE_TARGET_REF, "id=scale-target-ref-1"));
    resultHistory = List.of(ResultDto.builder()
        .withId("id-1")
        .withUdfStartTimestampMillis(0)
        .withUdfEndTimestampMillis(500)
        .withMonitorResult(Map.of("monitor-key", "value"))
        .withAnalyzeResult(Map.of("analyze-key", "value"))
        .withPlanResult(Map.of(topologyNodes.get(0), new PlanResultDto(5)))
        .withParallelism(Map.of(topologyNodes.get(0), 2))
        .build());
  }

  @Test
  void testSerialization() throws JsonProcessingException {
    RequestDto requestDTO = RequestDto.builder()
        .withId("id-2")
        .withUdfStartTimestampMillis(1000)
        .withTopologyNodes(topologyNodes)
        .withResultHistory(resultHistory)
        .build();
    String json = objectMapper.writeValueAsString(requestDTO);
    System.out.println(json);
    assertThat(json)
        .isEqualTo(
            "{\"id\":\"id-2\",\"udfStartTimestampMillis\":1000,\"topology\":[{\"type\":\"scaleTargetRef\",\"fields\":\"id=scale-target-ref-1\"}],\"monitorResult\":{},\"analyzeResult\":{},\"resultHistory\":[{\"id\":\"id-1\",\"udfStartTimestampMillis\":0,\"udfEndTimestampMillis\":500,\"monitorResult\":{\"monitor-key\":\"value\"},\"analyzeResult\":{\"analyze-key\":\"value\"},\"planResult\":{\"{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"id=scale-target-ref-1\\\"}\":{\"parallelism\":5}},\"parallelism\":{\"{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"id=scale-target-ref-1\\\"}\":2}}]}");
  }
}
