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

package com.dynatrace.research.kta.operator.util;

import com.dynatrace.research.kta.annotation.UtilityClass;
import com.dynatrace.research.kta.exception.InternalOperatorErrorException;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.dynatrace.research.kta.operator.KtaReconciler;
import com.dynatrace.research.kta.operator.persistence.Result;
import com.dynatrace.research.kta.operator.udf.UdfResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Utility methods for {@link KtaReconciler}. */
@UtilityClass
public final class ReconcilerUtils {

  public static ObjectMapper objectMapper;

  public static List<? extends KtaPolicySpec.TopologyNode> getTopology(
      KtaPolicySpec.ScaleDriver scaleDriver) {
    return switch (scaleDriver.getType()) {
      case GenericKubernetes -> scaleDriver.getGenericKubernetesTopology();
      case Flink -> scaleDriver.getFlinkTopology();
    };
  }

  public static String serializeResult(Result result) {
    try {
      return objectMapper.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      throw new InternalOperatorErrorException("Error serializing result " + result, e);
    }
  }

  public static String serializeUdfResult(UdfResult udfResult) {
    try {
      return objectMapper.writeValueAsString(udfResult);
    } catch (JsonProcessingException e) {
      throw new InternalOperatorErrorException("Error serializing UDF result " + udfResult, e);
    }
  }

  public static UdfResult deserializeUdfResult(String serializedUdfResult) {
    try {
      return objectMapper.readValue(serializedUdfResult, UdfResult.class);
    } catch (JsonProcessingException e) {
      throw new InternalOperatorErrorException(
          "Error deserializing UDF result " + serializedUdfResult, e);
    }
  }
}
