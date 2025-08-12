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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.generator.annotation.Default;
import io.fabric8.generator.annotation.Required;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.hibernate.validator.constraints.URL;

/** KtaPolicy CRD specification. */
public final class KtaPolicySpec {

  @JsonPropertyDescription(
      "Scale Driver. Different fields may be required, depending on the type. These fields are marked with `n/a`. Check the description to see if the field is required for a specific Scale Driver. Currently, a Scale Driver **must not** be updated.")
  @NotNull @Required
  private @Valid KtaPolicySpec.ScaleDriver scaleDriver;

  @JsonPropertyDescription("Policy behavior.")
  @NotNull @Required
  private @Valid Behavior behavior;

  public ScaleDriver getScaleDriver() {
    return this.scaleDriver;
  }

  public void setScaleDriver(final ScaleDriver scaleDriver) {
    this.scaleDriver = scaleDriver;
  }

  public Behavior getBehavior() {
    return this.behavior;
  }

  public void setBehavior(final Behavior behavior) {
    this.behavior = behavior;
  }

  public static final class ScaleDriver {

    public enum Type {
      GenericKubernetes,
      Flink
    }

    public enum FlinkJobDeploymentType {
      SharedTaskSlots,
      StreamingGraphNodePerTaskSlot
    }

    @JsonPropertyDescription("Scale driver type.")
    @Required
    @NotNull private Type type;

    public Type getType() {
      return this.type;
    }

    public void setType(final Type type) {
      this.type = type;
    }

    // begin GenericKubernetes
    @JsonPropertyDescription(
        "Topology nodes in topologically sorted order. Required for `GenericKubernetes` Scale Driver. ")
    private List<@Valid @NotNull ScaleTargetRef> genericKubernetesTopology;

    public List<ScaleTargetRef> getGenericKubernetesTopology() {
      return this.genericKubernetesTopology;
    }

    public void setGenericKubernetesTopology(final List<ScaleTargetRef> genericKubernetesTopology) {
      this.genericKubernetesTopology = genericKubernetesTopology;
    }
    // end

    // begin Flink
    @JsonPropertyDescription(
        "Job manager base URL. Required for `Flink` Scale Driver. Pattern: ^(http|https).*")
    @URL(
        regexp = "^(http|https).*",
        message =
            "Hints: 1. Did you set the protocol? 2. Only http and https are allowed protocols.")
    private String flinkJobManagerBaseUrl;

    @JsonPropertyDescription("ID of the job to be scaled. Required for `Flink` Scale Driver. ")
    private String flinkJobId;

    @JsonPropertyDescription(
        "Flink job deployment type to determine the required number of task manager deployments. Required for `Flink` Scale Driver. ")
    private FlinkJobDeploymentType flinkJobDeploymentType;

    @JsonPropertyDescription(
        "Name of the task manager deployment (Kubernetes API version: v1) to automatically scale the number of task managers with the parallelism. Each task manager must contain only a single task slot, this is, one task manager with 1 slot maps to a single replica of the deployment. If not set, the number of task managers has to be scaled externally, e.g., by using the Flink Kubernetes Operator.")
    private String flinkTaskManagerDeploymentName;

    @JsonPropertyDescription(
        "Topology nodes (Flink Streaming Graph Nodes) in topologically sorted order. Required for `Flink` Scale Driver. ")
    private List<@Valid @NotNull FlinkStreamingGraphNode> flinkTopology;

    public String getFlinkJobManagerBaseUrl() {
      return this.flinkJobManagerBaseUrl;
    }

    public void setFlinkJobManagerBaseUrl(final String flinkJobManagerBaseUrl) {
      this.flinkJobManagerBaseUrl = flinkJobManagerBaseUrl;
    }

    public String getFlinkJobId() {
      return this.flinkJobId;
    }

    public void setFlinkJobId(final String flinkJobId) {
      this.flinkJobId = flinkJobId;
    }

    public List<FlinkStreamingGraphNode> getFlinkTopology() {
      return this.flinkTopology;
    }

