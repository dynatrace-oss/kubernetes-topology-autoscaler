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

package com.dynatrace.research.kta.operator.scaling;

import com.dynatrace.research.kta.operator.KtaPolicySpec;
import java.util.Map;

/**
 * Scales the stream processing application to the desired parallelism. There is no restriction in
 * the way the scaling is performed. In particular, applications might be scaled in a
 * Kubernetes-native way (resources that implement the Scale Subresource) or by other means, e.g.,
 * by calling an API that takes care of scaling actions of the used stream processing system.
 */
public interface ScaleDriver {
  void scale(
      KtaPolicySpec.ScaleDriver scaleDriverSpec,
      Map<KtaPolicySpec.TopologyNode, Integer> parallelism);
}
