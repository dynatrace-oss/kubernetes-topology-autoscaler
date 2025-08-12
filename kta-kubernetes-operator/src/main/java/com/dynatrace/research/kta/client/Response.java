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

import com.dynatrace.research.kta.exception.UdfInvocationNotSuccessfulException;

/** Protocol-agnostic abstract base class for UDF responses. */
public abstract class Response<T> {

  private final int statusCode;
  private final T payload;

  public Response(final int statusCode, final T payload) {
    this.statusCode = statusCode;
    this.payload = payload;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public T getPayload() {
    return this.payload;
  }

  /**
   * Checks if the response contains a successful status code.
   *
   * @throws UdfInvocationNotSuccessfulException If the response did not contain a successful status
   *     code.
   */
  public abstract void raiseForStatus();
}
