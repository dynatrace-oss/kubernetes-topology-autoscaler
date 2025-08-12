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

public final class Config {
  static final String BOOTSTRAP_SERVER = "kta-kafka-cluster-kafka-bootstrap:9092";
  static final String SOURCE_TOPIC = "kta-quickstart-source-topic";
  static final String INTERMEDIATE_TOPIC = "kta-quickstart-intermediate-topic";
  static final String SINK_TOPIC = "kta-quickstart-sink-topic";
}
