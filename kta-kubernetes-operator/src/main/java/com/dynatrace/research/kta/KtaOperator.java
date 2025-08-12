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
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

/** Application entry point. */
@QuarkusMain
public final class KtaOperator implements QuarkusApplication {
  private final Operator operator;
  private final DependencyFactory dependencyFactory;

  @Inject
  public KtaOperator(final Operator operator, final DependencyFactory dependencyFactory) {
    this.operator = operator;
    this.dependencyFactory = dependencyFactory;
  }

  public static void main(final String... args) {
    Quarkus.run(KtaOperator.class, args);
  }

  @Override
  public int run(final String... args) {
    // Initialize utility classes
    ReconcilerUtils.objectMapper = this.dependencyFactory.getObjectMapper();
    JsonSerde.objectMapper = this.dependencyFactory.getObjectMapper();
    Condition.validator = this.dependencyFactory.getValidator();

    this.operator.start();
    Quarkus.waitForExit();

    return 0;
  }
}
