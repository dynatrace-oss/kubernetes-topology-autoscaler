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

import com.dynatrace.research.kta.client.PayloadDeserializer.TransparentPayloadDeserializer;
import com.dynatrace.research.kta.client.dto.RequestDto;
import com.dynatrace.research.kta.exception.UdfInvocationNotSuccessfulException;
import com.dynatrace.research.kta.exception.UdfInvocationTimeoutException;
import com.dynatrace.research.kta.exception.UnprocessableUdfPayloadException;
import java.util.Map;

/** Protocol-agnostic interface for UDF clients. */
public interface UdfClient {

  /**
   * Invokes the UDF at the given URL using {@link RequestDto} as payload. Deserializes the response
   * using {@link TransparentPayloadDeserializer}.
   *
   * @param url The url.
   * @param requestDto The request DTO.
   * @return The UDF response.
   * @throws UdfInvocationTimeoutException When the invocation times out.
   * @throws UdfInvocationNotSuccessfulException When the request fails due to a different reason
   *     than a timeout or the response contains a status code that indicates a failure.
   * @throws UnprocessableUdfPayloadException When the request succeeded (i.e., the response
   *     contains a status code that indicates a success), but the payload could not be processed.
   */
  Response<Map<String, Object>> invoke(final String url, final RequestDto requestDto);

  /**
   * Invokes the UDF at the given URL using the given {@link RequestDto} as payload. Deserializes
   * the response using the {@link PayloadDeserializer}.
   *
   * @param url The url.
   * @param requestDto The request DTO.
   * @param payloadDeserializer The payload deserializer.
   * @return The response.
   * @throws UdfInvocationTimeoutException When the invocation times out.
   * @throws UdfInvocationNotSuccessfulException When the request fails due to a different reason
   *     than a timeout or the response contains a status code that indicates a failure.
   * @throws UnprocessableUdfPayloadException When the request succeeded (i.e., the response
   *     contains a status code that indicates a success), but the payload could not be processed.
   */
  <T> Response<T> invoke(
      final String url,
      final RequestDto requestDto,
      final PayloadDeserializer<T> payloadDeserializer);
}
