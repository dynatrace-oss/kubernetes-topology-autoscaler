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

package com.dynatrace.research.kta.example.flink;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.util.Collector;

/* This file is based on source code of Apache Flink Project (https://flink.apache.org/), licensed by the Apache
 * Software Foundation (ASF) under the Apache License, Version 2.0. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. */

public class WordCount {

  /**
   * From Franz Kafka, "Der Prozess". Source: https://www.gutenberg.org/cache/epub/69327/pg69327.txt
   */
  private static final String[] TEXT = {
    "Jemand mußte Josef K. verleumdet haben, denn ohne daß er etwas Böses",
    "getan hätte, wurde er eines Morgens verhaftet. Die Köchin der Frau",
    "Grubach, seiner Zimmervermieterin, die ihm jeden Tag gegen acht Uhr",
    "früh das Frühstück brachte, kam diesmal nicht. Das war noch niemals",
    "geschehen. K. wartete noch ein Weilchen, sah von seinem Kopfkissen aus",
    "die alte Frau, die ihm gegenüber wohnte und die ihn mit einer an ihr",
    "ganz ungewöhnlichen Neugierde beobachtete, dann aber, gleichzeitig",
    "befremdet und hungrig, läutete er. Sofort klopfte es und ein Mann, den",
    "er in dieser Wohnung noch niemals gesehen hatte, trat ein. Er war",
    "schlank und doch fest gebaut, er trug ein anliegendes schwarzes Kleid,",
    "das ähnlich den Reiseanzügen mit verschiedenen Falten, Taschen,",
    "Schnallen, Knöpfen und einem Gürtel versehen war und infolgedessen,",
    "ohne daß man sich darüber klar wurde, wozu es dienen sollte, besonders",
    "praktisch erschien. „Wer sind Sie?“ fragte K. und saß gleich halb",
    "aufrecht im Bett. Der Mann aber ging über die Frage hinweg, als müsse",
    "man seine Erscheinung hinnehmen, und sagte bloß seinerseits: „Sie haben",
    "geläutet?“ „Anna soll mir das Frühstück bringen,“ sagte K. und",
    "versuchte zunächst stillschweigend durch Aufmerksamkeit und Überlegung",
    "festzustellen, wer der Mann eigentlich war. Aber dieser setzte sich",
    "nicht allzu lange seinen Blicken aus, sondern wandte sich zur Tür, die",
    "er ein wenig öffnete, um jemandem, der offenbar knapp hinter der Tür",
    "stand, zu sagen: „Er will, daß Anna ihm das Frühstück bringt.“ Ein",
    "kleines Gelächter im Nebenzimmer folgte, es war nach dem Klang nicht",
    "sicher, ob nicht mehrere Personen daran beteiligt waren. Trotzdem der",
    "fremde Mann dadurch nichts erfahren haben konnte, was er nicht schon",
    "früher gewußt hätte, sagte er nun doch zu K. im Tone einer Meldung: „Es",
    "ist unmöglich.“ „Das wäre neu,“ sagte K., sprang aus dem Bett und zog",
    "rasch seine Hosen an. „Ich will doch sehn, was für Leute im Nebenzimmer",
    "sind und wie Frau Grubach diese Störung mir gegenüber verantworten",
    "wird.“ Es fiel ihm zwar gleich ein, daß er das nicht hätte laut sagen",
    "müssen und daß er dadurch gewissermaßen ein Beaufsichtigungsrecht des",
    "Fremden anerkannte, aber es schien ihm jetzt nicht wichtig. Immerhin",
    "faßte es der Fremde so auf, denn er sagte: „Wollen Sie nicht lieber",
    "hierbleiben?“ „Ich will weder hierbleiben noch von Ihnen angesprochen",
    "werden, solange Sie sich mir nicht vorstellen.“"
  };

  private static final String PUNCTUATION_REGEX = "[„“.:?!]";
  private static final String STREAMING_GRAPH_NODE_PER_TASK_SLOT =
      "streaming-graph-node-per-task-slot";
  private static final String SHARED_TASK_SLOTS = "shared-task-slots";
  private static final String SLOT_SHARING_GROUP_SOURCE_TOKENIZER = "slot_source_tokenizer";
  private static final String SLOT_SHARING_GROUP_COUNTER = "slot_counter";

  public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    String taskSlotMode = args[args.length - 2].trim();
    if (!taskSlotMode.equals(STREAMING_GRAPH_NODE_PER_TASK_SLOT)
        && !taskSlotMode.equals(SHARED_TASK_SLOTS)) {
      throw new IllegalArgumentException(
          "Task slot mode must be either '" + STREAMING_GRAPH_NODE_PER_TASK_SLOT + "' or '"
              + SHARED_TASK_SLOTS + "'. Got: " + taskSlotMode);
    }
    int sleepMillis = Integer.parseInt(args[args.length - 1]);
    if (sleepMillis < 0) {
      throw new IllegalArgumentException("Sleep millis must not be negative. Got: " + sleepMillis);
    }

    SingleOutputStreamOperator<String> source = env.addSource(new WordCountSource(sleepMillis));
    SingleOutputStreamOperator<Tuple2<String, Integer>> tokenizer =
        source.flatMap(new Tokenizer()).uid("tokenizer");
    SingleOutputStreamOperator<Tuple2<String, Integer>> counter =
        tokenizer.keyBy(e -> e.f0).sum(1).uid("counter");

    if (taskSlotMode.equals(STREAMING_GRAPH_NODE_PER_TASK_SLOT)) {
      // Assign source and tokenizer to one slot sharing group, since operators can be
      // fused by Flink
      // Assign tokenizer to another slot
      source.slotSharingGroup(SLOT_SHARING_GROUP_SOURCE_TOKENIZER);
      counter.slotSharingGroup(SLOT_SHARING_GROUP_COUNTER);
    }

    counter.print();

    env.execute("KTA Quickstart Flink: Word Count");
  }

  public static final class WordCountSource extends RichParallelSourceFunction<String> {

    private final int sleepMillis;
    private volatile boolean isRunning = true;

    public WordCountSource(final int sleepMillis) {
      this.sleepMillis = sleepMillis;
    }

    @Override
    public void run(final SourceContext<String> ctx) throws Exception {
      while (true) {
        for (final String line : TEXT) {
          if (!this.isRunning) {
            return;
          }
          ctx.collect(line);
          Thread.sleep(this.sleepMillis);
        }
      }
    }

    @Override
    public void cancel() {
      this.isRunning = false;
    }
  }

  public static final class Tokenizer implements FlatMapFunction<String, Tuple2<String, Integer>> {

    @Override
    public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
      String[] tokens = value.toLowerCase().replaceAll(PUNCTUATION_REGEX, " ").split("\\W+");

      for (String token : tokens) {
        if (!token.isEmpty()) {
          out.collect(new Tuple2<>(token, 1));
        }
      }
    }
  }
}
