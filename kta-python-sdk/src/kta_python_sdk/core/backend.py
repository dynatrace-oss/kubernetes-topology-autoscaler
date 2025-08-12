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

"""Backends make autoscaling algorithms accessible to the Kubernetes Operator.

- Backends decouple the interprocess communication logic from the implementation logic of the autoscaling algorithms.
- Backends are agnostic to the autoscaling algorithm.
"""

import logging
from collections.abc import Callable
from copy import deepcopy
from http import HTTPStatus
from types import MappingProxyType
from typing import Any, Optional, Protocol, cast

import uvicorn
from fastapi import APIRouter, FastAPI
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from starlette.responses import JSONResponse

from kta_python_sdk.algorithm import ALGORITHMS
from kta_python_sdk.common.exception import (
    IllegalStateException,
    InternalSDKException,
    KTABaseException,
    NotImplementedException,
    UDFException,
    UDFResponseException,
)
from kta_python_sdk.common.model import (
    Context,
    PlanUdfResult,
    RequestDto,
    TopologyNode,
    UdfResult,
    UdfResultBound,
)
from kta_python_sdk.core._util import ImmutableProxy, request_dto_to_context
from kta_python_sdk.core.udf import (
    AutoscalingAlgorithmAnalyzeUDF,
    AutoscalingAlgorithmMonitorUDF,
    AutoscalingAlgorithmPlanUDF,
    AutoscalingAlgorithmUDFUnion,
)

_logger = logging.getLogger(__name__)


class BackendProtocol(Protocol):
    """Internal Protocol for backends. Methods of this Protocol **must not** be invoked by the user."""

    def set_up(self, value: AutoscalingAlgorithmUDFUnion) -> None:
        """Sets the autoscaling algorithm for a backend.

        Args:
            value: The autoscaling algorithm.

        Returns: `None`

        """
        ...

    def run(self) -> None:
        """Runs the backend with the set autoscaling algorithm.

        Returns: `None`

        """
        ...


