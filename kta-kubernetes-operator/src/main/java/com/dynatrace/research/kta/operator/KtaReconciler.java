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

import com.dynatrace.research.kta.config.DependencyFactory;
import com.dynatrace.research.kta.exception.InternalOperatorErrorException;
import com.dynatrace.research.kta.operator.persistence.KnowledgeStore;
import com.dynatrace.research.kta.operator.persistence.Result;
import com.dynatrace.research.kta.operator.scaling.ScaleDriver;
import com.dynatrace.research.kta.operator.udf.UdfInvocationHandler;
import com.dynatrace.research.kta.operator.udf.UdfResult;
import com.dynatrace.research.kta.operator.util.IdGenerator;
import com.dynatrace.research.kta.operator.util.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.*;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates the autoscaling process (MAPE-K loop).
 *
 * <ol>
 *   <li>Uses a {@link UdfInvocationHandler} to invoke UDFs and collect results (<b>M</b>onitor,
 *       <b>A</b>nalyze, <b>P</b>lan)
 *   <li>Applies the behavior configured in the CRD to the plan result (e.g., min and max
 *       parallelism).
 *   <li>Stores the result in a {@link KnowledgeStore} (<b>K</b>nowledge) and initiates the scaling
 *       action using a {@link ScaleDriver} (<b>E</b>xecute)
 * </ol>
 */
@ControllerConfiguration
public final class KtaReconciler implements Reconciler<KtaPolicy>, Cleaner<KtaPolicy> {

  private static final Logger LOG = LoggerFactory.getLogger(KtaReconciler.class);

  private final IdGenerator idGenerator;
  private final Clock clock;
  private final UdfInvocationHandler udfInvocationHandler;
  private final KnowledgeStore<Result> knowledgeStore;
  private final Function<KtaPolicySpec.ScaleDriver.Type, ScaleDriver> scaleDriverInitializer;
  private final Map<String, ScaleDriver> scaleDrivers = new HashMap<>();

  private KtaPolicySpec spec;
  private String crdName;
  private List<? extends KtaPolicySpec.TopologyNode> topology;
  private KtaPolicySpec.Behavior.UserDefinedFunctions userDefinedFunctions;
  private List<Result> resultHistory;

  @Inject
  public KtaReconciler(final DependencyFactory dependencyFactory) {
    this.idGenerator = dependencyFactory.getIdGenerator();
    this.clock = dependencyFactory.getClock();
    this.udfInvocationHandler = dependencyFactory.getUdfInvocationHandler();
    this.knowledgeStore = dependencyFactory.getKnowledgeStore();
    this.scaleDriverInitializer = dependencyFactory::getScaleDriver;
  }

  @Override
  public UpdateControl<KtaPolicy> reconcile(
      final KtaPolicy resource, final Context<KtaPolicy> context) {
    LOG.info("Starting reconciliation at {}", this.clock.instant());

    KtaPolicyStatus status = initializeStatusIfAbsent(resource);
    initializeReconciler(resource);

    if (status.getState() == null || status.getState() == KtaPolicyStatus.State.Completed) {
      LOG.debug("Starting: {}", KtaPolicyStatus.State.New);
      status.setState(KtaPolicyStatus.State.New);
      LOG.debug("Completed: {}", KtaPolicyStatus.State.New);
    }

    String id = null;
    if (status.getState() == KtaPolicyStatus.State.New) {
      LOG.debug("Starting: {}", KtaPolicyStatus.State.Init);
      id = this.idGenerator.generate();
      status.setId(id);
      status.setState(KtaPolicyStatus.State.Init);
      LOG.debug("Completed: {}", KtaPolicyStatus.State.Init);
    }

    UdfResult udfResult = null;
    if (status.getState() == KtaPolicyStatus.State.Init) {
      LOG.debug("Starting: {}", KtaPolicyStatus.State.MonitorAnalyzePlan);
      id = (id != null) ? id : status.getId();
      udfResult = this.udfInvocationHandler.invoke(
          this.clock, id, this.topology, this.resultHistory, this.userDefinedFunctions);
      status.setUdfResult(ReconcilerUtils.serializeUdfResult(udfResult));
      status.setState(KtaPolicyStatus.State.MonitorAnalyzePlan);
      LOG.debug("Completed: {}", KtaPolicyStatus.State.MonitorAnalyzePlan);
    }

    if (status.getState() == KtaPolicyStatus.State.MonitorAnalyzePlan) {
      LOG.debug("Starting: {}", KtaPolicyStatus.State.Knowledge);
      udfResult = (udfResult != null)
          ? udfResult
          : ReconcilerUtils.deserializeUdfResult(status.getUdfResult());
      Result result = getResult(
          this.spec.getBehavior().getScaleDownStrategy().getType(),
          this.spec.getBehavior().getScaleUpStrategy().getType(),
          this.spec.getBehavior().getMinParallelism(),
          this.spec.getBehavior().getMaxParallelism(),
          udfResult);
      status.setResult(ReconcilerUtils.serializeResult(result));
      this.knowledgeStore.add(this.crdName, result);
      status.setState(KtaPolicyStatus.State.Knowledge);
      LOG.debug("Completed: {}", KtaPolicyStatus.State.Knowledge);
    }

    if (status.getState() == KtaPolicyStatus.State.Knowledge) {
      LOG.debug("Starting: {}", KtaPolicyStatus.State.Execute);
      Result result = this.knowledgeStore.getLatest(this.crdName);
      ScaleDriver scaleDriver = this.scaleDrivers.computeIfAbsent(
          this.crdName,
          __ -> this.scaleDriverInitializer.apply(this.spec.getScaleDriver().getType()));
      scaleDriver.scale(resource.getSpec().getScaleDriver(), result.getParallelism());
      status.setState(KtaPolicyStatus.State.Execute);
      LOG.debug("Completed: {}", KtaPolicyStatus.State.Execute);
    }

    status.setState(KtaPolicyStatus.State.Completed);

    LOG.info(
        "Reconciliation successfully completed at {}. Next reconciliation in {}s latest.",
        this.clock.instant(),
        resource.getSpec().getBehavior().getReconciliationIntervalSeconds());

    return UpdateControl.<KtaPolicy>patchStatus(resource)
        .rescheduleAfter(Duration.of(
            resource.getSpec().getBehavior().getReconciliationIntervalSeconds(),
            ChronoUnit.SECONDS));
  }

