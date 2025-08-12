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

package com.dynatrace.research.kta.operator.persistence;

/** Result of the Plan step of a MAPE-K loop iteration. */
public final class PlanResult {
  public PlanResult() {}

  public PlanResult(final int parallelism) {
    this.parallelism = parallelism;
  }

  private int parallelism;

  public int getParallelism() {
    return this.parallelism;
  }

  public void setParallelism(final int parallelism) {
    this.parallelism = parallelism;
  }
}