    public void setFlinkTopology(final List<FlinkStreamingGraphNode> flinkTopology) {
      this.flinkTopology = flinkTopology;
    }

    public String getFlinkTaskManagerDeploymentName() {
      return this.flinkTaskManagerDeploymentName;
    }

    public void setFlinkTaskManagerDeploymentName(
        @Nullable final String flinkTaskManagerDeploymentName) {
      this.flinkTaskManagerDeploymentName = flinkTaskManagerDeploymentName;
    }

    public FlinkJobDeploymentType getFlinkJobDeploymentType() {
      return this.flinkJobDeploymentType;
    }

    public void setFlinkJobDeploymentType(final FlinkJobDeploymentType flinkJobDeploymentType) {
      this.flinkJobDeploymentType = flinkJobDeploymentType;
    }
    // end

    @AssertTrue(message = "Only fields relevant for the chosen scale driver should be set.") boolean isValidFieldsForType() {
      if (this.type == null) {
        return false;
      }
      return switch (this.type) {
        case GenericKubernetes ->
          this.flinkJobManagerBaseUrl == null
              && this.flinkJobId == null
              && this.flinkJobDeploymentType == null
              && this.flinkTaskManagerDeploymentName == null
              && this.flinkTopology == null;
        case Flink -> this.genericKubernetesTopology == null;
      };
    }

    @AssertTrue(message = "Invalid `GenericKubernetes` scale driver.") boolean isValidGenericAutoscaler() {
      if (this.type == Type.GenericKubernetes) {
        return this.genericKubernetesTopology != null
            && !this.genericKubernetesTopology.isEmpty()
            && new HashSet<>(this.genericKubernetesTopology).size()
                == this.genericKubernetesTopology.size();
      } else {
        return true;
      }
    }

    @AssertTrue(message = "Invalid `Flink` scale driver.") boolean isValidFlinkAutoscaler() {
      if (this.type == Type.Flink) {
        return this.flinkJobManagerBaseUrl != null
            && !this.flinkJobManagerBaseUrl.isEmpty()
            && this.flinkJobId != null
            && !this.flinkJobId.isEmpty()
            // When the task manager deployment name is given, the scale driver takes
            // care of scaling.
            // To determine the needed number of task managers, it needs information how the Flink
            // job is deployed.
            && ((this.flinkTaskManagerDeploymentName == null)
                == (this.flinkJobDeploymentType == null))
            && this.flinkTopology != null
            && !this.flinkTopology.isEmpty()
            && new HashSet<>(this.flinkTopology).size() == this.flinkTopology.size();
      } else {
        return true;
      }
    }
  }

