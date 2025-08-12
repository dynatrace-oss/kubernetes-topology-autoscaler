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

package com.dynatrace.research.kta.common;

import com.dynatrace.research.kta.annotation.UtilityClass;
import com.dynatrace.research.kta.exception.ConditionViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Objects;
import java.util.Set;

/** Methods to check pre-/post-conditions and invariants. */
@UtilityClass
public final class Condition {

  public static Validator validator;

  public static <T> T notNull(final T maybeNull) {
    if (Objects.isNull(maybeNull)) {
      throw new ConditionViolationException("Expected argument to be not null.");
    }
    return maybeNull;
  }

  public static <T> void validConstraints(final T obj) {
    Set<ConstraintViolation<T>> violations = validator.validate(obj);
    if (!violations.isEmpty()) {
      throw new ConditionViolationException(
          "Found the following constraint violations: " + violations);
    }
  }
}
