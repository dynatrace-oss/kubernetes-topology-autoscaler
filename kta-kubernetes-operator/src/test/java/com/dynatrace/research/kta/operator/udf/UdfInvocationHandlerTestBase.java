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

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.dynatrace.research.kta.TestBase;
import org.junit.jupiter.api.*;
import org.mockserver.integration.ClientAndServer;

/** Base test class for tests of {@link UdfInvocationHandler}. */
public abstract class UdfInvocationHandlerTestBase extends TestBase {

  public static final String SERVER_ADDRESS = "http://127.0.0.1:";
  public static final String API_PREFIX = "/api/test";

  private static ClientAndServer server;
  private static String serverBaseUrl;

  static UdfInvocationHandler udfInvocationHandler;

  @BeforeAll
  static void beforeAll() {
    udfInvocationHandler = new UdfInvocationHandler(
        dependencyFactory.getUdfClient(), dependencyFactory.getPlanPayloadDeserializer());

    server = startClientAndServer();
    serverBaseUrl = SERVER_ADDRESS + server.getLocalPort() + API_PREFIX;
  }

  @AfterAll
  static void afterAll() {
    server.close();
  }

  public static ClientAndServer getServer() {
    return server;
  }

  public static String getServerBaseUrl() {
    return serverBaseUrl;
  }
}
