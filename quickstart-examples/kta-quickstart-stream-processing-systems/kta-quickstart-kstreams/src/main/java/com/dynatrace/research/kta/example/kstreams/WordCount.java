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

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

public class WordCount {

  private static final String PUNCTUATION_REGEX = "[„“.:?!]";

  public static void start(String operator) throws Exception {
    Properties props = new Properties();
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, Config.BOOTSTRAP_SERVER);

    KafkaStreams streams;

    switch (operator) {
      case EntryPoint.OPERATOR_TOKENIZER: {
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "tokenizer");

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> source =
            builder.stream(Config.SOURCE_TOPIC, Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, Integer> tokenizer = source
            .flatMapValues(line -> Arrays.stream(
                    line.toLowerCase().replaceAll(PUNCTUATION_REGEX, " ").split("\\W+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList()))
            .map((key, word) -> KeyValue.pair(word, 1));
        tokenizer.to(Config.INTERMEDIATE_TOPIC, Produced.with(Serdes.String(), Serdes.Integer()));

        streams = new KafkaStreams(builder.build(), props);
        break;
      }
      case EntryPoint.OPERATOR_COUNTER: {
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "counter");
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Integer> tokenized = builder.stream(
            Config.INTERMEDIATE_TOPIC, Consumed.with(Serdes.String(), Serdes.Integer()));

        KStream<String, Integer> counter = tokenized
            .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
            .reduce(Integer::sum)
            .toStream();
        counter.to(Config.SINK_TOPIC);

        streams = new KafkaStreams(builder.build(), props);
        break;
      }
      case EntryPoint.OPERATOR_TOKENIZER_COUNTER: {
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "tokenizer-counter");
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> source =
            builder.stream(Config.SOURCE_TOPIC, Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, Integer> result = source
            .flatMapValues(line -> Arrays.stream(
                    line.toLowerCase().replaceAll(PUNCTUATION_REGEX, " ").split("\\W+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList()))
            .map((key, word) -> KeyValue.pair(word, 1))
            .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
            .reduce(Integer::sum)
            .toStream();
        result.to(Config.SINK_TOPIC);

        streams = new KafkaStreams(builder.build(), props);
        break;
      }
      default:
        throw new IllegalStateException("Unknown operator " + operator);
    }

    CountDownLatch latch = new CountDownLatch(1);

    Runtime.getRuntime().addShutdownHook(new Thread("kstreams-shutdown-hook") {
      @Override
      public void run() {
        streams.close();
        latch.countDown();
      }
    });

    try {
      streams.start();
      latch.await();
    } catch (Throwable e) {
      System.exit(1);
    }
  }
}
