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

import json
from http import HTTPStatus
from typing import Dict
from unittest import mock
from unittest.mock import MagicMock, Mock

import pytest
from fastapi.encoders import jsonable_encoder
from fastapi.testclient import TestClient
from httpx import HTTPStatusError

from kta_python_sdk.common.exception import IllegalStateException, InternalSDKException, UDFException
from kta_python_sdk.common.model import (
    Context,
    Empty,
    PlanUdfResult,
    RequestDto,
    Result,
    TopologyNode,
    TopologyNodeFields,
    UdfResult,
)
from kta_python_sdk.core import bootstrap
from kta_python_sdk.core._util import request_dto_to_context
from kta_python_sdk.core.backend import BackendProtocol, HTTPBackend
from kta_python_sdk.core.udf import AutoscalingAlgorithmMonitorUDF, AutoscalingAlgorithmUDFs
from test.pytest_utils import does_not_raise

API_PREFIX = "/api/v1alpha1"


class _TestException(Exception):
    pass


class _TestMonitorResult(UdfResult):
    value: str


class _TestAnalyzeResult(UdfResult):
    value: str


class _TestAlgorithmValid(AutoscalingAlgorithmUDFs):
    def monitor(self, _: Context):
        return _TestMonitorResult(value="monitor")

    def analyze(self, _: Context):
        return _TestAnalyzeResult(value="analyze")

    def plan(self, context: Context):
        if isinstance(context.topology, TopologyNode):
            return PlanUdfResult(parallelism=5)
        else:
            res = {}
            for s in context.topology:
                res[s] = PlanUdfResult(parallelism=5)
            return res


class _TestAlgorithmValidMonitorOnly(AutoscalingAlgorithmMonitorUDF):
    def monitor(self, _: Context):
        return _TestMonitorResult(value="monitor")


class _TestAlgorithmInvalid(AutoscalingAlgorithmUDFs):
    def monitor(self, _: Context):
        return None

    def analyze(self, _: Context):
        return set()

    def plan(self, _: Context):
        return None


def _decode_json_response_body(json_response) -> Dict:
    return json.loads(json_response.body.decode("utf-8"))


def request_fixture(*, num_topology_nodes=1, monitor_res=Empty(), analyze_res=Empty(), history_len=0):
    topology_nodes = [
        TopologyNode(
            type="flinkStreamingGraphNode", fields=TopologyNodeFields(id=f"topology-node-{i}", kind=None, name=None)
        )
        for i in range(num_topology_nodes)
    ]

    result_history = [
        Result(
            id=f"{i + 1}",
            udfStartTimestampMillis=5000 * i,
            udfEndTimestampMillis=6000 * i,
            monitorResult=UdfResult(),
            analyzeResult=UdfResult(),
            planResult=PlanUdfResult(parallelism=1),
            parallelism={t: 1 for t in topology_nodes},
        )
        for i in range(history_len)
    ]
    return RequestDto(
        id=f"{history_len + 1}",
        udfStartTimestampMillis=5000 * (history_len + 1),
        topology=topology_nodes,
        monitorResult=monitor_res,
        analyzeResult=analyze_res,
        resultHistory=result_history,
    )


def test_bootstrap_run_calls_only_expected_methods():
    """Methods in backend protocol should be called."""
    autoscaling_algorithm_mock = Mock(spec=AutoscalingAlgorithmUDFs)
    backend_mock = Mock(spec=BackendProtocol)
    bootstrap.run(autoscaling_algorithm_mock, backend_mock)
    backend_mock.set_up.assert_called()
    backend_mock.run.asset_called()


def test_request_to_context():
    """TopologyNode tuples of length 1 should be unpacked. TopologyNode tuples of length 2 should be untouched."""
    req = request_fixture(history_len=1)
    res = request_dto_to_context(req)
    assert isinstance(res.topology, TopologyNode)

    req = request_fixture(num_topology_nodes=2, history_len=1)
    res = request_dto_to_context(req)
    assert len(res.topology) == 2


def test_http_backend_run_throws_exception_if_autoscaling_algorithm_is_not_set():
    http_backend = HTTPBackend()
    with pytest.raises(InternalSDKException):
        http_backend.run()


def test_http_backend_set_up():
    http_backend = HTTPBackend()
    assert http_backend._autoscaling_algorithm is None
    autoscaling_algorithm_mock = MagicMock(spec=AutoscalingAlgorithmUDFs)
    http_backend.set_up(autoscaling_algorithm_mock)
    assert http_backend.unwrap_autoscaling_algorithm == autoscaling_algorithm_mock


def test_http_backend_calling_set_up_twice_throws_illegal_state_exception():
    http_backend = HTTPBackend()
    assert http_backend._autoscaling_algorithm is None
    autoscaling_algorithm_mock = Mock(spec=AutoscalingAlgorithmUDFs)
    http_backend.set_up(autoscaling_algorithm_mock)
    assert http_backend._autoscaling_algorithm is not None
    with pytest.raises(IllegalStateException):
        http_backend.set_up(autoscaling_algorithm_mock)