class HTTPBackend:
    """HTTPBackend.

    Exposes an autoscaling algorithm via HTTP using FastAPI and the uvicorn webserver.
    This backend requires the installation of the following extras: `http-backend`.

    """

    _default_app_config = {
        "host": "0.0.0.0",
        "port": 8096,
        # avoid cumbersome logging configuration issues and simply allow the user to configure logging
        # via logging.basicConfig()
        "log_config": None,
    }
    _api_prefix = "/api/v1alpha1"

    def __init__(self, **uvicorn_kwargs: Any) -> None:
        """Initializes a new instance.

        Args:
            **uvicorn_kwargs: Arguments passed to [uvicorn.run()](https://www.uvicorn.org/settings/#settings).
        """
        _logger.info("Initializing HTTPBackend")

        self._app = FastAPI(title="Kubernetes Topology Autoscaler (KTA) Python SDK :: HTTPEndpoints")

        self._uvicorn_config = MappingProxyType(self._default_app_config | deepcopy(uvicorn_kwargs))
        self._set_up_exception_handlers()
        self._autoscaling_algorithm: Optional[AutoscalingAlgorithmUDFUnion] = None

    def _set_up_api_routes(self, autoscaling_algorithm: AutoscalingAlgorithmUDFUnion) -> None:
        _logger.info("Set up API routes")

        self.router = APIRouter(prefix=self._api_prefix)

        at_least_one_active_route = False

        if isinstance(autoscaling_algorithm, AutoscalingAlgorithmMonitorUDF):
            self.router.add_api_route("/monitor", endpoint=self._monitor_endpoint, methods=["POST"])
            at_least_one_active_route |= True

        if isinstance(autoscaling_algorithm, AutoscalingAlgorithmAnalyzeUDF):
            self.router.add_api_route("/analyze", endpoint=self._analyze_endpoint, methods=["POST"])
            at_least_one_active_route |= True

        if isinstance(autoscaling_algorithm, AutoscalingAlgorithmPlanUDF):
            self.router.add_api_route("/plan", endpoint=self._plan_endpoint, methods=["POST"])
            at_least_one_active_route |= True

        if not at_least_one_active_route:
            raise UDFException(
                "No route was set active. Does the autoscaling algorithm implement at least one mandatory interface?"
            )

        self._app.include_router(self.router)

    @classmethod
    def _log_exception_and_create_response(cls, exc: Exception, *args: Any, **kwargs: Any) -> JSONResponse:
        _logger.exception(kwargs["content"], exc_info=exc)
        return JSONResponse(*args, **kwargs)

    def _set_up_exception_handlers(self) -> None:
        _logger.info("Set up exception handlers")

        self._app.exception_handler(UDFException)(
            lambda req, exc: self._log_exception_and_create_response(
                exc,
                status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
                content={
                    "message": f"Error in the implementation of the provided UDF on endpoint {req.url}. "
                    + f"Details: {(str(exc))}"
                },
            )
        )
        self._app.exception_handler(NotImplementedException)(
            lambda req, exc: self._log_exception_and_create_response(
                exc,
                status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
                content={"message": f"Exception on endpoint {req.url}. Details: {str(exc)}"},
            )
        )
        self._app.exception_handler(InternalSDKException)(
            lambda req, exc: self._log_exception_and_create_response(
                exc,
                status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
                content={
                    "message": f"Exception raised by endpoint {req.url}. "
                    + f"Please consider reporting this issue to the maintainers. Details: {str(exc)}"
                },
            )
        )
        self._app.exception_handler(KTABaseException)(
            lambda req, exc: self._log_exception_and_create_response(
                exc,
                status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
                content={"message": f"Exception raised by endpoint {req.url}. " + f"Details: {str(exc)}"},
            )
        )
        self._app.exception_handler(RequestValidationError)(
            lambda req, exc: self._log_exception_and_create_response(
                exc,
                status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
                content={"message": f"Invalid client request on endpoint {req.url}. Details: {str(exc)}"},
            )
        )
        # avoids application crash
        self._app.exception_handler(Exception)(
            lambda req, exc: self._log_exception_and_create_response(
                exc,
                status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
                content={
                    "message": f"Uncaught exception at the top level on endpoint {req.url}. "
                    + f"Please consider reporting this issue ot the maintainers. Details: {str(exc)}"
                },
            )
        )

    def _monitor_endpoint(self, req: RequestDto) -> JSONResponse:
        _logger.debug(f"Request: {req}")

        assert (
            self._autoscaling_algorithm is not None
            and hasattr(self._autoscaling_algorithm, "monitor")
            and callable(getattr(self._autoscaling_algorithm, "monitor"))
        )  # mypy

        ctx = request_dto_to_context(req)

        res = self._call_udf_with_safety_net(self._autoscaling_algorithm.monitor, ctx)

        if not isinstance(res, (UdfResult, dict)):
            raise UDFResponseException(
                f"Expected return value of {type(self.unwrap_autoscaling_algorithm)}'s monitor method to be "
                + f"an instance of one of the following: {UdfResultBound}. Got: {type(res)}."
            )

        return JSONResponse(
            status_code=HTTPStatus.OK,
            content=jsonable_encoder(res),
        )

    def _analyze_endpoint(self, req: RequestDto) -> JSONResponse:
        _logger.debug(f"Request: {req}")

        assert (
            self._autoscaling_algorithm is not None
            and hasattr(self._autoscaling_algorithm, "analyze")
            and callable(getattr(self._autoscaling_algorithm, "analyze"))
        )  # mypy

        ctx = request_dto_to_context(req)

        res = self._call_udf_with_safety_net(self._autoscaling_algorithm.analyze, ctx)

        if not isinstance(res, (UdfResult, dict)):
            raise UDFResponseException(
                f"Expected return value of {type(self.unwrap_autoscaling_algorithm)}'s analyze method to be "
                + f"an instance of one of the following: {UdfResultBound}. Got: {type(res)}."
            )

        return JSONResponse(
            status_code=HTTPStatus.OK,
            content=jsonable_encoder(res),
        )

    def _plan_endpoint(self, req: RequestDto) -> JSONResponse:
        _logger.debug(f"Request: {req}")

        assert (
            self._autoscaling_algorithm is not None
            and hasattr(self._autoscaling_algorithm, "plan")
            and callable(getattr(self._autoscaling_algorithm, "plan"))
        )  # mypy

        ctx = request_dto_to_context(req)

        res = self._call_udf_with_safety_net(self._autoscaling_algorithm.plan, ctx)

        should_raise = False

        if isinstance(res, dict):
            for k, v in res.items():
                if not isinstance(k, TopologyNode) or not isinstance(v, PlanUdfResult):
                    should_raise = True
        elif not isinstance(res, PlanUdfResult):
            should_raise = True

        if should_raise:
            raise UDFResponseException(
                f"Expected return value of {type(self.unwrap_autoscaling_algorithm)}'s plan method to be an "
                f"instance of one of the following: {UdfResultBound}. Got: {type(res)}."
            )

        if not isinstance(res, dict):
            if len(req.topology) > 1:
                raise UDFResponseException(
                    f"Expected {type(self.unwrap_autoscaling_algorithm)}'s plan method to return an "
                    f"{type(res)} instance for each scaling target in {req.topology}. Got: {res} (a single "
                    f"instance is only valid if there is a single scaling target)."
                )
            return JSONResponse(
                status_code=HTTPStatus.OK, content=jsonable_encoder({jsonable_encoder(req.topology[0]): res})
            )
        else:
            if res.keys() != set(req.topology):
                raise UDFResponseException(
                    f"Expected {type(self.unwrap_autoscaling_algorithm)}'s plan method to "
                    f"return a dict with exactly one entry for each scale target " + f"({req.topology}). Got: {res}."
                )
            return JSONResponse(
                status_code=HTTPStatus.OK, content=jsonable_encoder({jsonable_encoder(k): v for k, v in res.items()})
            )

    def _call_udf_with_safety_net(self, udf: Callable, ctx: Context) -> Optional[UdfResultBound]:
        try:
            res = udf(ctx)
        except Exception as e:
            if isinstance(self._autoscaling_algorithm, ALGORITHMS):
                raise InternalSDKException(
                    f"Method {udf} of provided algorithm {self.unwrap_autoscaling_algorithm} raised an " + "exception."
                ) from e
            else:
                raise UDFException() from e
        return res

    def set_up(self, value: AutoscalingAlgorithmUDFUnion) -> None:
        """Sets up the backend."""
        if self._autoscaling_algorithm is not None:
            raise IllegalStateException(f"Autoscaling algorithm already set: {self.unwrap_autoscaling_algorithm}.")
        # lazy initialization so we only activate implemented endpoints
        self._set_up_api_routes(value)
        self._autoscaling_algorithm = cast(AutoscalingAlgorithmUDFUnion, ImmutableProxy(value))

    def run(self) -> None:
        """Runs the backend with the set autoscaling algorithm UDF(s)."""
        if self._autoscaling_algorithm is None:
            raise InternalSDKException("Expected autoscaling algorithm to be set. Got: None.")

        uvicorn.run(self._app, **self._uvicorn_config)  # type: ignore[arg-type]

    @property
    def unwrap_autoscaling_algorithm(self) -> AutoscalingAlgorithmUDFUnion:
        """Visible for testing."""
        return self._autoscaling_algorithm.__dict__["_wraps"]
