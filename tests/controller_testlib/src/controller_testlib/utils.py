#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

from contextlib import contextmanager
from collections.abc import Callable
import difflib
import logging
import time
from typing import Any, Generator, List, Tuple

from controller_testlib import norm_json

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


def truncate_long_text(text: str, max_size: int) -> str:
    """Truncates long text if it exceeds the maximum size.

    Args:
        text (str): Text to be truncate.
        max_size (int): Maximum allowed length. If -1, text is not truncated.

    Returns:
        str: Final truncated text.
    """
    if max_size == -1:
        return text

    if len(text) > max_size:
        text = text[:max_size] + " ... (truncated long output)"

    return text


def verify_jsons_match(
    json1: str,
    json2: str,
    json1_data_label: str = "json1",
    json2_data_label: str = "json2",
    volatiles_list: List[str] | Tuple[str] = (),
    jmes_path: str | None = None,
    max_visual_diff_log_size: int = 2000,
):
    """Verify if provided jsons are the same after normalization.

    Args:
        json1 (str): First json value.
        json2 (str): Second json value.
        json1_data_label (str): Descrption of the first json value used as
            label.
        json2_data_label (str): Descrption of the second json value used as
            label.
        volatiles_list (List[str] | Tuple[str]): List of volatiles values,
            which should be ingored in comparison.
        jmes_path (str | None): Optional JMESPath expression used to query, filter,
            or extract a specific subset of the JSON data prior to comparison.
        max_visual_diff_log_size (int): Maximum length of the logged visual
            diff before it gets truncated.

    Returns:
        None
    """
    normalized_json1 = norm_json.normalize_json_text(
        json1, keys_with_volatiles=volatiles_list, jmes_path=jmes_path
    )
    normalized_json2 = norm_json.normalize_json_text(
        json2, keys_with_volatiles=volatiles_list, jmes_path=jmes_path
    )
    log.debug(f"{normalized_json1=}")
    log.debug(f"{normalized_json2=}")

    if normalized_json1 != normalized_json2:
        visual_diff = "\n".join(
            difflib.unified_diff(
                normalized_json1.splitlines(),
                normalized_json2.splitlines(),
                fromfile=json1_data_label,
                tofile=json2_data_label,
                lineterm="",
                n=2000,
            )
        )
        # TODO: show in the output part which is different
        visual_diff = truncate_long_text(visual_diff, max_visual_diff_log_size)
        raise AssertionError(f": \n{visual_diff}")


def wait_until_function_pass(
    retry_count: int, interval: int, function: Callable, *args, **kwargs
) -> Any:
    """Retry provided function with its arguments repeatedly until it passes.

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Maximum number of function calls retries.
        interval (int): Number of seconds to wait until next try.
        function (Callable): Function to be called, until it does not raise
            exception.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returned by last successful function call.
    """
    validator = lambda value: True
    return wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, function, *args, **kwargs
    )


def wait_until_function_returns_value(
    retry_count: int,
    interval: int,
    expected_value: Any,
    function: Callable,
    *args,
    **kwargs,
) -> Any:
    """Retry provided function repeatedly until it returns concrete value.

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Maximum number of function calls retries.
        interval (int): Number of seconds to wait until next try.
        expected_value (Any): Value which is expected to be returned
            by the function call.
        function (Callable): Function to be called, until it does not raise
            exception and returns expected value.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returned by last successful function call.
    """
    validator = lambda value: value == expected_value
    return wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, function, *args, **kwargs
    )


def wait_until_function_returns_value_with_custom_value_validator(
    retry_count: int,
    interval: int,
    return_value_validator: Callable,
    function: Callable,
    *args,
    **kwargs,
) -> Any:
    """Retry provided function repeatedly until returns value passing validator.

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Maximum number of function calls retries.
        interval (int): Number of seconds to wait until next try.
        return_value_validator (Callable): Validator for evaluating
            returned value, if it is expected or not.
        function (Callable): Function to be called, until it does not raise
            exception and returns value passing validator call.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returned by last successful function call.
    """
    last_exception = None
    logger_buffer = None

    for retry_num in range(1, retry_count + 1):
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
            f"{kwargs or ''}) after {retry_count} attempts.\n"
            f"Last encountered error: {str(last_exception)}"
        ) from last_exception
