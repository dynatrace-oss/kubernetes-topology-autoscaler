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

package com.dynatrace.research.kta.common;

import com.dynatrace.research.kta.annotation.UtilityClass;
import com.dynatrace.research.kta.client.dto.PlanResultDto;
import com.dynatrace.research.kta.client.dto.ResultDto;
import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.exception.InternalOperatorErrorException;
import com.dynatrace.research.kta.exception.MapperException;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.dynatrace.research.kta.operator.persistence.PlanResult;
import com.dynatrace.research.kta.operator.persistence.Result;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maps entity objects to DTO objects and vice versa. */
@UtilityClass
public final class Mapper {

  private static final Logger LOG = LoggerFactory.getLogger(Mapper.class);
  private static final String KEY_VALUE_FORMAT = "%s=%s";
  private static final String SCALE_TARGET_REF_KIND_KEY = "kind";
  private static final String SCALE_TARGET_REF_NAME_KEY = "name";
  private static final String FLINK_STREAMING_GRAPH_NODE_ID_KEY = "id";

  // begin toDto
  public static TopologyNodeDto toDto(KtaPolicySpec.ScaleTargetRef toBeMapped) {
    return new TopologyNodeDto(
        TopologyNodeDto.Type.SCALE_TARGET_REF,
        String.format(KEY_VALUE_FORMAT, SCALE_TARGET_REF_KIND_KEY, toBeMapped.getKind()),
        String.format(KEY_VALUE_FORMAT, SCALE_TARGET_REF_NAME_KEY, toBeMapped.getName()));
  }

  public static TopologyNodeDto toDto(KtaPolicySpec.FlinkStreamingGraphNode toBeMapped) {
    return new TopologyNodeDto(
        TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE,
        String.format(KEY_VALUE_FORMAT, FLINK_STREAMING_GRAPH_NODE_ID_KEY, toBeMapped.getId()));
  }

  public static PlanResultDto toDto(PlanResult toBeMapped) {
    return new PlanResultDto(toBeMapped.getParallelism());
  }

  @SuppressWarnings("unchecked")
  public static ResultDto toDto(Result toBeMapped) {
    return ResultDto.builder()
        .withId(toBeMapped.getId())
        .withUdfStartTimestampMillis(toBeMapped.getUdfStartTimestampMillis())
        .withUdfEndTimestampMillis(toBeMapped.getUdfEndTimestampMillis())
        .withMonitorResult(toBeMapped.getMonitorResult())
        .withAnalyzeResult(toBeMapped.getAnalyzeResult())
        .withPlanResult((Map<TopologyNodeDto, PlanResultDto>) toDto(toBeMapped.getPlanResult()))
        .withParallelism((Map<TopologyNodeDto, Integer>) toDto(toBeMapped.getParallelism()))
        .build();
  }

  public static List<?> toDto(List<?> toBeMapped) {
    if (toBeMapped.isEmpty()) {
      return List.of();
    }

    Set<Object> types =
        toBeMapped.stream().map(Object::getClass).collect(Collectors.toUnmodifiableSet());

    if (types.size() != 1) {
      throw new InternalOperatorErrorException(
          "Expected list of elements to be mapped to be homogenous, but list contains types: "
              + types);
    }

    Object typeRepresentative = toBeMapped.iterator().next();

    if (typeRepresentative instanceof KtaPolicySpec.ScaleTargetRef) {
      return toBeMapped.stream()
          .map(e -> Mapper.toDto((KtaPolicySpec.ScaleTargetRef) e))
          .toList();
    } else if (typeRepresentative instanceof KtaPolicySpec.FlinkStreamingGraphNode) {
      return toBeMapped.stream()
          .map(e -> Mapper.toDto((KtaPolicySpec.FlinkStreamingGraphNode) e))
          .toList();
    } else if (typeRepresentative instanceof PlanResult) {
      return toBeMapped.stream().map(e -> Mapper.toDto((PlanResult) e)).toList();
    } else if (typeRepresentative instanceof Result) {
      return toBeMapped.stream().map(e -> Mapper.toDto((Result) e)).toList();
    } else {
      LOG.error("Cannot find a mapping for type {}", typeRepresentative.getClass());
      throw new InternalOperatorErrorException(
          "Cannot find a mapping for type " + typeRepresentative.getClass());
    }
  }

