import logging
import sys
from typing import Callable
from typing import List

import defopt

from seshatio.tools.hello import hello

_tools: List[Callable] = [hello]


def setup_logging(level: str = "INFO") -> None:
    """Set up basic logging to print to the console."""
    fmt = "%(asctime)s %(name)s:%(funcName)s:%(lineno)s [%(levelname)s]: %(message)s"
    handler = logging.StreamHandler()
    handler.setLevel(level)
    handler.setFormatter(logging.Formatter(fmt))

    logger = logging.getLogger("seshatio")
    logger.setLevel(level)
    logger.addHandler(handler)


def run() -> None:
    """Set up logging, then hand over to defopt for running command line tools."""
    setup_logging()
    logger = logging.getLogger("seshatio")
    logger.info("Executing: " + " ".join(sys.argv))
    defopt.run(
        funcs=_tools,
        argv=sys.argv[1:],
    )
    logger.info("Finished executing successfully.")
