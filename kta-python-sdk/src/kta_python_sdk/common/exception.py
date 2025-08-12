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

"""SDK exceptions. In the following, "user" refers to the application using the SDK."""


class KTABaseException(Exception):
    """Common base class for all SDK exceptions."""

    def __init__(self, *args, **kwargs):  # type: ignore[no-untyped-def]
        super().__init__(args, kwargs)


class InternalSDKException(KTABaseException):
    """Raised if there is an error that was _not_ caused by the user."""

    pass


class IllegalAccessException(KTABaseException):
    """Raised if the user accesses an attribute, method, etc. that the user is not supposed to access.
    This might indicate that the implementation of a UDF is not stateless.
    """


class NotImplementedException(KTABaseException):
    """Raised if the control flow reaches a method that is not implemented yet."""

    pass


class IllegalStateException(KTABaseException):
    """Raised if the application reaches an illegal state and the illegal state was caused by the user."""

    pass


class UDFException(KTABaseException):
    """Raised if the provided autoscaling algorithm UDF behaves in an unexpected way."""

    pass


class UDFResponseException(UDFException):
    """Raised if the return value of a UDF is erroneous (e.g., of an unexpected type)."""

    pass
