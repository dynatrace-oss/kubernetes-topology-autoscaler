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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dynatrace.research.kta.client.dto.PlanResultDto;
import com.dynatrace.research.kta.client.dto.ResultDto;
import com.dynatrace.research.kta.client.dto.TopologyNodeDto;
import com.dynatrace.research.kta.exception.InternalOperatorErrorException;
import com.dynatrace.research.kta.exception.MapperException;
import com.dynatrace.research.kta.operator.KtaPolicySpec;
import com.dynatrace.research.kta.operator.persistence.PlanResult;
import com.dynatrace.research.kta.operator.persistence.Result;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for {@link Mapper}. */
public class MapperTest {

  @Test
  void testToDtoTopologyNode() {
    TopologyNodeDto topologyNodeDTO;
    topologyNodeDTO = Mapper.toDto(new KtaPolicySpec.ScaleTargetRef(
        KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"));
    assertThat(topologyNodeDTO.getType()).isEqualTo(TopologyNodeDto.Type.SCALE_TARGET_REF);
    assertThat(topologyNodeDTO.getFields()).isEqualTo("kind=Deployment|name=scale-target-ref-1");
    topologyNodeDTO = Mapper.toDto(new KtaPolicySpec.FlinkStreamingGraphNode("id-1"));
    assertThat(topologyNodeDTO.getType())
        .isEqualTo(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE);
    assertThat(topologyNodeDTO.getFields()).isEqualTo("id=id-1");
  }

  @Test
  void testToDtoPlanResult() {
    PlanResult planResult = new PlanResult(5);
    PlanResultDto planResultDTO = Mapper.toDto(planResult);
    assertThat(planResultDTO.getParallelism()).isEqualTo(5);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testToDtoList() {
    assertThat(Mapper.toDto(List.of()).size()).isEqualTo(0);

    // valid
    List<?> validList = List.of(
        new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"),
        new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-2"));
    List<TopologyNodeDto> dtos = (List<TopologyNodeDto>) Mapper.toDto(validList);
    assertThat(dtos.size()).isEqualTo(2);
    assertThat(dtos.get(0).getType()).isEqualTo(TopologyNodeDto.Type.SCALE_TARGET_REF);
    assertThat(dtos.get(0).getFields()).isEqualTo("kind=Deployment|name=scale-target-ref-1");
    assertThat(dtos.get(1).getType()).isEqualTo(TopologyNodeDto.Type.SCALE_TARGET_REF);
    assertThat(dtos.get(1).getFields()).isEqualTo("kind=Deployment|name=scale-target-ref-2");

    // invalid
    List<?> invalidList = List.of(
        new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"),
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"));
    assertThatThrownBy(() -> Mapper.toDto(invalidList))
        .isInstanceOf(InternalOperatorErrorException.class);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testToDtoMapTopologyNode() {
    Map<KtaPolicySpec.TopologyNode, PlanResult> toBeMapped;
    Map<TopologyNodeDto, PlanResultDto> mapped;

    toBeMapped = Map.of(
        new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"),
        new PlanResult(5),
        new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-2"),
        new PlanResult(10));
    mapped = (Map<TopologyNodeDto, PlanResultDto>) Mapper.toDto(toBeMapped);
    assertThat(mapped.size()).isEqualTo(2);
    assertThat(mapped
            .get(new TopologyNodeDto(
                TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref-1"))
            .getParallelism())
        .isEqualTo(5);
    assertThat(mapped
            .get(new TopologyNodeDto(
                TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref-2"))
            .getParallelism())
        .isEqualTo(10);
    toBeMapped = Map.of(
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
        new PlanResult(10),
        new KtaPolicySpec.FlinkStreamingGraphNode("id-2"),
        new PlanResult(20));
    mapped = (Map<TopologyNodeDto, PlanResultDto>) Mapper.toDto(toBeMapped);
    assertThat(mapped.size()).isEqualTo(2);
    assertThat(mapped
            .get(new TopologyNodeDto(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE, "id=id-1"))
            .getParallelism())
        .isEqualTo(10);
    assertThat(mapped
            .get(new TopologyNodeDto(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE, "id=id-2"))
            .getParallelism())
        .isEqualTo(20);
  }

  @Test
  void testToDtoResult() {
    Result result = new Result(
        "id-1",
        1000,
        2000,
        Map.of("monitor-key", "monitor-value"),
        Map.of("analyze-key", "analyze-value"),
        Map.of(
            new KtaPolicySpec.ScaleTargetRef(
                KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"),
            new PlanResult(10)),
        Map.of(
            new KtaPolicySpec.ScaleTargetRef(
                KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"),
            5));

    ResultDto resultDTO = Mapper.toDto(result);
    assertThat(resultDTO.getId()).isEqualTo("id-1");
    assertThat(resultDTO.getUdfStartTimestampMillis()).isEqualTo(1000);
    assertThat(resultDTO.getUdfEndTimestampMillis()).isEqualTo(2000);
    assertThat(resultDTO.getAnalyzeResult().size()).isEqualTo(1);
    assertThat(resultDTO.getMonitorResult().get("monitor-key")).isEqualTo("monitor-value");
    assertThat(resultDTO.getAnalyzeResult().get("analyze-key")).isEqualTo("analyze-value");
    assertThat(resultDTO.getPlanResult().size()).isEqualTo(1);
    assertThat(resultDTO
            .getPlanResult()
            .get(new TopologyNodeDto(
                TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref-1"))
            .getParallelism())
        .isEqualTo(10);
    assertThat(resultDTO
            .getParallelism()
            .get(new TopologyNodeDto(
                TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref-1")))
        .isEqualTo(5);
  }

  @Test
  void testFromDtoTopologyNode() {
    TopologyNodeDto topologyNodeDTO;
    KtaPolicySpec.TopologyNode topologyNode;

    topologyNodeDTO = new TopologyNodeDto(
        TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref-1");
    topologyNode = Mapper.fromDTO(topologyNodeDTO);
    assertThat(topologyNode).isInstanceOf(KtaPolicySpec.ScaleTargetRef.class);
    assertThat(((KtaPolicySpec.ScaleTargetRef) topologyNode).getKind())
        .isEqualTo(KtaPolicySpec.ScaleTargetRef.Kind.Deployment);
    assertThat(((KtaPolicySpec.ScaleTargetRef) topologyNode).getName())
        .isEqualTo("scale-target-ref-1");
    topologyNodeDTO =
        new TopologyNodeDto(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE, "id=id-1");
    topologyNode = Mapper.fromDTO(topologyNodeDTO);
    assertThat(topologyNode).isInstanceOf(KtaPolicySpec.FlinkStreamingGraphNode.class);
    assertThat(((KtaPolicySpec.FlinkStreamingGraphNode) topologyNode).getId()).isEqualTo("id-1");
  }

  @Test
  void testFromDtoPlanResult() {
    PlanResultDto planResultDTO = new PlanResultDto(10);
    PlanResult planResult = Mapper.fromDTO(planResultDTO);
    assertThat(planResult.getParallelism()).isEqualTo(10);
  }

  @Test
  void testFromDtoMapTopologyNodeValid() {
    Map<TopologyNodeDto, ?> map;
    Map<KtaPolicySpec.TopologyNode, ?> res;
    map = Map.of(
        new TopologyNodeDto(
            TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref-1"),
        new PlanResultDto(5));
    res = Mapper.fromDTO(map);
    assertThat(res.size()).isEqualTo(1);
    assertThat(res.containsKey(new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")))
        .isTrue();
    assertThat(((PlanResult) res.get(new KtaPolicySpec.ScaleTargetRef(
                KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")))
            .getParallelism())
        .isEqualTo(5);
    map = Map.of(
        new TopologyNodeDto(
            TopologyNodeDto.Type.SCALE_TARGET_REF, "kind=Deployment|name=scale-target-ref-1"),
        2);
    res = Mapper.fromDTO(map);
    assertThat(res.size()).isEqualTo(1);
    assertThat(res.containsKey(new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")))
        .isTrue();
    assertThat(((int) res.get(new KtaPolicySpec.ScaleTargetRef(
            KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"))))
        .isEqualTo(2);
  }

  @Test
  void testFromDtoMapTopologyNodeInvalid() {
    Map<TopologyNodeDto, String> invalidMap = Map.of(
        new TopologyNodeDto(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE, "id=id-1"), "5");
    assertThatThrownBy(() -> Mapper.fromDTO(invalidMap))
        .isInstanceOf(InternalOperatorErrorException.class);
  }

  @Test
  void testFromDtoMapPlanResult() {
    Union<PlanResult, Map<KtaPolicySpec.TopologyNode, PlanResult>> planResult;
    planResult = Mapper.fromDTO(Union.of(new PlanResultDto(10), null));
    assertThat(planResult.isFirst()).isTrue();
    assertThat(planResult.isSecond()).isFalse();
    assertThat(planResult.first().getParallelism()).isEqualTo(10);
    planResult = Mapper.fromDTO(Union.of(
        null,
        Map.of(
            new TopologyNodeDto(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE, "id=id-1"),
            new PlanResultDto(5),
            new TopologyNodeDto(TopologyNodeDto.Type.FLINK_STREAMING_GRAPH_NODE, "id=id-2"),
            new PlanResultDto(10))));
    assertThat(planResult.isFirst()).isFalse();
    assertThat(planResult.isSecond()).isTrue();
    assertThat(planResult.second().size()).isEqualTo(2);
    assertThat(planResult
            .second()
            .get(new KtaPolicySpec.FlinkStreamingGraphNode("id-1"))
            .getParallelism())
        .isEqualTo(5);
    assertThat(planResult
            .second()
            .get(new KtaPolicySpec.FlinkStreamingGraphNode("id-2"))
            .getParallelism())
        .isEqualTo(10);
  }

  @Test
  void testToCanonicalFormatValid() {
    Map<KtaPolicySpec.TopologyNode, PlanResult> canonicalFormat;

    PlanResult planResult = new PlanResult(5);
    KtaPolicySpec.TopologyNode topologyNode = new KtaPolicySpec.FlinkStreamingGraphNode("id-1");
    canonicalFormat =
        Mapper.toCanonicalFormatOrElseThrow(Union.of(planResult, null), List.of(topologyNode));
    assertThat(canonicalFormat.size()).isEqualTo(1);
    assertThat(canonicalFormat.get(topologyNode).getParallelism()).isEqualTo(5);

    List<KtaPolicySpec.TopologyNode> topologyNodes = List.of(
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
        new KtaPolicySpec.FlinkStreamingGraphNode("id-2"));
    // map can be in any order
    Map<KtaPolicySpec.TopologyNode, PlanResult> results = Map.of(
        new KtaPolicySpec.FlinkStreamingGraphNode("id-2"),
        new PlanResult(10),
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
        new PlanResult(2));
    canonicalFormat = Mapper.toCanonicalFormatOrElseThrow(Union.of(null, results), topologyNodes);
    assertThat(canonicalFormat.size()).isEqualTo(2);
    assertThat(canonicalFormat.get(topologyNodes.get(0)).getParallelism()).isEqualTo(2);
    assertThat(canonicalFormat.get(topologyNodes.get(1)).getParallelism()).isEqualTo(10);
  }

  @Test
  void testToCanonicalFormatInvalid() {
    Map<KtaPolicySpec.TopologyNode, PlanResult> canonicalFormat;

    PlanResult planResult = new PlanResult(5);
    List<KtaPolicySpec.TopologyNode> topologyNodes = List.of(
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
        new KtaPolicySpec.FlinkStreamingGraphNode("id-2"));
    assertThatThrownBy(
            () -> Mapper.toCanonicalFormatOrElseThrow(Union.of(planResult, null), topologyNodes))
        .isInstanceOf(MapperException.class)
        .hasMessageContaining("single plan result object");

    Map<KtaPolicySpec.TopologyNode, PlanResult> results = Map.of(
        new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
        planResult,
        new KtaPolicySpec.FlinkStreamingGraphNode("id-3"),
        planResult);
    assertThatThrownBy(
            () -> Mapper.toCanonicalFormatOrElseThrow(Union.of(null, results), topologyNodes))
        .isInstanceOf(MapperException.class)
        .hasMessageContaining("Expected nodes");
  }
}