  /**
   * Extending classes must be serializable using {@link ObjectMapper}. Hence, their exposed
   * properties must be annotated with {@code JsonProperty}.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
  @JsonSubTypes({
    @JsonSubTypes.Type(ScaleTargetRef.class),
    @JsonSubTypes.Type(FlinkStreamingGraphNode.class)
  })
  public abstract static class TopologyNode {}

  public static final class ScaleTargetRef extends TopologyNode {

    public enum Kind {
      Deployment
    }

    @JsonPropertyDescription("Kubernetes resource kind. Kubernetes API versions: Deployment -- v1.")
    @NotNull @JsonProperty
    @Required
    private @Valid Kind kind;

    @JsonPropertyDescription("Kubernetes resource name.")
    @NotNull @NotEmpty @NotBlank @JsonProperty
    @Required
    private String name;

    public ScaleTargetRef() {}

    public ScaleTargetRef(final Kind kind, final String name) {
      this.kind = kind;
      this.name = name;
    }

    public Kind getKind() {
      return this.kind;
    }

    public void setKind(final Kind kind) {
      this.kind = kind;
    }

    public String getName() {
      return this.name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof ScaleTargetRef that)) {
        return false;
      }
      return this.kind == that.kind && this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
      int result = this.kind == null ? super.hashCode() : this.kind.hashCode();
      result = 31 * result + (this.name == null ? super.hashCode() : this.name.hashCode());
      return result;
    }

    @Override
    public String toString() {
      return "ScaleTargetRef{" + "kind=" + this.kind + ", name='" + this.name + '\'' + '}';
    }
  }

  public static final class FlinkStreamingGraphNode extends TopologyNode {

    @JsonPropertyDescription("Operator ID.")
    @NotNull @NotEmpty @NotBlank @JsonProperty
    @Required
    private String id;

    public FlinkStreamingGraphNode() {}

    public FlinkStreamingGraphNode(final String id) {
      this.id = id;
    }

    public String getId() {
      return this.id;
    }

    public void setId(final String id) {
      this.id = id;
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof FlinkStreamingGraphNode that)) {
        return false;
      }
      return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
      return this.id == null ? super.hashCode() : this.id.hashCode();
    }
  }

  public static final class Behavior {
    @JsonPropertyDescription("Reconciliation interval in seconds.")
    @Positive(message = "reconciliationIntervalSeconds must be >= 1.") @Default("30")
    private int reconciliationIntervalSeconds = 30;

    @JsonPropertyDescription("Minimum parallelism of each topology node.")
    @Positive(message = "minParallelism must be >= 1.") @Default("1")
    private int minParallelism = 1;

    @JsonPropertyDescription(
        "Maximum parallelism of each topology node. Must be greater than or equal to the minimum parallelism.")
    @Positive(message = "maxParallelism must be >= 1.") @Default("" + Integer.MAX_VALUE)
    private int maxParallelism = Integer.MAX_VALUE;

    @JsonPropertyDescription("Maximum number of past results that are provided to UDFs.")
    @Default("" + Integer.MAX_VALUE)
    @Positive(message = "resultHistoryLength must be >= 1.") private @Valid int resultHistoryLength = Integer.MAX_VALUE;

    @JsonPropertyDescription("UDFs of the autoscaling algorithm.")
    @NotNull @Required
    private @Valid UserDefinedFunctions udfs;

    @JsonPropertyDescription(
        "Strategy that should be applied when the plan UDF triggers a scale up. "
            + "Default: NoStrategy")
    @NotNull private @Valid ScaleStrategy scaleUpStrategy = new ScaleStrategy(ScaleStrategy.Type.NoStrategy);

    @JsonPropertyDescription(
        "Strategy that should be applied when the plan UDF triggers a scale down. "
            + "Default: NoStrategy")
    @NotNull private @Valid ScaleStrategy scaleDownStrategy =
        new ScaleStrategy(ScaleStrategy.Type.NoStrategy);

    @AssertTrue(
        message = "Minimum parallelism must be smaller than or equal to the maximum parallelism.")
    boolean isValidParallelismRange() {
      return this.minParallelism <= this.maxParallelism;
    }

    public int getReconciliationIntervalSeconds() {
      return this.reconciliationIntervalSeconds;
    }

    public void setReconciliationIntervalSeconds(final int reconciliationIntervalSeconds) {
      this.reconciliationIntervalSeconds = reconciliationIntervalSeconds;
    }

    public int getMinParallelism() {
      return this.minParallelism;
    }

    public void setMinParallelism(final int minParallelism) {
      this.minParallelism = minParallelism;
    }

    public int getMaxParallelism() {
      return this.maxParallelism;
    }

    public void setMaxParallelism(final int maxParallelism) {
      this.maxParallelism = maxParallelism;
    }

    public int getResultHistoryLength() {
      return this.resultHistoryLength;
    }

    public void setResultHistoryLength(final int resultHistoryLength) {
      this.resultHistoryLength = resultHistoryLength;
    }

    public UserDefinedFunctions getUdfs() {
      return this.udfs;
    }

    public void setUdfs(final UserDefinedFunctions udfs) {
      this.udfs = udfs;
    }

    public ScaleStrategy getScaleUpStrategy() {
      return this.scaleUpStrategy;
    }

    public void setScaleUpStrategy(final ScaleStrategy scaleUpStrategy) {
      this.scaleUpStrategy = scaleUpStrategy;
    }

    public ScaleStrategy getScaleDownStrategy() {
      return this.scaleDownStrategy;
    }

    public void setScaleDownStrategy(final ScaleStrategy scaleDownStrategy) {
      this.scaleDownStrategy = scaleDownStrategy;
    }

    @Override
    public String toString() {
      return "Behavior{" + "reconciliationIntervalSeconds="
          + this.reconciliationIntervalSeconds + ", minParallelism="
          + this.minParallelism + ", maxParallelism="
          + this.maxParallelism + ", resultHistoryLength="
          + this.resultHistoryLength + ", udfs="
          + this.udfs + ", scaleUpStrategy="
          + this.scaleUpStrategy + ", scaleDownStrategy="
          + this.scaleDownStrategy + '}';
    }

    public static final class UserDefinedFunctions {
      @JsonPropertyDescription("Monitor UDF endpoint. Pattern: ^(http|https).*")
      @NotNull @NotEmpty @NotBlank @URL(
          regexp = "^(http|https).*",
          message = "Hints: 1. Did you set the protocol? 2. Only http and https are supported.")
      @Required
      private String monitorEndpoint;

      @JsonPropertyDescription("Analyze UDF endpoint. Pattern: ^(http|https).*")
      @Nullable @URL(
          regexp = "^(http|https).*",
          message = "Hints: 1. Did you set the protocol? 2. Only http and https are supported.")
      private String analyzeEndpoint;

      @JsonPropertyDescription("Plan UDF endpoint. Pattern: ^(http|https).*")
      @NotNull @NotEmpty @NotBlank @URL(
          regexp = "^(http|https).*",
          message = "Hints: 1. Did you set the protocol? 2. Only http and https are supported.")
      @Required
      private String planEndpoint;

      public String getMonitorEndpoint() {
        return this.monitorEndpoint;
      }

      public void setMonitorEndpoint(final String monitorEndpoint) {
        this.monitorEndpoint = monitorEndpoint;
      }

      public String getAnalyzeEndpoint() {
        return this.analyzeEndpoint;
      }

      public void setAnalyzeEndpoint(final String analyzeEndpoint) {
        this.analyzeEndpoint = analyzeEndpoint;
      }

      public String getPlanEndpoint() {
        return this.planEndpoint;
      }

      public void setPlanEndpoint(final String planEndpoint) {
        this.planEndpoint = planEndpoint;
      }

      @Override
      public String toString() {
        return "UserDefinedFunctions{" + "monitorEndpoint='"
            + this.monitorEndpoint + '\'' + ", analyzeEndpoint='"
            + this.analyzeEndpoint + '\'' + ", planEndpoint='"
            + this.planEndpoint + '\'' + '}';
      }
    }

    public static final class ScaleStrategy {

      public enum Type {
        NoStrategy
      }

      @JsonPropertyDescription("Scale strategy.")
      @NotNull @Required
      private @Valid Type type;

      @JsonPropertyDescription(
          "Arguments that configure the scale strategy. Arguments are always passed as string. "
              + "The format and semantics of the arguments differ based on the scale strategy.")
      @Nullable private String args;

      public ScaleStrategy() {}

      public ScaleStrategy(final Type type) {
        this.type = type;
      }

      public ScaleStrategy(final Type type, @Nullable final String args) {
        this.type = type;
        this.args = args;
      }

      public Type getType() {
        return this.type;
      }

      public void setType(final Type type) {
        this.type = type;
      }

      public String getArgs() {
        return this.args;
      }

      public void setArgs(@Nullable final String args) {
        this.args = args;
      }

      @Override
      public String toString() {
        return "ScaleStrategy{" + "type=" + this.type + ", args='" + this.args + '\'' + '}';
      }

      @AssertTrue(message = "NoStrategy does not accept arguments.") boolean isValidNoStrategyValue() {
        if (this.type == Type.NoStrategy) {
          return this.args == null || this.args.isBlank();
        } else {
          return true;
        }
      }
    }
  }
}
