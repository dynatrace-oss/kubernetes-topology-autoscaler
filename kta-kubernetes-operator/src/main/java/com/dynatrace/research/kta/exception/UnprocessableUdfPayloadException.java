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

package com.dynatrace.research.kta.exception;

import com.dynatrace.research.kta.client.UdfClient;

/**
 * Exception thrown by instances of {@link UdfClient}. Thrown when the UDF response contains a
 * <i>successful</i> status code but the payload could not be processed due to syntactical or
 * semantical error(s).
 */
public final class UnprocessableUdfPayloadException extends KTABaseException {

  public UnprocessableUdfPayloadException() {}

  public UnprocessableUdfPayloadException(final String message) {
    super(message);
  }

  public UnprocessableUdfPayloadException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
