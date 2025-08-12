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

package com.dynatrace.research.kta.operator.udf;

import static org.assertj.core.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.dynatrace.research.kta.exception.ReconciliationFailedException;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.MediaType;
import shaded_package.io.swagger.models.HttpMethod;

/** Tests for {@link UdfInvocationHandler}. */
public class UdfInvocationHandlerTest {

  @Nested
  class UdfInvocationHandlerValidTest extends UdfInvocationHandlerTestBase {

    void setUpServer(boolean withAnalyze, long responseDelayMilliseconds) {
      ClientAndServer server = getServer();
      server
          .when(request().withPath(API_PREFIX + "/monitor").withMethod(HttpMethod.POST.name()))
          .respond(response()
              .withDelay(TimeUnit.MILLISECONDS, responseDelayMilliseconds)
              .withStatusCode(200)
              .withContentType(MediaType.APPLICATION_JSON)
              .withBody("{\"monitor-key\": \"monitor-value\"}"));
      if (withAnalyze) {
        server
            .when(request().withPath(API_PREFIX + "/analyze").withMethod(HttpMethod.POST.name()))
            .respond(response()
                .withDelay(TimeUnit.MILLISECONDS, responseDelayMilliseconds)
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"analyze-key\": \"analyze-value\"}"));
      }
      server
          .when(request().withPath(API_PREFIX + "/plan").withMethod(HttpMethod.POST.name()))
          .respond(response()
              .withDelay(TimeUnit.MILLISECONDS, responseDelayMilliseconds)
              .withStatusCode(200)
              .withContentType(MediaType.APPLICATION_JSON)
              .withBody("{\"parallelism\": 5}"));
      server.when(request()).respond(response().withStatusCode(500));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1000})
    void testInvokeMonitorAnalyzePlanValid(long responseDelay) {
      setUpServer(true, responseDelay);
      KtaPolicySpec.Behavior.UserDefinedFunctions udfs =
          new KtaPolicySpec.Behavior.UserDefinedFunctions();
      udfs.setMonitorEndpoint(getServerBaseUrl() + "/monitor");
      udfs.setAnalyzeEndpoint(getServerBaseUrl() + "/analyze");
      udfs.setPlanEndpoint(getServerBaseUrl() + "/plan");
      UdfResult udfResult = this.udfInvocationHandler.invoke(
          Clock.systemUTC(),
          "id-1",
          List.of(new KtaPolicySpec.ScaleTargetRef(
              KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")),
          List.of(),
          udfs);
      assertThat(udfResult.getMonitorResult().get("monitor-key")).isEqualTo("monitor-value");
      assertThat(udfResult.getAnalyzeResult().get("analyze-key")).isEqualTo("analyze-value");
      assertThat(udfResult
              .getPlanResult()
              .get(new KtaPolicySpec.ScaleTargetRef(
                  KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"))
              .getParallelism())
          .isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1000})
    void testInvokeMonitorPlanValid(long responseDelay) {
      setUpServer(true, responseDelay);
      KtaPolicySpec.Behavior.UserDefinedFunctions udfs =
          new KtaPolicySpec.Behavior.UserDefinedFunctions();
      udfs.setMonitorEndpoint(getServerBaseUrl() + "/monitor");
      udfs.setPlanEndpoint(getServerBaseUrl() + "/plan");
      UdfResult udfResult = this.udfInvocationHandler.invoke(
          Clock.systemUTC(),
          "id-1",
          List.of(new KtaPolicySpec.ScaleTargetRef(
              KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")),
          List.of(),
          udfs);
      assertThat(udfResult.getMonitorResult().get("monitor-key")).isEqualTo("monitor-value");
      assertThat(udfResult
              .getPlanResult()
              .get(new KtaPolicySpec.ScaleTargetRef(
                  KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"))
              .getParallelism())
          .isEqualTo(5);
    }
  }

  @Nested
  class UdfInvocationHandlerInvalidTest extends UdfInvocationHandlerTestBase {
    void setUpServer(boolean withTimeout) {
      ClientAndServer server = getServer();
      server
          .when(request().withPath(API_PREFIX + "/monitor").withMethod(HttpMethod.POST.name()))
          .respond(response()
              .withStatusCode(200)
              .withContentType(MediaType.APPLICATION_JSON)
              .withBody("{\"monitor-key\": \"monitor-value\"}"));
      if (withTimeout) {
        server
            .when(
                request().withPath(API_PREFIX + "/analyze").withMethod(HttpMethod.POST.name()),
                Times.exactly(1))
            .respond(response()
                .withDelay(TimeUnit.MILLISECONDS, 10_000)
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"analyze-key\": \"analyze-value\"}"));
      }
      server
          .when(
              request().withPath(API_PREFIX + "/plan").withMethod(HttpMethod.POST.name()),
              Times.exactly(1))
          .respond(response()
              .withStatusCode(200)
              .withContentType(MediaType.APPLICATION_JSON)
              .withBody("not-a-json"));
      server.when(request()).respond(response().withStatusCode(500));
    }

    @Test
    void testTimeoutThrowsExpectedException() {
      setUpServer(true);
      KtaPolicySpec.Behavior.UserDefinedFunctions udfs =
          new KtaPolicySpec.Behavior.UserDefinedFunctions();
      udfs.setMonitorEndpoint(getServerBaseUrl() + "/monitor");
      udfs.setAnalyzeEndpoint(getServerBaseUrl() + "/analyze");
      udfs.setPlanEndpoint(getServerBaseUrl() + "/plan");
      assertThatThrownBy(() -> this.udfInvocationHandler.invoke(
              Clock.systemUTC(),
              "id-1",
              List.of(new KtaPolicySpec.ScaleTargetRef(
                  KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")),
              List.of(),
              udfs))
          .isInstanceOf(ReconciliationFailedException.class)
          .cause()
          .hasMessageContaining("timed out");
    }

    @Test
    void testInvalidResponseThrowsExpectedException() {
      setUpServer(true);
      KtaPolicySpec.Behavior.UserDefinedFunctions udfs =
          new KtaPolicySpec.Behavior.UserDefinedFunctions();
      udfs.setMonitorEndpoint(getServerBaseUrl() + "/monitor");
      udfs.setPlanEndpoint(getServerBaseUrl() + "/plan");
      assertThatThrownBy(() -> this.udfInvocationHandler.invoke(
              Clock.systemUTC(),
              "id-1",
              List.of(new KtaPolicySpec.ScaleTargetRef(
                  KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")),
              List.of(),
              udfs))
          .isInstanceOf(ReconciliationFailedException.class)
          .cause()
          .hasMessageContaining("Invalid response from endpoint");
    }

    @Test
    void testInvokeInvalidUrlThrowsExpectedException() {
      setUpServer(false);
      KtaPolicySpec.Behavior.UserDefinedFunctions udfs =
          new KtaPolicySpec.Behavior.UserDefinedFunctions();
      udfs.setMonitorEndpoint(getServerBaseUrl() + "/monitor");
      udfs.setPlanEndpoint("http://4-d0m41n-th4t-d3fin3t3ly-n0t-3x15t5.rocks");
      assertThatThrownBy(() -> this.udfInvocationHandler.invoke(
              Clock.systemUTC(),
              "id-1",
              List.of(new KtaPolicySpec.ScaleTargetRef(
                  KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")),
              List.of(),
              udfs))
          .isInstanceOf(ReconciliationFailedException.class)
          .cause()
          .hasMessageContaining("I/O error");
    }
  }
}
