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


def verify_jsons_match(
    json1: str,
    json2: str,
    json1_data_label: str = "json1",
    json2_data_label: str = "json2",
    volatiles_list: List[str] | Tuple[str] = (),
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

    Returns:
        None
    """
    normalized_json1 = norm_json.normalize_json_text(
        json1, keys_with_volatiles=volatiles_list
    )
    normalized_json2 = norm_json.normalize_json_text(
        json2, keys_with_volatiles=volatiles_list
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
        if len(visual_diff) > 2000:
            visual_diff = visual_diff[:2000] + " ... (truncated long output)"
        raise AssertionError(f": \n{visual_diff}")


def normalize_xml_lines(xml_input):
    """Parses, cleans, and standardizes XML formatting into a list of lines.

    Args:
        xml_input (str | bytes): The raw XML content to be normalized.

    Returns:
        List[str]: A list of cleanly formatted and indented XML lines.
    """
    if isinstance(xml_input, bytes):
        xml_input = xml_input.decode("utf-8")

    dom = xml.dom.minidom.parseString(xml_input)

    # Strip out existing purely whitespace text nodes.
    # This stops minidom from double-spacing or preserving weird indents.
    for node in dom.getElementsByTagName("*"):
        for child in list(node.childNodes):
            if child.nodeType == xml.dom.Node.TEXT_NODE and not child.nodeValue.strip():
                node.removeChild(child)

    pretty_xml = dom.toprettyxml(indent=" " * 4)

    return [line.rstrip() for line in pretty_xml.splitlines() if line.strip()]


def verify_xmls_match(xml1, xml2, xml1_data_label1, xml2_data_label):
    """Verify if provided xmls are the same after normalization.

    Args:
        xml1 (str): First xml value.
        xml2 (str): Second xml value.
        xml1_data_label (str): Descrption of the first xml value used as label.
        xml2_data_label (str): Descrption of the second xml value used as label.

    Returns:
        None
    """
    normalized_xml_lines1 = normalize_xml_lines(xml1)
    normalized_xml_lines2 = normalize_xml_lines(xml2)

    log.debug(f"{normalized_xml_lines1=}")
    log.debug(f"{normalized_xml_lines2=}")

    diff = list(
        difflib.unified_diff(
            normalized_xml_lines1,
            normalized_xml_lines2,
            fromfile=xml1_data_label1,
            tofile=xml2_data_label,
            lineterm="",
        )
    )

    if diff:
        diff_message = "\n".join(diff)
        raise AssertionError(f"XMLs do not match! Differences found:\n{diff_message}")


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
