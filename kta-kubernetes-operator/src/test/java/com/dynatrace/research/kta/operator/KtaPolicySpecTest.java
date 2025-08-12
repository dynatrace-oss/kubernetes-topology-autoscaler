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

import com.dynatrace.research.kta.util.TestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.params.support.ParameterDeclarations;

/** Tests for {@link KtaPolicy}. */
public class KtaPolicySpecTest {

  private static final String VALID_BASE_URL = "http://example.com/api/v1alpha1";

  private static final List<KtaPolicySpec.ScaleTargetRef> VALID_SCALE_TARGET_REF_SINGLE =
      List.of(new KtaPolicySpec.ScaleTargetRef(
          KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"));
  private static final List<KtaPolicySpec.ScaleTargetRef> VALID_SCALE_TARGET_REF_MULTIPLE = List.of(
      new KtaPolicySpec.ScaleTargetRef(
          KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"),
      new KtaPolicySpec.ScaleTargetRef(
          KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-2"));

  private static final String VALID_FLINK_JOBMANAGER_ADDRESS = "http://example.com:1234";
  private static final String VALID_JOB_ID = "job-id-1";
  private static final List<KtaPolicySpec.FlinkStreamingGraphNode>
      VALID_FLINK_STREAMING_GRAPH_NODE_SINGLE =
          List.of(new KtaPolicySpec.FlinkStreamingGraphNode("flink-streaming-graph-node-1"));
  private static final List<KtaPolicySpec.FlinkStreamingGraphNode>
      VALID_FLINK_STREAMING_GRAPH_NODE_MULTIPLE = List.of(
          new KtaPolicySpec.FlinkStreamingGraphNode("flink-streaming-graph-node-1"),
          new KtaPolicySpec.FlinkStreamingGraphNode("flink-streaming-graph-node-2"));
  ;

  @ParameterizedTest
  @MethodSource("provideValidAutoscalers")
  void testValidKTAPolicySpec(KtaPolicySpec.ScaleDriver scaleDriver) {
    KtaPolicySpec ktaPolicySpec = new KtaPolicySpec();
    ktaPolicySpec.setScaleDriver(scaleDriver);
    ktaPolicySpec.setBehavior(createValidBehaviorWithOptionalFieldsSet());
    TestUtils.assertThatNoConstraintIsViolated(ktaPolicySpec);
  }

  @Test
  void testInvalidKTAPolicySpec() {
    KtaPolicySpec ktaPolicySpec;

    ktaPolicySpec = new KtaPolicySpec();
    TestUtils.assertThatAtLeastOneConstraintIsViolated(ktaPolicySpec);

    ktaPolicySpec = new KtaPolicySpec();
    ktaPolicySpec.setScaleDriver(createGenericKubernetesAutoscaler(VALID_SCALE_TARGET_REF_SINGLE));
    ktaPolicySpec.setBehavior(null);
    TestUtils.assertThatAtLeastOneConstraintIsViolated(ktaPolicySpec);

    ktaPolicySpec = new KtaPolicySpec();
    ktaPolicySpec.setScaleDriver(null);
    ktaPolicySpec.setBehavior(createValidBehaviorWithOptionalFieldsSet());
    TestUtils.assertThatAtLeastOneConstraintIsViolated(ktaPolicySpec);
  }

  @Test
  void testNestedSpecsInKTAPolicySpecAreValidated() {
    KtaPolicySpec ktaPolicySpec;
    KtaPolicySpec.ScaleDriver scaleDriver;

    ktaPolicySpec = new KtaPolicySpec();
    // autoscale has no scale target refs
    scaleDriver = createGenericKubernetesAutoscaler(List.of());
    ktaPolicySpec.setScaleDriver(scaleDriver);
    ktaPolicySpec.setBehavior(createValidBehaviorWithOptionalFieldsSet());
    TestUtils.assertThatAtLeastOneConstraintIsViolated(ktaPolicySpec);

    ktaPolicySpec = new KtaPolicySpec();
    scaleDriver = createGenericKubernetesAutoscaler(VALID_SCALE_TARGET_REF_SINGLE);
    ktaPolicySpec.setScaleDriver(scaleDriver);
    // behavior does not have udfs set
    KtaPolicySpec.Behavior behavior = new KtaPolicySpec.Behavior();
    ktaPolicySpec.setBehavior(behavior);
    TestUtils.assertThatAtLeastOneConstraintIsViolated(ktaPolicySpec);
  }

  @Nested
  class ScaleDriverSpecTest {

    @Test
    void testInvalidAutoscalerSimple() {
      KtaPolicySpec.ScaleDriver scaleDriver = new KtaPolicySpec.ScaleDriver();
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidScaleTargetRefArgumentsProvider.class)
    void testValidGenericKubernetesAutoscaler(List<KtaPolicySpec.ScaleTargetRef> scaleTargetRefs) {
      KtaPolicySpec.ScaleDriver scaleDriver = createGenericKubernetesAutoscaler(scaleTargetRefs);
      TestUtils.assertThatNoConstraintIsViolated(scaleDriver);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidScaleTargetRefArgumentsProvider.class)
    void testInvalidAutoscalerGenericKubernetes(
        List<KtaPolicySpec.ScaleTargetRef> scaleTargetRefs) {
      KtaPolicySpec.ScaleDriver scaleDriver = new KtaPolicySpec.ScaleDriver();

      // mandatory properties have to bet set
      scaleDriver.setType(KtaPolicySpec.ScaleDriver.Type.GenericKubernetes);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setGenericKubernetesTopology(List.of());
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      List<KtaPolicySpec.ScaleTargetRef> listWithNull = new ArrayList<>();
      listWithNull.add(null);
      scaleDriver.setGenericKubernetesTopology(listWithNull);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      // flink properties must not be set
      scaleDriver = createGenericKubernetesAutoscaler(scaleTargetRefs);
      scaleDriver.setFlinkTopology(List.of());
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setFlinkTopology(VALID_FLINK_STREAMING_GRAPH_NODE_SINGLE);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setFlinkJobManagerBaseUrl(VALID_FLINK_JOBMANAGER_ADDRESS);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setFlinkTopology(null);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);
    }

    @Test
    void testGenericKubernetesAutoscalerTopologyMustBeUnique() {
      KtaPolicySpec.ScaleDriver scaleDriver = createGenericKubernetesAutoscaler(List.of(
          new KtaPolicySpec.ScaleTargetRef(
              KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1"),
          new KtaPolicySpec.ScaleTargetRef(
              KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-2"),
          new KtaPolicySpec.ScaleTargetRef(
              KtaPolicySpec.ScaleTargetRef.Kind.Deployment, "scale-target-ref-1")));
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFlinkStreamingGraphNodeArgumentsProvider.class)
    void testValidFlinkAutoscaler(
        List<KtaPolicySpec.FlinkStreamingGraphNode> flinkStreamingGraphNodes) {
      KtaPolicySpec.ScaleDriver scaleDriver = createFlinkAutoscaler(
          VALID_FLINK_JOBMANAGER_ADDRESS, VALID_JOB_ID, flinkStreamingGraphNodes);
      TestUtils.assertThatNoConstraintIsViolated(scaleDriver);
    }

    @Test
    void testNestedSpecsInFlinkAutoscalerAreValidated() {
      KtaPolicySpec.ScaleDriver scaleDriver = createFlinkAutoscaler(
          VALID_FLINK_JOBMANAGER_ADDRESS, VALID_JOB_ID, VALID_FLINK_STREAMING_GRAPH_NODE_SINGLE);
      // id must not be null
      scaleDriver.setFlinkTopology(List.of(new KtaPolicySpec.FlinkStreamingGraphNode(null)));
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFlinkStreamingGraphNodeArgumentsProvider.class)
    void testInvalidAutoscalerFlink(
        List<KtaPolicySpec.FlinkStreamingGraphNode> flinkStreamingGraphNodes) {
      KtaPolicySpec.ScaleDriver scaleDriver = new KtaPolicySpec.ScaleDriver();

      // mandatory properties have to be set
      scaleDriver.setType(KtaPolicySpec.ScaleDriver.Type.Flink);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setFlinkJobManagerBaseUrl(VALID_FLINK_JOBMANAGER_ADDRESS);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setFlinkTopology(List.of());
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      List<KtaPolicySpec.FlinkStreamingGraphNode> listWithNull = new ArrayList<>();
      listWithNull.add(null);
      scaleDriver.setFlinkTopology(listWithNull);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setFlinkJobManagerBaseUrl(null);
      scaleDriver.setFlinkTopology(flinkStreamingGraphNodes);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      // mutation of valid autoscaler
      scaleDriver = createFlinkAutoscaler(
          VALID_FLINK_JOBMANAGER_ADDRESS, VALID_JOB_ID, flinkStreamingGraphNodes);
      scaleDriver.setFlinkJobManagerBaseUrl("~not-a-valid-job-manager-address~");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      // generic kubernetes autoscaler properties must not bet set
      scaleDriver = createFlinkAutoscaler(
          VALID_FLINK_JOBMANAGER_ADDRESS, VALID_JOB_ID, flinkStreamingGraphNodes);
      scaleDriver.setGenericKubernetesTopology(List.of());
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);

      scaleDriver.setGenericKubernetesTopology(VALID_SCALE_TARGET_REF_SINGLE);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);
    }

    @Test
    void testAutoscalerFlinkTopologyMustBeUnique() {
      KtaPolicySpec.ScaleDriver scaleDriver = createFlinkAutoscaler(
          VALID_FLINK_JOBMANAGER_ADDRESS,
          VALID_JOB_ID,
          List.of(
              new KtaPolicySpec.FlinkStreamingGraphNode("id-1"),
              new KtaPolicySpec.FlinkStreamingGraphNode("id-2"),
              new KtaPolicySpec.FlinkStreamingGraphNode("id-1")));
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleDriver);
    }
  }

  @Nested
  class BehaviorSpecTest {

    @Test
    void testValidBehaviorSimple() {
      KtaPolicySpec.Behavior behavior = createValidBehaviorWithOptionalFieldsSet();
      TestUtils.assertThatNoConstraintIsViolated(behavior);
    }

    @Test
    void testNestedSpecsInBehaviorAreValidated() {
      // mutate valid behavior by setting udfs with missing mandatory plan endpoint
      KtaPolicySpec.Behavior behavior = createValidBehaviorWithOptionalFieldsSet();
      // plan udf is not set
      KtaPolicySpec.Behavior.UserDefinedFunctions udfs =
          new KtaPolicySpec.Behavior.UserDefinedFunctions();
      udfs.setMonitorEndpoint(VALID_BASE_URL + "/monitor");
      behavior.setUdfs(udfs);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);
    }

    @Test
    void testInvalidBehavior() {
      KtaPolicySpec.Behavior behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setMinParallelism(0);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);

      behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setMinParallelism(-1);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);

      behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setMaxParallelism(0);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);

      behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setMaxParallelism(-1);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);

      behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setMinParallelism(10);
      behavior.setMaxParallelism(9);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);

      behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setReconciliationIntervalSeconds(0);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);

      behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setReconciliationIntervalSeconds(-1);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);

      behavior = createValidBehaviorWithOptionalFieldsSet();
      behavior.setUdfs(null);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(behavior);
    }
  }

  @Nested
  class TopologyNodeSpecTest {

    @ParameterizedTest
    @ArgumentsSource(ValidScaleTargetRefArgumentsProvider.class)
    void testValidScaleTargetRefs(List<KtaPolicySpec.ScaleTargetRef> scaleTargetRefs) {
      scaleTargetRefs.forEach(TestUtils::assertThatNoConstraintIsViolated);
    }

    @Test
    void testInvalidScaleTargetRefs() {
      KtaPolicySpec.ScaleTargetRef scaleTargetRef;

      scaleTargetRef = new KtaPolicySpec.ScaleTargetRef();
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleTargetRef);

      scaleTargetRef = new KtaPolicySpec.ScaleTargetRef();
      scaleTargetRef.setKind(null);
      scaleTargetRef.setName("scale-target-ref");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleTargetRef);

      scaleTargetRef = new KtaPolicySpec.ScaleTargetRef();
      scaleTargetRef.setKind(KtaPolicySpec.ScaleTargetRef.Kind.Deployment);
      scaleTargetRef.setName("");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleTargetRef);

      scaleTargetRef = new KtaPolicySpec.ScaleTargetRef();
      scaleTargetRef.setKind(KtaPolicySpec.ScaleTargetRef.Kind.Deployment);
      scaleTargetRef.setName(null);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(scaleTargetRef);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFlinkStreamingGraphNodeArgumentsProvider.class)
    void testValidFlinkStreamingGraphNodes(
        List<KtaPolicySpec.FlinkStreamingGraphNode> flinkStreamingGraphNodes) {
      flinkStreamingGraphNodes.forEach(TestUtils::assertThatNoConstraintIsViolated);
    }

    @Test
    void testInvalidFlinkStreamingGraphNodes() {
      KtaPolicySpec.FlinkStreamingGraphNode flinkStreamingGraphNode;

      flinkStreamingGraphNode = new KtaPolicySpec.FlinkStreamingGraphNode("");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(flinkStreamingGraphNode);

      flinkStreamingGraphNode = new KtaPolicySpec.FlinkStreamingGraphNode("   ");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(flinkStreamingGraphNode);

      flinkStreamingGraphNode = new KtaPolicySpec.FlinkStreamingGraphNode(null);
      TestUtils.assertThatAtLeastOneConstraintIsViolated(flinkStreamingGraphNode);
    }
  }

  @Nested
  class UserDefinedFunctionsSpecTest {

    @Test
    void testValidUserDefinedFunctions() {
      KtaPolicySpec.Behavior.UserDefinedFunctions userDefinedFunctions =
          new KtaPolicySpec.Behavior.UserDefinedFunctions();
      // analyze endpoint is optional
      userDefinedFunctions.setMonitorEndpoint(VALID_BASE_URL + "/monitor");
      userDefinedFunctions.setPlanEndpoint(VALID_BASE_URL + "/plan");
      TestUtils.assertThatNoConstraintIsViolated(userDefinedFunctions);

      userDefinedFunctions.setAnalyzeEndpoint(VALID_BASE_URL + "/analyze");
      TestUtils.assertThatNoConstraintIsViolated(userDefinedFunctions);
    }

    @Test
    void testInvalidUserDefinedFunctions() {
      KtaPolicySpec.Behavior.UserDefinedFunctions userDefinedFunctions;

      // mandatory fields must be set
      userDefinedFunctions = new KtaPolicySpec.Behavior.UserDefinedFunctions();
      TestUtils.assertThatAtLeastOneConstraintIsViolated(userDefinedFunctions);

      userDefinedFunctions.setAnalyzeEndpoint(VALID_BASE_URL + "/analyze");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(userDefinedFunctions);

      userDefinedFunctions.setMonitorEndpoint(VALID_BASE_URL + "/monitor");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(userDefinedFunctions);

      userDefinedFunctions.setMonitorEndpoint(null);
      userDefinedFunctions.setPlanEndpoint(VALID_BASE_URL + "/analyze");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(userDefinedFunctions);

      userDefinedFunctions = new KtaPolicySpec.Behavior.UserDefinedFunctions();
      userDefinedFunctions.setMonitorEndpoint(VALID_BASE_URL + "/monitor");
      userDefinedFunctions.setPlanEndpoint("ftp://endpoint-with-wrong-protocol");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(userDefinedFunctions);

      userDefinedFunctions = new KtaPolicySpec.Behavior.UserDefinedFunctions();
      userDefinedFunctions.setMonitorEndpoint(VALID_BASE_URL + "/monitor");
      userDefinedFunctions.setPlanEndpoint("endpoint-with-missing-protocol");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(userDefinedFunctions);

      userDefinedFunctions = new KtaPolicySpec.Behavior.UserDefinedFunctions();
      userDefinedFunctions.setMonitorEndpoint(VALID_BASE_URL + "/monitor");
      userDefinedFunctions.setPlanEndpoint("     ");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(userDefinedFunctions);
    }
  }

  @Nested
  class StrategySpecTest {
    @Test
    void testValidNoStrategy() {
      // NO_STRATEGY does not accept arguments
      KtaPolicySpec.Behavior.ScaleStrategy strategy;

      strategy = new KtaPolicySpec.Behavior.ScaleStrategy();
      strategy.setType(KtaPolicySpec.Behavior.ScaleStrategy.Type.NoStrategy);
      TestUtils.assertThatNoConstraintIsViolated(strategy);

      strategy = new KtaPolicySpec.Behavior.ScaleStrategy();
      strategy.setType(KtaPolicySpec.Behavior.ScaleStrategy.Type.NoStrategy);
      strategy.setArgs("");
      TestUtils.assertThatNoConstraintIsViolated(strategy);
    }

    @Test
    void testInvalidNoStrategy() {
      KtaPolicySpec.Behavior.ScaleStrategy strategy = new KtaPolicySpec.Behavior.ScaleStrategy();
      strategy.setType(KtaPolicySpec.Behavior.ScaleStrategy.Type.NoStrategy);
      strategy.setArgs("NO_STRATEGY-does-not-accept-arguments");
      TestUtils.assertThatAtLeastOneConstraintIsViolated(strategy);
    }
  }

  static class ValidScaleTargetRefArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<Arguments> provideArguments(ParameterDeclarations __, ExtensionContext context) {
      return Stream.of(
          Arguments.of(VALID_SCALE_TARGET_REF_SINGLE),
          Arguments.of(VALID_SCALE_TARGET_REF_MULTIPLE));
    }
  }

  static class ValidFlinkStreamingGraphNodeArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<Arguments> provideArguments(ParameterDeclarations __, ExtensionContext context) {
      return Stream.of(
          Arguments.of(VALID_FLINK_STREAMING_GRAPH_NODE_SINGLE),
          Arguments.of(VALID_FLINK_STREAMING_GRAPH_NODE_MULTIPLE));
    }
  }

  static Stream<Arguments> provideValidAutoscalers() {
    return Stream.of(
        Arguments.of(createGenericKubernetesAutoscaler(VALID_SCALE_TARGET_REF_SINGLE)),
        Arguments.of(createGenericKubernetesAutoscaler(VALID_SCALE_TARGET_REF_MULTIPLE)),
        Arguments.of(createFlinkAutoscaler(
            VALID_FLINK_JOBMANAGER_ADDRESS, VALID_JOB_ID, VALID_FLINK_STREAMING_GRAPH_NODE_SINGLE)),
        Arguments.of(createFlinkAutoscaler(
            VALID_FLINK_JOBMANAGER_ADDRESS,
            VALID_JOB_ID,
            VALID_FLINK_STREAMING_GRAPH_NODE_MULTIPLE)));
  }

  static KtaPolicySpec.ScaleDriver createGenericKubernetesAutoscaler(
      List<KtaPolicySpec.ScaleTargetRef> scaleTargetRefs) {
    KtaPolicySpec.ScaleDriver scaleDriver = new KtaPolicySpec.ScaleDriver();
    scaleDriver.setType(KtaPolicySpec.ScaleDriver.Type.GenericKubernetes);
    scaleDriver.setGenericKubernetesTopology(scaleTargetRefs);
    return scaleDriver;
  }

  static KtaPolicySpec.ScaleDriver createFlinkAutoscaler(
      String jobManagerAddress,
      String jobId,
      List<KtaPolicySpec.FlinkStreamingGraphNode> flinkStreamingGraphNodes) {
    KtaPolicySpec.ScaleDriver scaleDriver = new KtaPolicySpec.ScaleDriver();
    scaleDriver.setType(KtaPolicySpec.ScaleDriver.Type.Flink);
    scaleDriver.setFlinkJobManagerBaseUrl(jobManagerAddress);
    scaleDriver.setFlinkJobId(jobId);
    scaleDriver.setFlinkTopology(flinkStreamingGraphNodes);
    return scaleDriver;
  }

  static KtaPolicySpec.Behavior createValidBehaviorWithOptionalFieldsSet() {
    KtaPolicySpec.Behavior behavior = new KtaPolicySpec.Behavior();
    behavior.setMinParallelism(1);
    behavior.setMaxParallelism(10);
    KtaPolicySpec.Behavior.UserDefinedFunctions udfs =
        new KtaPolicySpec.Behavior.UserDefinedFunctions();
    udfs.setMonitorEndpoint(VALID_BASE_URL + "/monitor");
    udfs.setAnalyzeEndpoint(VALID_BASE_URL + "/analyze");
    udfs.setPlanEndpoint(VALID_BASE_URL + "/plan");
    behavior.setUdfs(udfs);
    return behavior;
  }
}
