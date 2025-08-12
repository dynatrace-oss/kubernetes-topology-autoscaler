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

package com.dynatrace.research.kta.util;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

/** Utility methods for tests. */
public class TestUtils {

  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  public static void assertThatNoConstraintIsViolated(Object obj) {
    Set<ConstraintViolation<Object>> constraintViolations = validator.validate(obj);
    assertThat(constraintViolations.size()).isEqualTo(0);
  }

  public static void assertThatAtLeastOneConstraintIsViolated(Object obj) {
    Set<ConstraintViolation<Object>> constraintViolations = validator.validate(obj);
    assertThat(constraintViolations.size()).isGreaterThan(0);
  }
}
