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

import com.dynatrace.research.kta.annotation.ThreadSafe;
import java.util.*;

/**
 * In-memory implementation of {@link KnowledgeStore}. This implementation is neither fault-tolerant
 * nor persistent.
 */
public final class InMemoryKnowledgeStore<T> implements KnowledgeStore<T> {
  private final Map<String, LinkedList<T>> crdToResult = new HashMap<>();

  @Override
  @ThreadSafe
  public synchronized void add(String name, T result) {
    if (result == null) {
      throw new IllegalArgumentException("Result must not be null");
    }
    this.crdToResult.computeIfAbsent(name, __ -> new LinkedList<>()).addFirst(result);
  }

  @Override
  @ThreadSafe
  public synchronized List<T> get(String key) {
    return Collections.unmodifiableList(this.crdToResult.getOrDefault(key, new LinkedList<>()));
  }

  @Override
  @ThreadSafe
  public synchronized List<T> get(String key, int limit) {
    return get(key).stream().limit(limit).toList();
  }

  @Override
  @ThreadSafe
  public synchronized T getLatest(final String key) {
    return get(key).get(0);
  }

  @Override
  @ThreadSafe
  public synchronized void remove(final String key) {
    this.crdToResult.remove(key);
  }
}
