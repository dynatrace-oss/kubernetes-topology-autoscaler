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

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class Producer {

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

  public static void start(int sleepMillis) throws Exception {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Config.BOOTSTRAP_SERVER);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
      while (true) {
        for (final String line : TEXT) {
          ProducerRecord<String, String> record = new ProducerRecord<>(Config.SOURCE_TOPIC, line);
          producer.send(record);
          Thread.sleep(sleepMillis);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
