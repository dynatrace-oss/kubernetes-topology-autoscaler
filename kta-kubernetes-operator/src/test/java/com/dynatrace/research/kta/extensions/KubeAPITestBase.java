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

package com.dynatrace.research.kta.extensions;

import com.dynatrace.research.kta.TestBase;
import io.fabric8.kubernetes.client.KubernetesClient;

/** Base class for test classes using {@link KubeAPITestExtension}. */
public abstract class KubeAPITestBase extends TestBase {

  public abstract String getKubeConfig();

  private String namespace;

  private KubernetesClient kubernetesClient;

  public String getNamespace() {
    return this.namespace;
  }

  public void setNamespace(final String namespace) {
    this.namespace = namespace;
  }

  public KubernetesClient getKubernetesClient() {
    return this.kubernetesClient;
  }

  public void setKubernetesClient(final KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }
}
