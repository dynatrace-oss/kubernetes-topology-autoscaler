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

/**
 * Union type. Exactly one element must be non-null.
 *
 * @param first First element
 * @param second Second element
 * @param <T> Type of first element
 * @param <U> Type of second element.
 */
public record Union<T, U>(T first, U second) {

  public Union {
    if ((first == null) == (second == null)) {
      throw new IllegalArgumentException(
          "Exactly one of first and second must be given. Got first: " + first + " Got second: "
              + second);
    }
  }

  public boolean isFirst() {
    return this.first != null;
  }

  public boolean isSecond() {
    return this.second != null;
  }

  public static <T, U> Union<T, U> of(T first, U second) {
    return new Union<>(first, second);
  }
}
