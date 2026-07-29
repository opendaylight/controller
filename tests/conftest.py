#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import allure
from contextlib import contextmanager
import io
import logging
import pytest
from typing import ContextManager, Generator, Callable, List, Optional


from libraries import infra
from libraries.variables import variables

ODL_IP = variables.ODL_IP
KARAF_LOG_LEVEL = variables.KARAF_LOG_LEVEL
ODL_FEATURES = [
    "odl-clustering-test-app"
]

log = logging.getLogger(__name__)


def pytest_addoption(parser):
    """Adds custom command-line options to pytest."""
    parser.addoption(
        "--step-include",
        action="store",
        default=None,
        help="Comma-separated list of step tags to run",
    )
    parser.addoption(
        "--step-exclude",
        action="store",
        default=None,
        help="Comma-separated list of step tags to skip",
    )


@pytest.fixture
def allure_step_with_separate_logging(
    request: pytest.FixtureRequest,
) -> Callable[[str], ContextManager[None]]:
    """Provide context manager for Allure steps which separates logging

    This fixture extends standart allure_step context manger with functionality
    to store logs for each step seperately.

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        Callable: context manager for allure step with seperate logging.
    """

    @contextmanager
    def _log_step(title: str) -> Generator[any, None, None]:
        """Exectue allure step with seperate logging

        Args:
            title (str): Step title.

        Returns:
            Generator[any, None, None]: context manager for allure step
        """
        log_capture_string = io.StringIO()
        handler = logging.StreamHandler(log_capture_string)
        tox_ini_log_fromat = request.config.getini("log_format")
        formatter = logging.Formatter(tox_ini_log_fromat)
        handler.setFormatter(formatter)

        root_logger = logging.getLogger()
        root_logger.addHandler(handler)

        try:
            with allure.step(title) as allure_step:
                infra.log_message_to_karaf(f"Starting step: {title}")
                yield allure_step
        finally:
            infra.log_message_to_karaf(f"End of step: {title}")
            root_logger.removeHandler(handler)
            log_contents = log_capture_string.getvalue()
            if log_contents:
                allure.attach(
                    log_contents,
                    name=f"Logs for '{title}'",
                    attachment_type=allure.attachment_type.TEXT,
                )

    return _log_step


@pytest.fixture
def step_tag_checker(
    request: pytest.FixtureRequest,
) -> Callable[[Optional[List[str]]], bool]:
    """
    Returns a function that checks if a step should run based on tags.
    Reads --step-include and --step-exclude command-line options.

    Logic mimics Robot Framework:
    1. If --step-include is used, the step *must* match one tag.
    2. If --step-exclude is used, the step *must not* match any tag.
    """
    include_str = request.config.getoption("--step-include")
    exclude_str = request.config.getoption("--step-exclude")

    include_tags = set(include_str.split(",")) if include_str else set()
    exclude_tags = set(exclude_str.split(",")) if exclude_str else set()

    def _should_run_step(tags: Optional[str | List[str]]) -> bool:
        step_tags = {tags} if tags else set()
        if not exclude_tags.isdisjoint(step_tags):
            return False
        if include_tags and include_tags.isdisjoint(step_tags):
            return False
        return True

    return _should_run_step


@pytest.fixture(scope="session")
def preconditions():
    """Fixture for basic test session setup.

    It handles setting features to be installed, starting karaf, etc.

    Args:
        None

    Returns:
        None
    """
    infra.shell("rm -rf tmp && mkdir tmp")
    infra.shell("ls results || mkdir results")
    try:
        infra.start_odl_with_features(ODL_FEATURES)
        # This is a functional check of clustering itself, rather than a generic
        # "system ready" log message.
        infra.wait_for_shard_ready(timeout=580)
        infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}")
        yield
    finally:
        infra.shell("kill $(pgrep -f org.apache.karaf.main.[M]ain | grep -v ^$$\$)")


@pytest.fixture(scope="class")
def log_test_suite_start_end_to_karaf(request: pytest.FixtureRequest):
    """Fixture to log in karaf test suite start and end markers

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        None
    """
    infra.log_message_to_karaf(f"Starting suite {request.cls.__name__}")
    yield
    infra.log_message_to_karaf(f"End of suite {request.cls.__name__}")


@pytest.fixture(scope="function")
def log_test_case_start_end_to_karaf(request: pytest.FixtureRequest):
    """Fixture to log in karaf test case start and end markers

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        None
    """
    infra.log_message_to_karaf(
        f"Starting test {request.cls.__name__}.{request.node.name}"
    )
    yield
    infra.log_message_to_karaf(
        f"End of test {request.cls.__name__}.{request.node.name}"
    )
