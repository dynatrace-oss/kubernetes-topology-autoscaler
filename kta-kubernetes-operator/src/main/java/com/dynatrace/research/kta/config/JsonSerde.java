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

package com.dynatrace.research.kta.config;

import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;

/** Custom serializers and deserializers for {@link ObjectMapper}. */
public final class JsonSerde {

  public static ObjectMapper objectMapper;

  static class TopologyNodeDTOSerializer extends JsonSerializer<TopologyNodeDto> {
    @Override
    public void serialize(
        final TopologyNodeDto topologyNodeDTO,
        final JsonGenerator jsonGenerator,
        final SerializerProvider serializerProvider)
        throws IOException {
      String json = objectMapper.writeValueAsString(topologyNodeDTO);
      jsonGenerator.writeFieldName(json);
    }
  }

  static class TopologyNodeDTOKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(final String key, final DeserializationContext ctxt)
        throws IOException {
      return objectMapper.readValue(key, TopologyNodeDto.class);
    }
  }

  static class TopologyNodeSerializer extends JsonSerializer<KtaPolicySpec.TopologyNode> {
    @Override
    public void serialize(
        final KtaPolicySpec.TopologyNode topologyNodeDTO,
        final JsonGenerator jsonGenerator,
        final SerializerProvider serializerProvider)
        throws IOException {
      String json = objectMapper.writeValueAsString(topologyNodeDTO);
      jsonGenerator.writeFieldName(json);
    }
  }

  static class TopologyNodeKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(final String key, final DeserializationContext ctxt)
        throws IOException {
      return objectMapper.readValue(key, KtaPolicySpec.TopologyNode.class);
    }
  }
}
