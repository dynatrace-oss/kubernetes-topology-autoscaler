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

import com.dynatrace.research.kta.client.dto.RequestDto;
import com.dynatrace.research.kta.common.Condition;
import com.dynatrace.research.kta.exception.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link UdfClient} implementation for the HTTP protocol. */
public class HttpUdfClient implements UdfClient {

  private static final Logger LOG = LoggerFactory.getLogger(HttpUdfClient.class);

  private final PayloadDeserializer<Map<String, Object>> transparentPayloadDeserializer;
  private final ObjectMapper objectMapper;
  private final HttpClient client;
  private final Duration requestTimeout;

  public HttpUdfClient(
      final PayloadDeserializer<Map<String, Object>> transparentPayloadDeserializer,
      final ObjectMapper objectMapper,
      final HttpClient client,
      final Duration requestTimeout) {
    this.transparentPayloadDeserializer = transparentPayloadDeserializer;
    this.objectMapper = objectMapper;
    this.client = client;
    this.requestTimeout = requestTimeout;
  }

  @Override
  public Response<Map<String, Object>> invoke(final String url, final RequestDto requestDto) {
    return invoke(url, requestDto, this.transparentPayloadDeserializer);
  }

  @Override
  public <T> Response<T> invoke(
      final String url,
      final RequestDto requestDto,
      final PayloadDeserializer<T> payloadDeserializer) {
    Condition.notNull(url);
    Condition.notNull(requestDto);
    Condition.validConstraints(requestDto);

    HttpRequest httpRequest;
    String body;
    try {
      body = this.objectMapper.writeValueAsString(requestDto);
      LOG.debug("Request to {}: {}", url, body);
      httpRequest = HttpRequest.newBuilder(URI.create(url))
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .header("Content-Type", "application/json")
          .timeout(this.requestTimeout)
          .build();
    } catch (JsonProcessingException | IllegalArgumentException e) {
      throw new InternalOperatorErrorException("Error building HTTP request", e);
    }

    java.net.http.HttpResponse<String> httpResponse;
    try {
      httpResponse =
          this.client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
    } catch (HttpTimeoutException e) {
      throw new UdfInvocationTimeoutException("UDF invocation (" + url + ")  timed out.", e);
    } catch (IOException | InterruptedException e) {
      throw new UdfInvocationNotSuccessfulException(
          "UDF invocation (" + url + ") failed due to I/O error or unexpected interruption", e);
    } catch (IllegalArgumentException e) {
      throw new InternalOperatorErrorException("Error sending HTTP request.", e);
    }

    try {
      return new HttpResponse<>(
          httpResponse.statusCode(), payloadDeserializer.deserialize(httpResponse.body()));
    } catch (DeserializationException e) {
      throw new UnprocessableUdfPayloadException("Invalid response from endpoint " + url, e);
    }
  }

  public static class HttpResponse<T> extends Response<T> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponse.class);

    public HttpResponse(final int statusCode, final T payload) {
      super(statusCode, payload);
    }

    @Override
    public void raiseForStatus() {
      int statusCode = getStatusCode();

      if (statusCode / 100 != 2) {
        LOG.error("UDF responded with non-successful status code: {}", statusCode);
        throw new UdfInvocationNotSuccessfulException(
            "UDF responded with status code " + statusCode + ". Expected: 2XX");
      }
    }
  }
}