  public static <T> Map<TopologyNodeDto, ?> toDto(
      Map<? extends KtaPolicySpec.TopologyNode, T> toBeMapped) {
    Set<Object> types = toBeMapped.keySet().stream()
        .map(KtaPolicySpec.TopologyNode::getClass)
        .collect(Collectors.toUnmodifiableSet());

    if (types.size() != 1) {
      throw new InternalOperatorErrorException(
          "Expected list of elements to be mapped to be homogenous, but list contains types: "
              + types);
    }

    final Object typeRepresentative = toBeMapped.keySet().iterator().next();

    if (typeRepresentative instanceof KtaPolicySpec.ScaleTargetRef) {
      return Collections.unmodifiableMap(toBeMapped.entrySet().stream()
          .collect(
              HashMap::new,
              (m, e) -> {
                Object value = e.getValue();
                if (value instanceof PlanResult) {
                  m.put(
                      Mapper.toDto((KtaPolicySpec.ScaleTargetRef) e.getKey()),
                      Mapper.toDto((PlanResult) value));
                } else if (value instanceof Integer) {
                  m.put(Mapper.toDto((KtaPolicySpec.ScaleTargetRef) e.getKey()), value);
                } else {
                  LOG.error(
                      "Invalid type {}. Expected one of: {}, {}",
                      value.getClass(),
                      PlanResult.class,
                      Integer.class);
                  throw new InternalOperatorErrorException(
                      "Invalid value type " + value.getClass());
                }
              },
              HashMap::putAll));
    } else if (typeRepresentative instanceof KtaPolicySpec.FlinkStreamingGraphNode) {
      return Collections.unmodifiableMap(toBeMapped.entrySet().stream()
          .collect(
              HashMap::new,
              (m, e) -> {
                Object value = e.getValue();
                if (value instanceof PlanResult) {
                  m.put(
                      Mapper.toDto((KtaPolicySpec.FlinkStreamingGraphNode) e.getKey()),
                      Mapper.toDto((PlanResult) value));
                } else if (value instanceof Integer) {
                  m.put(Mapper.toDto((KtaPolicySpec.FlinkStreamingGraphNode) e.getKey()), value);
                } else {
                  LOG.error(
                      "Invalid type {}. Expected one of: {}, {}",
                      value.getClass(),
                      PlanResult.class,
                      Integer.class);
                  throw new InternalOperatorErrorException(
                      "Invalid type value type: " + value.getClass());
                }
              },
              HashMap::putAll));
    } else {
      LOG.error("Cannot find a mapping for type {}", typeRepresentative.getClass());
      throw new InternalOperatorErrorException(
          "Cannot find a mapping for type " + typeRepresentative.getClass());
    }
  }
  // end

  // begin fromDto
  public static KtaPolicySpec.TopologyNode fromDTO(TopologyNodeDto toBeMapped) {
    Map<String, String> fieldComponents =
        TopologyNodeDto.extractFieldComponents(toBeMapped.getFields());
    return switch (toBeMapped.getType()) {
      case SCALE_TARGET_REF -> {
        KtaPolicySpec.ScaleTargetRef scaleTargetRef = new KtaPolicySpec.ScaleTargetRef();
        scaleTargetRef.setKind(KtaPolicySpec.ScaleTargetRef.Kind.valueOf(
            fieldComponents.get(SCALE_TARGET_REF_KIND_KEY)));
        scaleTargetRef.setName(fieldComponents.get(SCALE_TARGET_REF_NAME_KEY));
        yield scaleTargetRef;
      }
      case FLINK_STREAMING_GRAPH_NODE -> {
        KtaPolicySpec.FlinkStreamingGraphNode flinkStreamingGraphNode =
            new KtaPolicySpec.FlinkStreamingGraphNode();
        flinkStreamingGraphNode.setId(fieldComponents.get("id"));
        yield flinkStreamingGraphNode;
      }
    };
  }

  public static PlanResult fromDTO(PlanResultDto toBeMapped) {
    return new PlanResult(toBeMapped.getParallelism());
  }

  public static <T> Map<KtaPolicySpec.TopologyNode, ?> fromDTO(Map<TopologyNodeDto, T> toBeMapped) {
    Set<Object> types = toBeMapped.keySet().stream()
        .map(TopologyNodeDto::getType)
        .collect(Collectors.toUnmodifiableSet());

    if (types.size() != 1) {
      throw new InternalOperatorErrorException(
          "Expected list of elements to be mapped to be homogenous, but list contains types: "
              + types);
    }

    return Collections.unmodifiableMap(toBeMapped.entrySet().stream()
        .collect(
            HashMap::new,
            (m, e) -> {
              Object value = e.getValue();
              if (value instanceof PlanResultDto) {
                m.put(Mapper.fromDTO(e.getKey()), Mapper.fromDTO((PlanResultDto) e.getValue()));
              } else if (value instanceof Integer) {
                m.put(Mapper.fromDTO(e.getKey()), value);
              } else {
                LOG.error(
                    "Invalid type {}. Expected one of: {}, {}",
                    value.getClass(),
                    PlanResultDto.class,
                    Integer.class);
                throw new InternalOperatorErrorException("Invalid value type: " + value.getClass());
              }
            },
            HashMap::putAll));
  }

  @SuppressWarnings("unchecked")
  public static Union<PlanResult, Map<KtaPolicySpec.TopologyNode, PlanResult>> fromDTO(
      Union<PlanResultDto, Map<TopologyNodeDto, PlanResultDto>> toBeMapped) {
    if (toBeMapped.isFirst()) {
      return Union.of(Mapper.fromDTO(toBeMapped.first()), null);
    } else {
      return Union.of(
          null, (Map<KtaPolicySpec.TopologyNode, PlanResult>) fromDTO(toBeMapped.second()));
    }
  }
  // end

  // begin other mappers
  public static Map<KtaPolicySpec.TopologyNode, PlanResult> toCanonicalFormatOrElseThrow(
      Union<PlanResult, Map<KtaPolicySpec.TopologyNode, PlanResult>> union,
      List<? extends KtaPolicySpec.TopologyNode> topologyNodes) {
    if (union.isFirst()) {
      if (topologyNodes.size() != 1) {
        throw new MapperException(
            "Got single plan result object, but there are " + topologyNodes.size()
                + " topology nodes. A single plan result object can only be mapped if there is 1 topology node.");
      }
      return Map.of(topologyNodes.get(0), union.first());
    } else {
      Set<KtaPolicySpec.TopologyNode> actualNodes = union.second().keySet();
      Set<KtaPolicySpec.TopologyNode> expectedNodes = new HashSet<>(topologyNodes);
      if (!actualNodes.equals(expectedNodes)) {
        throw new MapperException("Expected nodes " + expectedNodes.stream().sorted() + " but got "
            + actualNodes.stream().sorted() + ".");
      }
      return union.second();
    }
  }
  // end
}
