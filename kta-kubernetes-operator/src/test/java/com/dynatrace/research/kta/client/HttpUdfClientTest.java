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

import static org.assertj.core.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.dynatrace.research.kta.TestBase;
import com.dynatrace.research.kta.client.dto.RequestDto;
import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import shaded_package.io.swagger.models.HttpMethod;

/** Tests for {@link HttpUdfClient}. */
public class HttpUdfClientTest extends TestBase {

  private static final String SERVER_ADDRESS = "http://127.0.0.1:";
  private static final String API_PREFIX = "/api/test";

  private static ClientAndServer server;
  private static String serverBaseUrl;
  private static UdfClient udfClient;

  private static RequestDto validRequestDto;
  private static RequestDto invalidRequestDto;

  @BeforeAll
  static void beforeAll() throws Exception {
    ObjectMapper objectMapper = dependencyFactory.getObjectMapper();
    udfClient = new HttpUdfClient(
        new PayloadDeserializer.TransparentPayloadDeserializer(objectMapper),
        objectMapper,
        dependencyFactory.getHttpClient(),
        Duration.ofSeconds(3));
    server = startClientAndServer();
    serverBaseUrl = SERVER_ADDRESS + server.getLocalPort() + API_PREFIX;
    setUpServer();
    initializeTestFixtures();
  }

  @AfterAll
  static void afterAll() throws Exception {
    server.stop();
  }

  private static void initializeTestFixtures() {
    validRequestDto = RequestDto.builder()
        .withId("id-1;")
        .withUdfStartTimestampMillis(5000)
        .withResultHistory(List.of())
        .withTopologyNodes(List.of(new TopologyNodeDto(
            TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref")))
        .build();
    invalidRequestDto =
        RequestDto.builder().withId("id-1").withUdfStartTimestampMillis(5000).build();
  }

  private static void setUpServer() {
    server
        .when(request().withPath(API_PREFIX + "/valid-response").withMethod(HttpMethod.POST.name()))
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    "{\"key-str-value\": \"value-1\", \"key-list-value\": [1, 2, 3], \"key-null-value\": null, \"key-obj-value\": {\"key-str-value\": \"nested-value\"}}"));
    server
        .when(request()
            .withPath(API_PREFIX + "/valid-response-with-empty-json-payload")
            .withMethod(HttpMethod.POST.name()))
        .respond(response()
            .withStatusCode(200)
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("{}"));
    server
        .when(request().withPath(API_PREFIX + "/pretends-to-be-a-json-response"))
        .respond(response()
            .withContentType(MediaType.APPLICATION_JSON)
            .withBody("just-plain-text-and-not-a-json"));
    server
        .when(request().withPath(API_PREFIX + "/no-payload"))
        .respond(response().withContentType(MediaType.APPLICATION_JSON));
    server
        .when(request().withPath(API_PREFIX + "/response-with-error-status"))
        .respond(response().withStatusCode(500).withBody("{\"message\": \"error-message\"}"));
    server
        .when(request().withPath(API_PREFIX + "/timeout"))
        .respond(response().withDelay(TimeUnit.MILLISECONDS, 7500));
    server.when(request()).respond(response().withStatusCode(500));
  }

  @Test
  void testValidatesArguments() {
    assertThatThrownBy(() -> udfClient.invoke(null, validRequestDto))
        .isInstanceOf(ConditionViolationException.class);
    assertThatThrownBy(() -> udfClient.invoke(serverBaseUrl, null))
        .isInstanceOf(ConditionViolationException.class);
    assertThatThrownBy(() -> udfClient.invoke(serverBaseUrl, invalidRequestDto))
        .isInstanceOf(ConditionViolationException.class);
    assertThatCode(() -> udfClient.invoke(serverBaseUrl + "/valid-response", validRequestDto))
        .doesNotThrowAnyException();
  }

  @Test
  void testReturnsValidServerResponseWithNonEmptyPayload() {
    Response<Map<String, Object>> responseDTO =
        udfClient.invoke(serverBaseUrl + "/valid-response", validRequestDto);
    assertThatCode(responseDTO::raiseForStatus).doesNotThrowAnyException();
    Map<String, Object> payload = responseDTO.getPayload();
    assertThat(payload.size()).isEqualTo(4);
    // strings are recognized
    assertThat(payload.get("key-str-value")).isEqualTo("value-1");
    // lists are recognized
    assertThat(((List<?>) payload.get("key-list-value")).size()).isEqualTo(3);
    // numbers are recognized
    assertThat(((List<?>) payload.get("key-list-value")).get(0)).isEqualTo(1);
    // null is recognized
    assertThat(payload.get("key-null-value")).isNull();
    // nested objects are recognized
    assertThat(((Map<?, ?>) payload.get("key-obj-value")).get("key-str-value"))
        .isEqualTo("nested-value");
  }

  @Test
  void testReturnsValidServerResponseOnEmptyPayload() {
    assertThatCode(() -> udfClient.invoke(
            serverBaseUrl + "/valid-response-with-empty-json-payload", validRequestDto))
        .doesNotThrowAnyException();
  }

  @Test
  void testThrowsOnInvalidPayload() {
    assertThatThrownBy(() ->
            udfClient.invoke(serverBaseUrl + "/pretends-to-be-a-json-response", validRequestDto))
        .isInstanceOf(UnprocessableUdfPayloadException.class);

    assertThatThrownBy(() -> udfClient.invoke(serverBaseUrl + "/no-payload", validRequestDto))
        .isInstanceOf(UnprocessableUdfPayloadException.class);
  }

  @Test
  void testRaiseForStatusThrowsOnNonSuccessfulStatusCode() {
    Response<Map<String, Object>> responseDTO =
        udfClient.invoke(serverBaseUrl + "/response-with-error-status", validRequestDto);

    assertThatThrownBy(responseDTO::raiseForStatus)
        .isInstanceOf(UdfInvocationNotSuccessfulException.class);

    assertThat(responseDTO.getPayload().get("message")).isEqualTo("error-message");
  }

  @Test
  void testTimeout() {
    assertThatThrownBy(() -> udfClient.invoke(serverBaseUrl + "/timeout", validRequestDto))
        .isInstanceOf(UdfInvocationTimeoutException.class);
  }

  @Test
  void testInvalidUrl() {
    assertThatThrownBy(() -> udfClient.invoke("INVALID_URL", validRequestDto))
        .isInstanceOf(InternalOperatorErrorException.class);
  }
}
