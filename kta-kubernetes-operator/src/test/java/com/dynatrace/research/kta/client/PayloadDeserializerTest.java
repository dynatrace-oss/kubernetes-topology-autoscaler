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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dynatrace.research.kta.TestBase;
import com.dynatrace.research.kta.client.dto.PlanResultDto;
import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.common.Union;
import com.dynatrace.research.kta.exception.DeserializationException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link PayloadDeserializer}. */
public class PayloadDeserializerTest {

  @Nested
  class TransparentPayloadDeserializerTest extends TestBase {

    private static PayloadDeserializer<Map<String, Object>> payloadDeserializer;

    @BeforeAll
    static void beforeAll() {
      payloadDeserializer = dependencyFactory.getTransparentPayloadDeserializer();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidInput() {
      Map<String, Object> payload;
      payload = payloadDeserializer.deserialize("{}");
      assertThat(payload.isEmpty()).isTrue();
      payload = payloadDeserializer.deserialize(
          "{\"key-1\": \"value\", \"key-2\": 10, \"key-3\": {}, \"key-4\": {\"key-5\": null}}");
      assertThat(payload.isEmpty()).isFalse();
      assertThat(payload.get("key-1")).isEqualTo("value");
      assertThat(payload.get("key-2")).isEqualTo(10);
      assertThat(((Map<?, ?>) payload.get("key-3")).isEmpty()).isTrue();
      assertThat(((Map<String, ?>) payload.get("key-4")).get("key-5")).isNull();
    }

    @Test
    void testInvalidInput() {
      assertThatThrownBy(() -> payloadDeserializer.deserialize(""))
          .isInstanceOf(DeserializationException.class);
      assertThatThrownBy(() -> payloadDeserializer.deserialize("{malformed-json: 10}"))
          .isInstanceOf(DeserializationException.class);
    }
  }

  @Nested
  class PlanPayloadDeserializerTest extends TestBase {

    private static PayloadDeserializer<Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>>>
        payloadDeserializer;

    @BeforeAll
    static void beforeAll() {
      payloadDeserializer =
          new PayloadDeserializer.PlanPayloadDeserializer(dependencyFactory.getObjectMapper());
    }

    @Test
    void testValidInput() {
      Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>> payload;
      payload = payloadDeserializer.deserialize("{\"parallelism\": 5}");
      assertThat(payload.isFirst()).isTrue();
      assertThat(payload.first().getParallelism()).isEqualTo(5);
      payload = payloadDeserializer.deserialize(
          "{\"{\\\"type\\\":\\\"scaleTargetRef\\\",\\\"fields\\\":\\\"key=value\\\"}\": 10}");
      assertThat(payload.isSecond()).isTrue();
      assertThat(payload
              .second()
              .get(new TopologyNodeDto(TopologyNodeDto.Type.SCALE_TARGET_REF, "key=value"))
              .getParallelism())
          .isEqualTo(10);
    }

    @Test
    void testInvalidInput() {
      assertThatThrownBy(() -> payloadDeserializer.deserialize(""))
          .isInstanceOf(DeserializationException.class);
      assertThatThrownBy(() -> payloadDeserializer.deserialize("{}"))
          .isInstanceOf(DeserializationException.class);
      assertThatThrownBy(() -> payloadDeserializer.deserialize("{\"not-parallelism\": 5}"))
          .isInstanceOf(DeserializationException.class);
      assertThatThrownBy(() -> payloadDeserializer.deserialize("not-a-json"))
          .isInstanceOf(DeserializationException.class);
    }
  }
}
