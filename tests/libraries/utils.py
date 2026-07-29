#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

from contextlib import contextmanager
from collections.abc import Callable
import logging
import time
from typing import Any, Generator, List, Tuple

import allure
import difflib
import xml.dom.minidom


log = logging.getLogger(__name__)


class DeferredLogHandler(logging.Handler):
    """Stores log records in memory instead of writing them immediately."""

    def __init__(self):
        """Initialize the deferred handler with an empty record list."""
        super().__init__()
        self.records = []

    def emit(self, record: logging.LogRecord):
        """Store an emitted log record in local buffer.

        Args:
            record (logging.LogRecord): Log record to be stored.

        Returns:
            None
        """
        self.records.append(record)

    def flush_to_target(self, target_logger: logging.Logger):
        """Replay buffered log records to the target logger.

        Args:
            target_logger (logging.Logger): Logger to which the buffered records
                should be sent.

        Returns:
            None
        """
        for record in self.records:
            target_logger.handle(record)


@contextmanager
def deferred_logging() -> Generator["DeferredLogHandler", None, None]:
    """Context manager for temporary log buffering.

    This replaces the root logger's handlers with a temporary buffer. Logs
    generated within this context are stored in memory and can be either flushed
    to the original handlers or discarded. Usefull for functions which produces
    a lot of log records and it is not known in advanced if those log
    entries will be kept or discarded (e.g. wait until functions).

    Args:
        None

    Yields:
        DeferredLogHandler: Temporary handler for storing buffered logs.
    """
    root_logger = logging.getLogger()
    original_handlers = root_logger.handlers[:]
    buffer_handler = DeferredLogHandler()
    root_logger.handlers = [buffer_handler]
    try:
        yield buffer_handler
    finally:
        root_logger.handlers = original_handlers


@contextmanager
def report_known_bug_on_failure(bug_id: str):
    """If the wrapped code fails, it enriches the failure report with the bug URL
    and fails the test normally. If it passes, it does nothing.

    Args:
        bug_id (str): Known bug identifier from opendaylight jira or bugzilla

    Yields:
        None
    """
    try:
        yield
    except AssertionError as e:
        url = (
            f"https://jira.opendaylight.org/browse/{bug_id}"
            if "-" in bug_id
            else f"https://bugs.opendaylight.org/show_bug.cgi?id={bug_id_str}"
        )
        allure.dynamic.link(url, name=f"Related Bug {bug_id}")
        error_msg = f"\nThis test failed due to a previously reported bug: {url}\nOriginal error: {str(e)}"

        raise AssertionError(error_msg) from e


def wait_until_function_returns_value_with_custom_value_validator(
    retry_count: int,
    interval: int,
    return_value_validator: Callable,
    function: Callable,
    *args,
    **kwargs,
) -> Any:
    """Retry provided funtion repeatedly until returns value passing validator.

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Maximum nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        return_value_validator (Callable): Validator for evaluating
            returned value, if it is expected or not.
        funtion (Callable): Function to be called, until it does not raise
            exception and returns value passing validator call.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    last_exception = None
    logger_buffer = None

    for retry_num in range(retry_count):
        try:
            with deferred_logging() as logger_buffer:
                result = function(*args, **kwargs)
            if return_value_validator(result):
                logger_buffer.flush_to_target(log)
                return result
            else:
                raise AssertionError(
                    f"{function.__name__}({args} {kwargs or ''}) did not return "
                    f"expected value, but: {result}"
                )
        except Exception as e:
            last_exception = e
            log.info(
                f"{function.__name__}({args} {kwargs or ''}) failed with: {e} "
                f"({retry_num}/{retry_count})"
            )
            log.debug(f"failed with: {e}")
        time.sleep(interval)
    else:
        if logger_buffer:
            logger_buffer.flush_to_target(log)
        raise AssertionError(
            f"Failed to execute "
            f"{function.__name__}({','.join([str(arg) for arg in args])} "
            f"{kwargs or ''}) after {retry_count} attempts."
        ) from last_exception
