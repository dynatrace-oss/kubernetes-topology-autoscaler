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

package com.dynatrace.research.kta;

import com.dynatrace.research.kta.common.Condition;
import com.dynatrace.research.kta.config.DependencyFactory;
import com.dynatrace.research.kta.config.JsonSerde;
import com.dynatrace.research.kta.operator.util.ReconcilerUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;

/** Base class for all test cases that require dependencies from {@link DependencyFactory}. */
public class TestBase {
  protected static DependencyFactory dependencyFactory =
      new DependencyFactory(ConfigProvider.getConfig());

  @BeforeAll
  static void beforeAll() {
    // Initialize utility classes
    ReconcilerUtils.objectMapper = dependencyFactory.getObjectMapper();
    JsonSerde.objectMapper = dependencyFactory.getObjectMapper();
    Condition.validator = dependencyFactory.getValidator();
  }
}
