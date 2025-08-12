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

package com.dynatrace.research.kta.operator;

/** KtaPolicy Status. */
public final class KtaPolicyStatus {

  public enum State {
    New,
    Init,
    MonitorAnalyzePlan,
    Knowledge,
    Execute,
    Completed
  }

  private String id;
  private State state;
  public boolean error;
  // Results are serialized as JSON, because polymorphism of Map<String, Object> cannot be
  // handled by CRD generator
  private String udfResult;
  private String result;

  public String getId() {
    return this.id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public State getState() {
    return this.state;
  }

  public void setState(final State state) {
    this.state = state;
  }

  public boolean isError() {
    return this.error;
  }

  public void setError(final boolean error) {
    this.error = error;
  }

  public String getUdfResult() {
    return this.udfResult;
  }

  public void setUdfResult(final String udfResult) {
    this.udfResult = udfResult;
  }

  public String getResult() {
    return this.result;
  }

  public void setResult(final String result) {
    this.result = result;
  }
}
