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

"""Functions for application bootstrapping."""

from kta_python_sdk.core.backend import BackendProtocol
from kta_python_sdk.core.udf import AutoscalingAlgorithmUDFUnion


def run(
    autoscaling_algorithm: AutoscalingAlgorithmUDFUnion,
    backend: BackendProtocol,
) -> None:
    """Entry point.

    Args:
        autoscaling_algorithm: The autoscaling algorithm.
        backend: The backend.

    Returns: `None`

    """
    backend.set_up(autoscaling_algorithm)
    backend.run()
