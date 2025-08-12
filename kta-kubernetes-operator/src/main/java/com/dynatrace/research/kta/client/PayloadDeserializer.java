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

package com.dynatrace.research.kta.client;

import com.dynatrace.research.kta.client.dto.PlanResultDto;
import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.common.Condition;
import com.dynatrace.research.kta.common.Union;
import com.dynatrace.research.kta.exception.ConditionViolationException;
import com.dynatrace.research.kta.exception.DeserializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Serializes the payload of responses received by a {@link UdfClient}.
 *
 * @param <T> Payload type
 */
public interface PayloadDeserializer<T> {

  T deserialize(String serializedPayload);

  class TransparentPayloadDeserializer implements PayloadDeserializer<Map<String, Object>> {

    private static final TypeReference<Map<String, Object>> TYPE =
        new TypeReference<Map<String, Object>>() {};

    private final ObjectMapper objectMapper;

    public TransparentPayloadDeserializer(final ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> deserialize(String serializedPayload) {
      try {
        return this.objectMapper.readValue(serializedPayload, TYPE);
      } catch (JsonProcessingException e) {
        throw new DeserializationException("Error deserializing " + serializedPayload, e);
      }
    }
  }

  class PlanPayloadDeserializer
      implements PayloadDeserializer<Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>>> {

    private static final TypeReference<Map<TopologyNodeDto, PlanResultDto>> TYPE =
        new TypeReference<Map<TopologyNodeDto, PlanResultDto>>() {};

    private final ObjectMapper objectMapper;

    public PlanPayloadDeserializer(final ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>> deserialize(
        String serializedPayload) {
      PlanResultDto maybePlanResultDto = null;
      Map<TopologyNodeDto, PlanResultDto> maybePlanResultDtoByScaleTargetRef = null;

      try {
        maybePlanResultDto = this.objectMapper.readValue(serializedPayload, PlanResultDto.class);
        try {
          Condition.validConstraints(maybePlanResultDto);
        } catch (ConditionViolationException e) {
          throw new DeserializationException("Error deserializing " + serializedPayload, e);
        }
      } catch (JsonProcessingException e1) {
        try {
          maybePlanResultDtoByScaleTargetRef = this.objectMapper.readValue(serializedPayload, TYPE);
        } catch (JsonProcessingException e2) {
          throw new DeserializationException("Error deserializing " + serializedPayload, e2);
        }
      }
      assert maybePlanResultDto != null || maybePlanResultDtoByScaleTargetRef != null;

      return new Union<>(maybePlanResultDto, maybePlanResultDtoByScaleTargetRef);
    }
  }
}
