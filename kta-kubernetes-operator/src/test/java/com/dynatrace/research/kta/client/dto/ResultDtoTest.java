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
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for {@link ResultDto}. */
public class ResultDtoTest extends TestBase {

  private static ObjectMapper objectMapper;
  private static TopologyNodeDto topologyNodeDTO;
  private static Map<TopologyNodeDto, PlanResultDto> planResultDTO;
  private static Map<TopologyNodeDto, Integer> parallelism;

  @BeforeAll
  static void beforeAll() {
    objectMapper = dependencyFactory.getObjectMapper();
    initializeTestFixtures();
  }

  private static void initializeTestFixtures() {
    topologyNodeDTO = new TopologyNodeDto(TopologyNodeDto.Type.SCALE_TARGET_REF, "key=value");
    planResultDTO = Map.of(topologyNodeDTO, new PlanResultDto(5));
    parallelism = Map.of(topologyNodeDTO, 2);
  }

  @Test
  void testSerialization() throws JsonProcessingException {
    ResultDto resultDTO;
    String json;
    resultDTO = ResultDto.builder()
        .withId("id-1")
        .withUdfStartTimestampMillis(1000)
        .withUdfEndTimestampMillis(2000)
        .withMonitorResult(Map.of())
        .withAnalyzeResult(Map.of())
        .withPlanResult(planResultDTO)
        .withParallelism(parallelism)
        .build();
    json = objectMapper.writeValueAsString(resultDTO);
    assertThat(json)
        .isEqualTo(
            "{\"id\":\"id-1\",\"udfStartTimestampMillis\":1000,\"udfEndTimestampMillis\":2000,\"monitorResult\":{},\"analyzeResult\":{},\"planResult\":{\"{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"key=value\\\"}\":{\"parallelism\":5}},\"parallelism\":{\"{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"key=value\\\"}\":2}}");

    resultDTO = ResultDto.builder()
        .withId("id-1")
        .withUdfStartTimestampMillis(1000)
        .withUdfEndTimestampMillis(2000)
        .withMonitorResult(Map.of("monitor-key", "value"))
        .withAnalyzeResult(Map.of("analyze-key", "value"))
        .withPlanResult(planResultDTO)
        .withParallelism(parallelism)
        .build();
    json = objectMapper.writeValueAsString(resultDTO);
    assertThat(json)
        .isEqualTo(
            "{\"id\":\"id-1\",\"udfStartTimestampMillis\":1000,\"udfEndTimestampMillis\":2000,\"monitorResult\":{\"monitor-key\":\"value\"},\"analyzeResult\":{\"analyze-key\":\"value\"},\"planResult\":{\"{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"key=value\\\"}\":{\"parallelism\":5}},\"parallelism\":{\"{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"key=value\\\"}\":2}}");
  }
}
