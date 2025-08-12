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
import java.util.List;

/**
 * Stores and provides results of previous MAPE-K loop iterations. Results are stored on a per-key
 * basis, as one Operator might evaluate multiple CRDs with different names of the same type.
 * Implementations may be not fault-tolerant or persistent (e.g., in memory only).
 */
public interface KnowledgeStore<T> {

  /**
   * Adds a single entry to the result store with the given key.
   *
   * @param name Key
   * @param result The result. Must not be null.
   */
  @ThreadSafe
  void add(String name, T result);

  /**
   * Get all results of a given key. The latest result is at index 0.
   *
   * @param key Key
   * @return The results
   */
  @ThreadSafe
  List<T> get(String key);

  /**
   * Get the last n results for a given key. The latest result is at index 0.
   *
   * @param key Key
   * @param limit Limit
   * @return The results
   */
  @ThreadSafe
  List<T> get(String key, int limit);

  /**
   * Gets the latest result for a given key.
   *
   * @param key The key
   * @return The result
   */
  @ThreadSafe
  T getLatest(String key);

  /**
   * Removes all results for a given key.
   *
   * @param key The key
   */
  @ThreadSafe
  void remove(String key);
}
