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

import com.dynatrace.research.kta.common.Mapper;

/** Exception thrown by {@link Mapper}. Thrown when an error during the mapping process occurs. */
public final class MapperException extends KTABaseException {

  public MapperException() {}

  public MapperException(final String message) {
    super(message);
  }

  public MapperException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