def test_http_backend_endpoint_implementations_invokes_autoscaling_algorithm_and_returns_correct_result():
    autoscaling_algorithm = _TestAlgorithmValid()

    with mock.patch.object(HTTPBackend, "run"):
        http_backend = HTTPBackend()
        http_backend.set_up(autoscaling_algorithm)
        http_backend.run()

        req = request_fixture()
        monitor_res = _decode_json_response_body(http_backend._monitor_endpoint(req))
        assert monitor_res["value"] == "monitor"

        req = request_fixture(monitor_res=monitor_res)
        analyze_res = _decode_json_response_body(http_backend._analyze_endpoint(req))
        assert analyze_res["value"] == "analyze"

        req = request_fixture(monitor_res=monitor_res, analyze_res=analyze_res)

        # single topology node
        res = http_backend._plan_endpoint(req)
        plan_res_single_topology_node = _decode_json_response_body(res)
        assert len(plan_res_single_topology_node) == 1
        assert all(list(map(lambda x: x == {"parallelism": 5}, list(plan_res_single_topology_node.values()))))

        # multiple topology nodes
        req = request_fixture(num_topology_nodes=2, monitor_res=monitor_res, analyze_res=analyze_res)
        res = http_backend._plan_endpoint(req)
        plan_res_multiple_topology_nodes = _decode_json_response_body(res)
        assert len(plan_res_multiple_topology_nodes) == 2
        assert all(list(map(lambda x: x == {"parallelism": 5}, list(plan_res_multiple_topology_nodes.values()))))


def test_http_backend_endpoint_implementations_sanitize_udf_result():
    autoscaling_algorithm = _TestAlgorithmInvalid()

    with mock.patch.object(HTTPBackend, "run"):
        http_backend = HTTPBackend()
        http_backend.set_up(autoscaling_algorithm)
        http_backend.run()

        req = request_fixture()
        with pytest.raises(UDFException):
            http_backend._monitor_endpoint(req)

        req = request_fixture(monitor_res=UdfResult())
        with pytest.raises(UDFException):
            http_backend._analyze_endpoint(req)

        req = request_fixture(monitor_res=UdfResult(), analyze_res=UdfResult())
        with pytest.raises(UDFException):
            http_backend._plan_endpoint(req)


@pytest.fixture
def fast_api_fixture(algorithm=None):
    if algorithm is None:
        algorithm = _TestAlgorithmValid()
    with mock.patch.object(HTTPBackend, "run"):
        backend = HTTPBackend()
        autoscaling_algorithm = algorithm
        backend.set_up(autoscaling_algorithm)
        backend.run()
    return TestClient(backend._app), autoscaling_algorithm


def test_http_backend_monitor_api_valid_request(fast_api_fixture):
    client, algorithm = fast_api_fixture
    req = request_fixture()
    response = client.post(f"{API_PREFIX}/monitor", json=jsonable_encoder(req))
    with does_not_raise(HTTPStatusError):
        response.raise_for_status()
    assert response.json() is not None and len(response.json()) != 0


def test_http_backend_analyze_api_valid_request(fast_api_fixture):
    client, algorithm = fast_api_fixture
    req = request_fixture(monitor_res=algorithm.monitor(Mock(spec=_TestMonitorResult)))
    json_req = jsonable_encoder(req)
    response = client.post(f"{API_PREFIX}/analyze", json=json_req)
    with does_not_raise(HTTPStatusError):
        response.raise_for_status()
    assert response.json() is not None and len(response.json()) != 0


def test_http_backend_plan_api_valid_request(fast_api_fixture):
    client, algorithm = fast_api_fixture
    req = request_fixture(
        monitor_res=algorithm.monitor(Mock(spec=_TestMonitorResult)),
        analyze_res=algorithm.analyze(Mock(spec=_TestAnalyzeResult)),
    )
    response = client.post(f"{API_PREFIX}/plan", json=jsonable_encoder(req))
    with does_not_raise(HTTPStatusError):
        response.raise_for_status()
    assert response.json() is not None and len(response.json()) != 0


def test_http_backend_catches_udf_exception(fast_api_fixture):
    client, algorithm = fast_api_fixture
    mock = Mock()
    mock.side_effect = _TestException()
    algorithm.monitor = mock
    req = request_fixture()
    response = client.post(f"{API_PREFIX}/monitor", json=jsonable_encoder(req))
    with pytest.raises(HTTPStatusError):
        response.raise_for_status()
    assert "message" in response.json()


def test_http_backend_invalid_request_returns_status_unprocessable_entity(fast_api_fixture):
    client, algorithm = fast_api_fixture
    invalid_request = {"this-is-an-invalid-field": "with-an-invalid-value"}

    response = client.post(f"{API_PREFIX}/monitor", json=jsonable_encoder(invalid_request))
    with pytest.raises(HTTPStatusError):
        response.raise_for_status()
    assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY

    response = client.post(f"{API_PREFIX}/analyze", json=jsonable_encoder(invalid_request))
    with pytest.raises(HTTPStatusError):
        response.raise_for_status()
    assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY

    response = client.post(f"{API_PREFIX}/plan", json=jsonable_encoder(invalid_request))
    with pytest.raises(HTTPStatusError):
        response.raise_for_status()
    assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY


def test_http_backend_non_existing_endpoint_returns_status_not_found(fast_api_fixture):
    client, algorithm = fast_api_fixture
    req = request_fixture()
    response = client.post(f"{API_PREFIX}/definitely-not-existing-endpoint", json=jsonable_encoder(req))
    with pytest.raises(HTTPStatusError):
        response.raise_for_status()
    assert response.status_code == HTTPStatus.NOT_FOUND