  private void initializeReconciler(final KtaPolicy resource) {
    this.spec = resource.getSpec();
    this.crdName = resource.getCRDName();
    this.userDefinedFunctions = this.spec.getBehavior().getUdfs();
    this.topology = ReconcilerUtils.getTopology(this.spec.getScaleDriver());
    this.resultHistory = this.knowledgeStore.get(
        this.crdName, resource.getSpec().getBehavior().getResultHistoryLength());
  }

  private Result getResult(
      KtaPolicySpec.Behavior.ScaleStrategy.Type scaleDownStrategyType,
      KtaPolicySpec.Behavior.ScaleStrategy.Type scaleUpStrategyType,
      int minParallelism,
      int maxParallelism,
      final UdfResult udfResult) {
    // TODO: currently do nothing, because strategies are not implemented yet
    if (scaleDownStrategyType != KtaPolicySpec.Behavior.ScaleStrategy.Type.NoStrategy) {
      throw new InternalOperatorErrorException(
          "Unknown scale down strategy type " + scaleUpStrategyType);
    }

    if (scaleUpStrategyType != KtaPolicySpec.Behavior.ScaleStrategy.Type.NoStrategy) {
      throw new InternalOperatorErrorException(
          "Unknown scale up strategy type " + scaleUpStrategyType);
    }

    Map<KtaPolicySpec.TopologyNode, Integer> parallelism =
        udfResult.getPlanResult().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> {
              int p = e.getValue().getParallelism();
              if (p < minParallelism) {
                LOG.info(
                    "Parallelism of {} returned by UDF for {} is smaller than min parallelism. Using min parallelism of {}",
                    p,
                    e.getKey(),
                    minParallelism);
                return minParallelism;
              } else if (p > maxParallelism) {
                LOG.info(
                    "Parallelism of {} returned by UDF for {} is greater than max parallelism. Using max parallelism of {}",
                    p,
                    e.getKey(),
                    maxParallelism);
                return maxParallelism;
              } else {
                return e.getValue().getParallelism();
              }
            }));

    return Result.builder(udfResult).withParallelism(parallelism).build();
  }

  @Override
  public ErrorStatusUpdateControl<KtaPolicy> updateErrorStatus(
      final KtaPolicy resource, final Context<KtaPolicy> context, final Exception e) {
    LOG.error("Reconciliation failed at state {}", resource.getStatus().getState());
    KtaPolicyStatus status = initializeStatusIfAbsent(resource);
    status.setError(true);
    return ErrorStatusUpdateControl.patchStatus(resource);
  }

  private static KtaPolicyStatus initializeStatusIfAbsent(final KtaPolicy resource) {
    KtaPolicyStatus status =
        resource.getStatus() != null ? resource.getStatus() : new KtaPolicyStatus();
    resource.setStatus(status);
    return status;
  }

  @Override
  public DeleteControl cleanup(final KtaPolicy resource, final Context<KtaPolicy> context) {
    LOG.info("Cleaning up resource {}", resource.getCRDName());
    String crdName = resource.getCRDName();
    this.knowledgeStore.remove(crdName);
    this.scaleDrivers.remove(crdName);
    return DeleteControl.defaultDelete();
  }
}
