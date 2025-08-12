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
import com.dynatrace.research.kta.util.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for {@link PlanResultDto}. */
public class PlanResultDtoTest extends TestBase {
  private static ObjectMapper objectMapper;

  @BeforeAll
  static void beforeAll() {
    objectMapper = dependencyFactory.getObjectMapper();
  }

  @Test
  void testSerialization() throws JsonProcessingException {
    String serialized = objectMapper.writeValueAsString(new PlanResultDto(25));
    assertThat(serialized).isEqualTo("{\"parallelism\":25}");
  }

  @Test
  void testDeserialization() throws JsonProcessingException {
    PlanResultDto planResultDTO =
        objectMapper.readValue("{\"parallelism\":5}", PlanResultDto.class);
    assertThat(planResultDTO.getParallelism()).isEqualTo(5);
  }

  @Test
  void testValidationValidObject() {
    PlanResultDto planResultDTO = new PlanResultDto(5);
    TestUtils.assertThatNoConstraintIsViolated(planResultDTO);
  }

  @Test
  void testValidationInvalidObject() {
    PlanResultDto planResultDTO;
    planResultDTO = new PlanResultDto(0);
    TestUtils.assertThatAtLeastOneConstraintIsViolated(planResultDTO);
    planResultDTO = new PlanResultDto(-1);
    TestUtils.assertThatAtLeastOneConstraintIsViolated(planResultDTO);
  }
}
