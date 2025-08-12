import argparse
import logging

from kta_python_sdk.algorithm import KTADummyAutoscaler
from kta_python_sdk.core.backend import HTTPBackend
from kta_python_sdk.core.bootstrap import run

_logger = logging.getLogger(__name__)


def parse_arguments():
    parser = argparse.ArgumentParser(description="KTA Python SDK Quickstart")
    parser.add_argument("--toggle-threshold", type=int, default=2, help="Toggle threshold")
    parser.add_argument(
        "--num-replicas-by-state", nargs=2, type=int, default=(4, 2), help="Number of replicas by state"
    )
    return parser.parse_args()


if __name__ == "__main__":
    # Log level must be explicitly configured by the user
    logging.basicConfig(level=logging.DEBUG)

    # Parameters of algorithm are customizable via CLI
    args = parse_arguments()
    _logger.info(f"Arguments: {args}")

    # Here is where the magic happens. Just create an instance of your autoscaling algorithm class and the backend
    # and call run()
    autoscaler = KTADummyAutoscaler(args.toggle_threshold, args.num_replicas_by_state)
    backend = HTTPBackend()
    run(autoscaler, backend)
