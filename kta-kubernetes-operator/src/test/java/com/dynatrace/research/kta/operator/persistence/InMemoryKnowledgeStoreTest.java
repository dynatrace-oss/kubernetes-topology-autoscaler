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

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test for {@link InMemoryKnowledgeStore}. */
public class InMemoryKnowledgeStoreTest {

  private KnowledgeStore<Integer> knowledgeStore;

  @BeforeEach
  void beforeEach() {
    this.knowledgeStore = new InMemoryKnowledgeStore<>();
  }

  @Test
  void testEmtpyResultStore() {
    List<Integer> results = this.knowledgeStore.get("crd1");
    assertThat(results).isEmpty();
  }

  @Test
  void testAddAndRetrieveSingleResult() {
    this.knowledgeStore.add("crd1", 1);

    List<Integer> results = this.knowledgeStore.get("crd1");
    assertThat(results).containsExactly(1);
  }

  @Test
  void testAddAndRetrieveMultipleResults() {
    this.knowledgeStore.add("crd1", 1);
    this.knowledgeStore.add("crd1", 2);
    this.knowledgeStore.add("crd1", 3);

    List<Integer> results = this.knowledgeStore.get("crd1");
    assertThat(results).containsExactly(3, 2, 1);
  }

  @Test
  void testGetLatest() {
    this.knowledgeStore.add("crd1", 1);
    this.knowledgeStore.add("crd1", 2);
    this.knowledgeStore.add("crd1", 3);

    int results = this.knowledgeStore.getLatest("crd1");
    assertThat(results).isEqualTo(3);
  }

  @Test
  void testLimitWithResults() {
    this.knowledgeStore.add("crd1", 10);
    this.knowledgeStore.add("crd1", 20);
    this.knowledgeStore.add("crd1", 30);

    List<Integer> limitedResults = this.knowledgeStore.get("crd1", 2);
    assertThat(limitedResults).containsExactly(30, 20);
  }

  @Test
  void testLimitWithoutResults() {
    List<Integer> limitedResults = this.knowledgeStore.get("crd1", 10);
    assertThat(limitedResults).isEmpty();
  }

  @Test
  void testIllegalResultValue() {
    assertThatThrownBy(() -> this.knowledgeStore.add("cfd", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testResultListCannotBeModified() {
    this.knowledgeStore.add("crd1", 1);

    List<Integer> results = this.knowledgeStore.get("crd1");
    assertThatThrownBy(() -> results.add(2)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testMultipleKeys() {
    this.knowledgeStore.add("crd1", 1);
    this.knowledgeStore.add("crd2", 2);
    this.knowledgeStore.add("crd1", 3);
    this.knowledgeStore.add("crd3", 4);

    List<Integer> resultsA = this.knowledgeStore.get("crd1");
    List<Integer> resultsB = this.knowledgeStore.get("crd2");
    List<Integer> resultsC = this.knowledgeStore.get("crd3");

    assertThat(resultsA).containsExactly(3, 1);
    assertThat(resultsB).containsExactly(2);
    assertThat(resultsC).containsExactly(4);
  }

  @Test
  void testRemove() {
    this.knowledgeStore.add("crd1", 1);
    this.knowledgeStore.add("crd2", 2);
    this.knowledgeStore.add("crd3", 3);
    this.knowledgeStore.add("crd3", 4);

    this.knowledgeStore.remove("crd1");

    assertThat(this.knowledgeStore.get("crd1").size()).isEqualTo(0);
    assertThat(this.knowledgeStore.get("crd2").size()).isEqualTo(1);
    assertThat(this.knowledgeStore.get("crd3").size()).isEqualTo(2);

    this.knowledgeStore.remove("crd3");

    assertThat(this.knowledgeStore.get("crd1").size()).isEqualTo(0);
    assertThat(this.knowledgeStore.get("crd2").size()).isEqualTo(1);
    assertThat(this.knowledgeStore.get("crd3").size()).isEqualTo(0);

    this.knowledgeStore.remove("crd2");

    assertThat(this.knowledgeStore.get("crd1").size()).isEqualTo(0);
    assertThat(this.knowledgeStore.get("crd2").size()).isEqualTo(0);
    assertThat(this.knowledgeStore.get("crd3").size()).isEqualTo(0);
  }
}
