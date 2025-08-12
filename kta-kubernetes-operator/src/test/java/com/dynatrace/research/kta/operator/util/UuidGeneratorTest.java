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

package com.dynatrace.research.kta.operator.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for {@link UuidGenerator}. */
class UuidGeneratorTest {

  private static IdGenerator idGenerator;

  @BeforeAll
  static void beforeAll() {
    idGenerator = new UuidGenerator();
  }

  @Test
  void testGeneratesValidIds() {
    for (int i = 0; i < 10; i++) {
      var id = idGenerator.generate();
      Assertions.assertNotNull(id, "Expected id to be not null at iteration " + i + ".");
      Assertions.assertFalse(id.isEmpty(), "Expected id to be not empty at iteration " + i + ".");
      Assertions.assertFalse(id.isBlank(), "Expected id to be not blank at iteration " + i + ".");
    }
  }
}
