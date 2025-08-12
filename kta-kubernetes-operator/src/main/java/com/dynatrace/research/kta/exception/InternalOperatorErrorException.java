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

/** Thrown when an invariant is violated, e.g., unexpected behavior of internal APIs. */
public final class InternalOperatorErrorException extends KTABaseException {

  private static final String MAINTAINER_INFORMATION =
      "\n\nPLEASE CONSIDER REPORTING THIS ISSUE TO THE PROJECT MAINTAINERS BY OPENING AN ISSUE (https://github.com/dynatrace-oss/kubernetes-topology-autoscaler/issues)!";

  public InternalOperatorErrorException() {}

  public InternalOperatorErrorException(final String message) {
    super(message + MAINTAINER_INFORMATION);
  }

  public InternalOperatorErrorException(final String message, final Throwable cause) {
    super(message + MAINTAINER_INFORMATION, cause);
  }
}
