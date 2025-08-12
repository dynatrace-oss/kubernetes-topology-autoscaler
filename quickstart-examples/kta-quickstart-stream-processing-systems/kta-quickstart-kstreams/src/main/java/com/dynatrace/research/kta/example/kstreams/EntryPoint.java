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

package com.dynatrace.research.kta.example.kstreams;

public class EntryPoint {

  private static final String OPERATOR_SOURCE = "producer";
  public static final String OPERATOR_TOKENIZER = "tokenizer";
  public static final String OPERATOR_COUNTER = "counter";
  public static final String OPERATOR_TOKENIZER_COUNTER = "tokenizer-counter";

  public static void main(String[] args) throws Exception {
    String operator = args[0];
    if (operator.equals(OPERATOR_SOURCE)) {
      if (args.length != 2) {
        throw new IllegalArgumentException(
            "Expected second argument (sleepTimeMillis) for operator " + OPERATOR_SOURCE);
      }
      int sleepMillis = Integer.parseInt(args[args.length - 1]);
      if (sleepMillis < 0) {
        throw new IllegalArgumentException(
            "Sleep millis must not be negative. Got: " + sleepMillis);
      }
      Producer.start(sleepMillis);
    } else if (operator.equals(OPERATOR_TOKENIZER)
        || operator.equals(OPERATOR_COUNTER)
        || operator.equals(OPERATOR_TOKENIZER_COUNTER)) {
      WordCount.start(operator);
    } else {
      throw new IllegalArgumentException("Unknown operator argument " + operator);
    }
  }
}
