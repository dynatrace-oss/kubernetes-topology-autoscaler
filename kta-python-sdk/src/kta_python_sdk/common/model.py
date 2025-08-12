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

"""Record classes (using [Pydantic](https://docs.pydantic.dev/latest/))."""

import json
from collections import namedtuple
from dataclasses import field
from typing import Annotated, Any, Dict, List, Optional, Union, final

from annotated_types import MinLen
from pydantic import BaseModel, PositiveInt, field_validator, model_serializer, model_validator


class UdfResult(BaseModel):
    """Result base class for user-defined results of autoscaling algorithm UDFs."""

    # Allow extra values because the structure is user-defined
    model_config = {"frozen": True, "extra": "allow"}


class Empty(UdfResult):
    """The empty type."""

    model_config = {"extra": "forbid"}


"""
Fields for topology nodes.

Depending on the KTA Scale Driver, different fields might be set.

- Flink Scale Driver: id
- Generic Kubernetes Scale Driver: kind, name
"""
TopologyNodeFields = namedtuple("TopologyNodeFields", "id kind name")


@final
class TopologyNode(BaseModel):
    """Topology node."""

    model_config = {"frozen": True}

    type: str
    fields: TopologyNodeFields

    @model_serializer
    def _serialize(self) -> str:
        """Custom serializer.

        When using instances of this class as a key in a dict, they need be encoded manually as a string because it
        is only known by the invoking context if the object is used as a key or not.
        """
        serialized = {"type": self.type, "fields": ""}
        if self.type == "flinkStreamingGraphNode":
            serialized["fields"] += f"id={self.fields.id}"
        elif self.type == "scaleTargetRef":
            serialized["fields"] += f"kind={self.fields.kind}|name={self.fields.name}"
        return json.dumps(serialized)

    @model_validator(mode="before")
    @classmethod
    def _deserialize(cls, data: Any) -> Any:
        """Custom deserializer.

        The custom deserializer is needed because instances of this class can also be used as key (encoded as string) in
        a JSON object.
        """
        if isinstance(data, str):
            maybe_dict = json.loads(data)
            return (
                cls._deserialize_fields(maybe_dict)
                if isinstance(maybe_dict, dict)
                else cls._deserialize_fields(json.loads(maybe_dict))
            )
        elif isinstance(data, dict):
            if isinstance(data["fields"], str):
                return cls._deserialize_fields(data)
            return data
        assert False, f"Unknown data type {data}"

    @classmethod
    def _deserialize_fields(cls, d: Dict[Any, Any]) -> Dict[Any, Any]:
        kv_pairs = [f.split("=") for f in d["fields"].split("|")]
        fields = {k: v for (k, v) in kv_pairs}
        return d | {
            "fields": TopologyNodeFields(
                id=fields.get("id", None), kind=fields.get("kind", None), name=fields.get("name", None)
            )
        }


@final
class PlanUdfResult(UdfResult):
    """Result of the Plan step."""

    model_config = {"frozen": True}

    parallelism: PositiveInt


UdfResultBound = Union[
    UdfResult,
    # if the user does not want to use Pydantic for the intermediate results at all
    Dict[Any, Any],
]

UdfPlanResultBound = Union[
    PlanUdfResult,
    Dict[TopologyNode, PlanUdfResult],
]


@final
class Result[MONITOR: UdfResultBound, ANALYZE: UdfResultBound, PLAN: UdfPlanResultBound](BaseModel):
    """Result."""

    model_config = {"frozen": True}

    id: str
    udfStartTimestampMillis: int
    udfEndTimestampMillis: int
    monitorResult: MONITOR
    analyzeResult: ANALYZE | Empty = field(default_factory=Empty)
    planResult: PLAN
    parallelism: Dict[TopologyNode, int]


@final
class Context[MONITOR: UdfResultBound, ANALYZE: UdfResultBound, PLAN: UdfPlanResultBound](BaseModel):
    """Context passed to autoscaling algorithm UDFs.

    The context contains intermediate results of the current MAPE-K loop evaluation (e.g., in the analyze step,
    the result of the monitor step is available) and also the result history of successfully completed
    preceding MAPE-K loop evaluations (up to the specified limit in the KTAPolicy).
    """

    model_config = {"frozen": True}

    id: str
    udfStartTimestampMillis: int
    # topologically sorted; if there is only a single node in the topology, it is automatically unpacked by the backend
    topology: Union[TopologyNode | Annotated[tuple[TopologyNode, ...], MinLen(2)]]

    monitorResult: Optional[MONITOR] = None
    analyzeResult: Optional[ANALYZE] = None

    resultHistory: List[Result[MONITOR, ANALYZE, PLAN]] = field(default_factory=lambda: [])


@final
class RequestDto[MONITOR: UdfResultBound, ANALYZE: UdfResultBound, PLAN: UdfPlanResultBound](BaseModel):
    """Request DTO.

    `RequestDto` instances decouple the internal interface between the Kubernetes Operator and SDK backends from the
    SDK user interface (UDFs). A `Request` is automatically converted to a `Context` object by the used backend.
    """

    model_config = {"frozen": True}

    id: str
    udfStartTimestampMillis: int
    topology: List[TopologyNode]
    # although monitor is mandatory, it will empty upon invoking the monitor UDF itself
    monitorResult: MONITOR | Empty = field(default_factory=Empty)
    analyzeResult: ANALYZE | Empty = field(default_factory=Empty)

    resultHistory: List[Result[MONITOR, ANALYZE, PLAN]] = field(default_factory=lambda: [])

    @field_validator("topology", mode="before")
    @classmethod
    def topology_validator(cls, value: List[TopologyNode]) -> List[TopologyNode]:
        """Topology validator."""
        if len(value) < 1:
            raise ValueError("'topology' must contain at least one item.")
        return value
