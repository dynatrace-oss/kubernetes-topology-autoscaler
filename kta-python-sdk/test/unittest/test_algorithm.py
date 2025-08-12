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

from unittest.mock import Mock, PropertyMock

import pytest

from kta_python_sdk.algorithm.dummy import KTADummyAutoscaler
from kta_python_sdk.common.model import Context, PlanUdfResult, Result, TopologyNode, TopologyNodeFields, UdfResult
from test.pytest_utils import does_not_raise

generic_autoscaler_tests_fixtures = ("autoscaler", [KTADummyAutoscaler(1, (10, 20))])


@pytest.mark.parametrize(*generic_autoscaler_tests_fixtures)
def test_monitor_only_accesses_expected_attributes(autoscaler):
    ctx_mock = Mock(spec=Context)
    property_mock = PropertyMock()
    type(ctx_mock).monitorResult = property_mock
    type(ctx_mock).analyzeResult = property_mock
    ctx_mock.resultHistory = []
    autoscaler.monitor(ctx_mock)
    assert len(property_mock.mock_calls) == 0


@pytest.mark.parametrize(*generic_autoscaler_tests_fixtures)
def test_analyze_only_accesses_expected_attributes(autoscaler):
    ctx_mock = Mock(spec=Context)
    monitor_property_mock = PropertyMock()
    type(ctx_mock).monitorResult = monitor_property_mock
    analyze_property_mock = PropertyMock()
    type(ctx_mock).analyzeResult = analyze_property_mock
    ctx_mock.resultHistory = []
    autoscaler.analyze(ctx_mock)
    assert len(analyze_property_mock.mock_calls) == 0


@pytest.mark.parametrize(
    ("toggle_threshold", "num_replicas_by_state"), [(1, (10, 20)), (1, (20, 10)), (7, (10, 20)), (7, (20, 10))]
)
def test_dummy_autoscaler_init_with_valid_arguments_does_not_raise_value_error(toggle_threshold, num_replicas_by_state):
    with does_not_raise(ValueError):
        KTADummyAutoscaler(toggle_threshold, num_replicas_by_state)


@pytest.mark.parametrize(
    ("toggle_threshold", "num_replicas_by_state"),
    [
        (-1, tuple()),
        (0, tuple()),
        (1, tuple()),
        (-1, (10,)),
        (0, (10,)),
        (-1, (10, -1)),
        (-1, (10, 20)),
        (0, (-1, 20)),
        (0, (10, 20)),
        (1, (10, -1)),
        (1, (10, 0)),
        (1, (0, 20)),
        (1, (10, 10)),
    ],
)
def test_dummy_autoscaler_init_with_invalid_arguments_raises_value_error(toggle_threshold, num_replicas_by_state):
    with pytest.raises(ValueError):
        KTADummyAutoscaler(toggle_threshold, num_replicas_by_state)


def test_dummy_autoscaler_monitor():
    ctx_mock = Mock(spec=Context)
    ctx_mock.resultHistory = []
    dummy_auto_scaler = KTADummyAutoscaler(1, (10, 20))
    res = dummy_auto_scaler.monitor(ctx_mock)
    assert isinstance(res, UdfResult)
    assert res.numInvocations == 1

    ctx_mock = Mock(spec=Context)
    result_mock = Mock(spec=Result)
    result_mock.monitorResult = Mock(spec=UdfResult, numInvocations=5)
    ctx_mock.resultHistory = [result_mock]
    dummy_auto_scaler = KTADummyAutoscaler(1, (10, 20))
    res = dummy_auto_scaler.monitor(ctx_mock)
    assert isinstance(res, UdfResult)
    assert res.numInvocations == 6


