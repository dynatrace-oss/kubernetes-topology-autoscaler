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

"""Utilities."""

import copy
from typing import Any, override

from kta_python_sdk.common.exception import IllegalAccessException
from kta_python_sdk.common.model import Context, RequestDto


class ImmutableProxy:
    """Recursively checks if objects are mutated in a best effort manner to avoid severe user errors."""

    # TODO: maybe recursive immutability is too restrictive for some third-party libraries (e.g., scikit?)
    # TODO: add other dunder methods that don't modify by contract (e.g., __len__)?

    def __init__(self, wraps: Any):
        self.__dict__["_wraps"] = wraps

    def __getattr__(self, item: Any) -> Any:
        _wraps = self.__dict__["_wraps"]
        return ImmutableProxy(getattr(_wraps, item))

    @override
    def __setattr__(self, key: Any, value: Any) -> None:
        raise IllegalAccessException(
            f"Attempt to modify immutable object. Details: Attempt to set attribute {key} " + f"with value {value}."
        )

    def __call__(self, *args: Any, **kwargs: Any) -> Any:
        try:
            instance = self._wraps.__self__
            old_state = copy.deepcopy(instance.__dict__)
            res = self._wraps(*args, **kwargs)
            new_state = self._wraps.__self__.__dict__
        except Exception as e:
            raise IllegalAccessException(
                f"{self.__dict__['_wraps']} raised an exception. Check the information above for the root cause ^^^"
            ) from e
        if old_state != new_state:
            raise IllegalAccessException(
                "Attempt to modify immutable object. Details: Attempt to mutate state "
                + f"with a method or function call. Details: {self._wraps}, old state: "
                + f"{old_state}, new state: {new_state}."
            )
        return res


def request_dto_to_context(req: RequestDto) -> Context:
    """Converts a `RequestDto` instance to a `Context` instance.

    Args:
        req: The request DTO instance.

    Returns:
        The context instance.
    """
    req_data = req.model_dump()

    req_data["topology"] = _unpack_if_len_1(req.topology)

    return Context(**req_data)


def _unpack_if_len_1(obj: Any) -> Any:
    return obj if len(obj) > 1 else obj[0]
