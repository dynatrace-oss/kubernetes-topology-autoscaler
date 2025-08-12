#   Copyright (c) 2024 Dynatrace LLC
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

"""KTA Dummy Autoscaler (`KTADummyAutoscaler`).

The `KTADummyAutoscaler` is an autoscaler for demonstration and testing purposes. It is deliberately kept simple to
showcase the functionality of KTA.

It toggles between 2 states (off/on encoded as 0/1) where each state corresponds to a configurable number of replicas.
The state is changed based on the toggle threshold _n_, which corresponds to the number of MAPE-K loop iterations
incl. the one that is in progress.

Algorithm details:

- The monitor UDF tracks the number of MAPE-K loop iterations.
- The analyze UDF determines the state (0/1).
- The plan UDF determines the number of replicas based on the toggle state of the analyze UDF result.

Note that the wall clock time between state changes depends on the toggle threshold _n_ AND the reconciliation
time interval of the Kubernetes Operator that is configured in the KTAPolicy. For example, a toggle threshold of
5 and a reconciliation interval of 5s will toggle the state approximately every 5 * 5s = 25s.

To demonstrate how the Python SDK can be used, we provide the `KTADummyAutoscaler` in 2 different versions:

- Combined version where all UDFs are implemented in a single class.
- Separate classes for (1) the monitor (2) the analyze and plan UDF.
"""

import logging
from typing import Any, Dict, List, Literal, Optional, Tuple, Union, override

from kta_python_sdk.common.model import (
    Context,
    Empty,
    PlanUdfResult,
    TopologyNode,
    UdfResult,
)
from kta_python_sdk.core.udf import (
    AutoscalingAlgorithmAnalyzeUDF,
    AutoscalingAlgorithmMonitorUDF,
    AutoscalingAlgorithmPlanUDF,
    AutoscalingAlgorithmUDFs,
)

_logger = logging.getLogger(__name__)


class _KTADummyAutoscalerMonitorResult(UdfResult):
    numInvocations: int


class _KTADummyAutoscalerAnalyzeResult(UdfResult):
    toggleState: Literal[0, 1]


def _monitor(
    ctx: Context[_KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]],
) -> _KTADummyAutoscalerMonitorResult:
    invocations = ctx.resultHistory[0].monitorResult.numInvocations + 1 if ctx.resultHistory else 1

    _logger.debug(f"Number of invocations incl. the current one: {invocations}")

    return _KTADummyAutoscalerMonitorResult(numInvocations=invocations)


def _analyze(
    ctx: Context[_KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]],
    toggle_threshold: int,
) -> Optional[_KTADummyAutoscalerAnalyzeResult]:
    current_toggle_state: Literal[0, 1]
    if ctx.resultHistory:
        assert not isinstance(ctx.resultHistory[0].analyzeResult, Empty)
        current_toggle_state = ctx.resultHistory[0].analyzeResult.toggleState
    else:
        current_toggle_state = 0

    _logger.debug(f"Current toggle state: {current_toggle_state}")

    new_toggle_state: Literal[0, 1]

    assert ctx.monitorResult is not None  # mypy
    if ctx.monitorResult.numInvocations % toggle_threshold == 0:
        _logger.debug("Changing toggle state")
        # due to literal type
        new_toggle_state = 0 if (current_toggle_state + 1) % 2 == 0 else 1
    else:
        new_toggle_state = current_toggle_state

    _logger.debug(f"New toggle state: {new_toggle_state}")

    return _KTADummyAutoscalerAnalyzeResult(toggleState=new_toggle_state)


def _plan(
    ctx: Context[_KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]],
    num_replicas_by_state: Union[List[int], tuple[int, ...]],
) -> Dict[TopologyNode, PlanUdfResult]:
    assert ctx.analyzeResult is not None
    state = ctx.analyzeResult.toggleState
    assert state in (0, 1) and len(num_replicas_by_state) == 2  # mypy
    # support single and multiple scaling targets
    scale_targets = ctx.topology if isinstance(ctx.topology, (list, tuple)) else (ctx.topology,)
    res = {}
    # 1st operator gets `state`, second operator the other of the 2 states, third operators gets
    # again state `state`, etc.
    idx: int
    for idx, s in enumerate(scale_targets):  # type (int, TopologyNode)
        assert isinstance(s, TopologyNode)
        num_replicas = num_replicas_by_state[(state + idx) % 2]
        _logger.debug(f"Desired number of replicas for scale target {s} based on toggle state: {num_replicas}")
        res[s] = PlanUdfResult(parallelism=num_replicas)
    return res


