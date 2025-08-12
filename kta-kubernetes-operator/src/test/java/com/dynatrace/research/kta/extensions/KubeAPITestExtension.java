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

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

public class KubeAPITestExtension implements TestInstancePostProcessor {

  private static final String OPERATOR_TEST_NAMESPACE_PREFIX = "kubeapitest-";

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    if (!(testInstance instanceof final KubeAPITestBase kubeAPITest)) {
      throw new RuntimeException("Test instance must extend " + KubeAPITestBase.class.getName());
    }

    if (kubeAPITest.getKubeConfig() == null) {
      throw new RuntimeException("Configuration was not injected.");
    }

    String namespaceName = OPERATOR_TEST_NAMESPACE_PREFIX + UUID.randomUUID();
    kubeAPITest.setNamespace(namespaceName);

    KubernetesClient kubernetesClient = new KubernetesClientBuilder()
        .withConfig(Config.fromKubeconfig(kubeAPITest.getKubeConfig()))
        .editOrNewConfig()
        .withNamespace(kubeAPITest.getNamespace())
        .endConfig()
        .build();

    Resource<Namespace> resource = kubernetesClient
        .namespaces()
        .resource(new NamespaceBuilder()
            .withNewMetadata()
            .withName(namespaceName)
            .endMetadata()
            .build());
    resource.create();

    kubeAPITest.setKubernetesClient(kubernetesClient);
  }
}
