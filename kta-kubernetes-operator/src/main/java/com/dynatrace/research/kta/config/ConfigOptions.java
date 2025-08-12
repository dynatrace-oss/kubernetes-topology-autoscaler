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

import java.time.Duration;

/** The configuration options of the application. */
public final class ConfigOptions {
  private static final String KTA_PREFIX = "kta.";
  public static final String KTA_KUBERNETES_CLIENT_MAX_CONCURRENT_REQUESTS =
      KTA_PREFIX + "kubernetes-client.max-concurrent-requests";
  public static final int KTA_KUBERNETES_CLIENT_MAX_CONCURRENT_REQUESTS_DEFAULT = 32;
  public static final String HTTP_CLIENT_CONNECTION_TIMEOUT =
      KTA_PREFIX + "http-client.connect-timeout";
  public static final Duration HTTP_CLIENT_CONNECTION_TIMEOUT_DEFAULT = Duration.ofSeconds(5);
  public static final String KTA_UDF_HANDLER_REQUEST_TIMEOUT =
      KTA_PREFIX + "udf-handler.request-timeout";
  public static final Duration KTA_UDF_HANDLER_REQUEST_TIMEOUT_DEFAULT = Duration.ofSeconds(5);
  public static final String KTA_FLINK_SCALE_DRIVER_REQUEST_TIMEOUT =
      KTA_PREFIX + "flink-scale-driver.request-timeout";
  public static final Duration KTA_FLINK_SCALE_DRIVER_REQUEST_DEFAULT = Duration.ofSeconds(5);
}