def _validate_init(toggle_threshold: int, states: Union[List[int], tuple[int, ...]]) -> None:
    if not isinstance(toggle_threshold, int):
        raise ValueError(f"Toggle threshold must be of type int. Got: {type(toggle_threshold)}.")

    if toggle_threshold < 1:
        raise ValueError(f"Toggle threshold must be >= 1. Got: {toggle_threshold}")

    if not isinstance(states, list) and not isinstance(states, tuple):
        raise ValueError(f"States must be a list or tuple. Got: {type(states)}")

    if len(states) != 2:
        raise ValueError("States must contain exactly 2 elements")

    for s in states:
        if not isinstance(s, int):
            raise ValueError(f"Entries of states must be of type int. Got: {[type(s) for s in states]}")

        if s <= 0:
            raise ValueError(f"Each entry in state must be at least 1. Got: {states}.")

    if states[0] == states[1]:
        raise ValueError(f"State entries must be different. Got: {states}")


class KTADummyAutoscaler(
    AutoscalingAlgorithmUDFs[
        _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
    ]
):
    """Implements all UDFs."""

    def __init__(
        self, toggle_threshold: int, num_replicas_by_state: Union[List[int], tuple[int, ...]], *args: Any, **kwargs: Any
    ) -> None:
        """Initializes the autoscaler.

        Args:
            toggle_threshold: Toggle threshold.
            num_replicas_by_state: Number of replicas for each state. Must be of length 2.
            *args: Params for super types.
            **kwargs: Params for super types.
        """
        super().__init__(*args, **kwargs)
        _validate_init(toggle_threshold, num_replicas_by_state)
        self.toggle_threshold = toggle_threshold
        self.num_replicas_by_state = num_replicas_by_state

    @override
    def monitor(
        self,
        ctx: Context[
            _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
        ],
    ) -> _KTADummyAutoscalerMonitorResult:
        return _monitor(ctx)

    @override
    def analyze(
        self,
        ctx: Context[
            _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
        ],
    ) -> Optional[_KTADummyAutoscalerAnalyzeResult]:
        return _analyze(ctx, self.toggle_threshold)

    @override
    def plan(
        self,
        ctx: Context[
            _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
        ],
    ) -> Dict[TopologyNode, PlanUdfResult]:
        return _plan(ctx, self.num_replicas_by_state)


class KTADummyAutoscalerMonitor(
    AutoscalingAlgorithmMonitorUDF[
        _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
    ]
):
    """Only implements the monitor UDF."""

    @override
    def monitor(
        self,
        ctx: Context[
            _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
        ],
    ) -> _KTADummyAutoscalerMonitorResult:
        return _monitor(ctx)


class KTADummyAutoscalerAnalyzePlan(
    AutoscalingAlgorithmAnalyzeUDF[
        _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
    ],
    AutoscalingAlgorithmPlanUDF[
        _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
    ],
):
    """Only implements the analyze and plan UDF."""

    def __init__(
        self, toggle_threshold: int, num_replicas_by_state: Union[List[int], Tuple[int]], *args: Any, **kwargs: Any
    ) -> None:
        """Initializes the autoscaler.

        Args:
            toggle_threshold: Toggle threshold.
            num_replicas_by_state: Number of replicas for each state. Must be of length 2.
            *args: Params for super types.
            **kwargs: Params for super types.
        """
        super().__init__(*args, **kwargs)
        _validate_init(toggle_threshold, num_replicas_by_state)
        self.toggle_threshold = toggle_threshold
        self.num_replicas_by_state = num_replicas_by_state

    @override
    def analyze(
        self,
        ctx: Context[
            _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
        ],
    ) -> Optional[_KTADummyAutoscalerAnalyzeResult]:
        return _analyze(ctx, self.toggle_threshold)

    @override
    def plan(
        self,
        ctx: Context[
            _KTADummyAutoscalerMonitorResult, _KTADummyAutoscalerAnalyzeResult, Dict[TopologyNode, PlanUdfResult]
        ],
    ) -> Dict[TopologyNode, PlanUdfResult]:
        return _plan(ctx, self.num_replicas_by_state)
