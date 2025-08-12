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

"""Interfaces to implement autoscaling algorithms via _stateless_ user-defined functions (UDFs).

Autoscaling algorithms consist of up to 3 steps as part of the MAPE-K loop: Monitor, Analyze (optional), Plan.
The Execute and Knowledge step are handled by the Kubernetes Operator,
Each of algorithm step (Monitor, Analyze, Plan) maps to exactly one UDF.
We provide individual interfaces for each step as well as a combined interface.

Implementing steps with individual interfaces is useful in cases where at least one UDF

- requires specialized hardware (e.g., a GPU for algorithms that use deep learning models) and should be scheduled
on a specific node.
- should run in a separate pod (for any reason).
- needs resources that cannot be accessed from inside the cluster.
- should serve multiple clusters and is deployed in a central environment.
"""

from abc import ABC, abstractmethod
from typing import Optional, Union

from kta_python_sdk.common.model import (
    Context,
    UdfPlanResultBound,
    UdfResultBound,
)


class AutoscalingAlgorithmUDFMeta(ABC):
    """Base class for all autoscaling algorithm UDFs.

    Subtypes of this class may only contain attributes, regular methods and the __call__  method
    (but _no_ other 'dunder' methods).
    All methods in the class (except __init__) have to be stateless.

    Warning:
        It is the responsibility of the user implementing the UDFs to ensure statelessness. However, provided
        backends will check if the implemented functions are stateless in a best effort manner.
    """

    pass


class AutoscalingAlgorithmMonitorUDF[MONITOR: UdfResultBound, ANALYZE: UdfResultBound, PLAN: UdfPlanResultBound](
    AutoscalingAlgorithmUDFMeta
):
    """Interface for the monitor step of the MAPE-K loop."""

    def __init__(self, *args, **kwargs) -> None:  # type: ignore[no-untyped-def]
        super().__init__(*args, **kwargs)

    @abstractmethod
    def monitor(self, ctx: Context[MONITOR, ANALYZE, PLAN]) -> MONITOR:
        """The monitor step.

        Args:
            ctx: Current MAPE-K evaluation loop context.

        Returns: Result of the monitor step.

        """
        ...


class AutoscalingAlgorithmAnalyzeUDF[MONITOR: UdfResultBound, ANALYZE: UdfResultBound, PLAN: UdfPlanResultBound](
    AutoscalingAlgorithmUDFMeta
):
    """Interface for the analyze step of the MAPE-K loop."""

    def __init__(self, *args, **kwargs) -> None:  # type: ignore[no-untyped-def]
        super().__init__(*args, **kwargs)

    @abstractmethod
    def analyze(self, ctx: Context[MONITOR, ANALYZE, PLAN]) -> Optional[ANALYZE]:
        """The analyze step.

        Args:
            ctx: Current MAPE-K evaluation loop context.

        Returns: Result of the analyze step or `None`

        """
        ...


class AutoscalingAlgorithmPlanUDF[MONITOR: UdfResultBound, ANALYZE: UdfResultBound, PLAN: UdfPlanResultBound](
    AutoscalingAlgorithmUDFMeta
):
    """Interface for the plan step of the MAPE-K loop."""

    def __init__(self, *args, **kwargs) -> None:  # type: ignore[no-untyped-def]
        super().__init__(*args, **kwargs)

    @abstractmethod
    def plan(self, ctx: Context[MONITOR, ANALYZE, PLAN]) -> PLAN:
        """The plan step.

        Args:
            ctx: Current MAPE-K evaluation loop context.

        Returns: Result of the plan step.

        """
        ...


class AutoscalingAlgorithmUDFs[MONITOR: UdfResultBound, ANALYZE: UdfResultBound, PLAN: UdfPlanResultBound](
    AutoscalingAlgorithmMonitorUDF[MONITOR, ANALYZE, PLAN],
    AutoscalingAlgorithmAnalyzeUDF[MONITOR, ANALYZE, PLAN],
    AutoscalingAlgorithmPlanUDF[MONITOR, ANALYZE, PLAN],
    AutoscalingAlgorithmUDFMeta,
    ABC,
):
    """Combined interface for all steps (monitor, analyze, plan) of the MAPE-K loop."""

    pass


AutoscalingAlgorithmUDFUnion = Union[
    AutoscalingAlgorithmMonitorUDF,
    AutoscalingAlgorithmAnalyzeUDF,
    AutoscalingAlgorithmPlanUDF,
    AutoscalingAlgorithmUDFs,
]
