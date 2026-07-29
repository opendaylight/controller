#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

import pytest

from libraries import infra

log = logging.getLogger(__name__)


@pytest.mark.usefixtures(
    "preconditions",
    "log_test_suite_start_end_to_karaf",
    "log_test_case_start_end_to_karaf",
)
class TestSanity:
    """Sanity test suite verifying that the controller PyTest environment can
    actually start and drive a real ODL instance.

    The session-scoped 'preconditions' fixture starts karaf with the
    configured features before this test runs and stops it afterwards, so a
    passing test demonstrates that the whole environment setup (assembling
    and booting ODL, talking to its karaf console) is working correctly.
    """

    def test_odl_is_up_and_responsive(self):
        """Verify karaf console is responsive."""
        stdout, stderr = infra.execute_karaf_command("feature:list -i")
        log.info(f"Installed karaf features:\n{stdout}")

        assert not stderr, f"Failed to query installed karaf features: {stderr}"

    def test_default_shard_is_functional(self):
        """Verify the default datastore shard has a resolved role.

        This is a functional check: cluster-admin:get-shard-role only
        resolves once the datastore, cluster wiring and leader election
        have actually completed, which proves clustering is genuinely
        operational rather than just that bundles were marked installed.
        """
        role = infra.wait_for_shard_ready(timeout=60)
        log.info(f"Default operational shard role: {role}")
        assert role in ("Leader", "Follower")