def test_dummy_autoscaler_analyze():
    ctx_mock = Mock(spec=Context)
    ctx_mock.resultHistory = []
    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=5)
    dummy_auto_scaler = KTADummyAutoscaler(1, (10, 20))
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert isinstance(res, UdfResult)
    assert res.toggleState == 1

    ctx_mock = Mock(spec=Context)
    result_history_mock = Mock(spec=UdfResult, toggleState=1)
    result_history_mock.analyzeResult = result_history_mock
    ctx_mock.resultHistory = [result_history_mock]
    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=5)
    dummy_auto_scaler = KTADummyAutoscaler(1, (10, 20))
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert isinstance(res, UdfResult)
    assert res.toggleState == 0

    ctx_mock = Mock(spec=Context)
    result_history_mock = Mock(spec=UdfResult, toggleState=0)
    result_history_mock.analyzeResult = result_history_mock
    ctx_mock.resultHistory = [result_history_mock]
    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=5)
    dummy_auto_scaler = KTADummyAutoscaler(1, (10, 20))
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert isinstance(res, UdfResult)
    assert res.toggleState == 1

    dummy_auto_scaler = KTADummyAutoscaler(3, (10, 20))
    ctx_mock = Mock(spec=Context)
    result_history_mock = Mock(spec=UdfResult, toggleState=1)
    result_history_mock.analyzeResult = result_history_mock
    ctx_mock.resultHistory = [result_history_mock]

    # toggle state should NOT change
    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=2)
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert res.toggleState == 1

    # toggle state should change
    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=3)
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert res.toggleState == 0

    # toggle state should NOT change
    result_history_mock = Mock(spec=UdfResult, toggleState=0)
    result_history_mock.analyzeResult = result_history_mock
    ctx_mock.resultHistory = [result_history_mock]

    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=4)
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert res.toggleState == 0

    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=5)
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert res.toggleState == 0

    # toggle state should change again
    ctx_mock.monitorResult = Mock(spec=UdfResult, numInvocations=6)
    res = dummy_auto_scaler.analyze(ctx_mock)
    assert res.toggleState == 1


def test_dummy_autoscaler_plan():
    ctx_mock = Mock(spec=Context)
    topology_node_1 = TopologyNode(
        type="flinkStreamingGraphNode", fields=TopologyNodeFields(id="topology-node-1", kind=None, name=None)
    )
    ctx_mock.topology = [topology_node_1]
    analyze_result_mock = Mock(spec=UdfResult, toggleState=0)
    ctx_mock.analyzeResult = analyze_result_mock
    dummy_auto_scaler = KTADummyAutoscaler(1, (10, 20))
    res = dummy_auto_scaler.plan(ctx_mock)
    assert isinstance(res, dict)
    assert len(res) == 1
    assert (
        topology_node_1 in res.keys()
        and isinstance(res[topology_node_1], PlanUdfResult)
        and res[topology_node_1].parallelism == 10
    )

    ctx_mock = Mock(spec=Context)
    topology_node_1 = TopologyNode(
        type="flinkStreamingGraphNode", fields=TopologyNodeFields(id="topology-node-1", kind=None, name=None)
    )
    topology_node_2 = TopologyNode(
        type="flinkStreamingGraphNode", fields=TopologyNodeFields(id="topology-node-2", kind=None, name=None)
    )
    topology_node_3 = TopologyNode(
        type="flinkStreamingGraphNode", fields=TopologyNodeFields(id="topology-node-3", kind=None, name=None)
    )
    topology_node_4 = TopologyNode(
        type="flinkStreamingGraphNode", fields=TopologyNodeFields(id="topology-node-4", kind=None, name=None)
    )
    ctx_mock.topology = [topology_node_1, topology_node_2, topology_node_3, topology_node_4]
    analyze_result_mock = Mock(spec=UdfResult, toggleState=1)
    ctx_mock.analyzeResult = analyze_result_mock
    dummy_auto_scaler = KTADummyAutoscaler(1, (10, 20))
    res = dummy_auto_scaler.plan(ctx_mock)
    assert isinstance(res, dict)
    assert len(res) == 4
    for s in ctx_mock.topology:
        assert s in res.keys()
        assert isinstance(res[s], PlanUdfResult)
    assert res[topology_node_1].parallelism == 20
    assert res[topology_node_2].parallelism == 10
    assert res[topology_node_3].parallelism == 20
    assert res[topology_node_4].parallelism == 10
