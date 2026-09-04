#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import time

from controller_testlib.infra import shell
from controller_testlib.KarafShell import KarafShell
from controller_testlib.shard_readiness import shard_has_leader

KARAF_SHELL_INSTANCE = None

log = logging.getLogger(__name__)


def start_odl_with_features(features: tuple[str], timeout: int = 60):
    """Starts ODL with installed provided features.

    Args:
        features (tuple[str]): Features to be installed in ODL.
        timeout (int): Timeout within which it needs to start ODL, otherwise fail.

    Returns:
        None
    """
    # set config with the required features
    shell(
        f"sed -ie 's/\(featuresBoot=\|featuresBoot =\)/featuresBoot = "
        f"{",".join(features)},/g' etc/org.apache.karaf.features.cfg",
        cwd="opendaylight",
    )

    shell(
        "sed -ie 's/memory-mapped = true/memory-mapped = false/g' "
        "system/org/opendaylight/controller/sal-clustering-config/*/"
        "sal-clustering-config-*-factorypekkoconf.xml",
        cwd="opendaylight",
    )

    # start ODL
    shell("JAVA_OPTS=-Xmx8g ./bin/start", cwd="opendaylight")

    # wait for proper message with timeout
    interval = 5
    for attempt in range(1, (timeout // interval) + 1):
        rc, _ = shell(
            "grep 'org.opendaylight.infrautils.*System ready' data/log/karaf.log",
            cwd="opendaylight",
        )
        if rc == 0:
            return
        time.sleep(interval)

    raise TimeoutError(f"ODL did not become ready within {timeout} seconds.")


def execute_karaf_command(command: str) -> tuple[str, str]:
    """Executed specific command using ODL karaf CLI console

    It usses ssh connection to connect to karaf CLI.

    Args:
        command (str): Command to be executed.

    Returns:
        tuple[str, str]: Stdout from karaf CLI, stderr from karaf CLI.
    """
    global KARAF_SHELL_INSTANCE

    log.info(f"Executing command '{command}' on karaf console.")

    if KARAF_SHELL_INSTANCE is None:
        KARAF_SHELL_INSTANCE = KarafShell(host="127.0.0.1", port=8101)

    try:
        stdout = KARAF_SHELL_INSTANCE.execute(command)
        log.info(f"Command Output:\n{stdout}")

        return stdout, ""

    except Exception as e:
        log.error(f"Failed to execute karaf command: {e}")
        return "", str(e)


def log_message_to_karaf(message: str):
    """Log specific mesage to ODL karaf

    It usses ssh connection to connect to karaf CLI.

    Args:
        message (str): Message to be logged.

    Returns:
        None
    """
    execute_karaf_command(f"log:log 'ROBOT MESSAGE: {message}'")


def is_karaf_feature_installed(feature_name: str) -> bool:
    """Check if the given feature is found in the output of 'feature:list -i'.

    Args:
        feature_name (str): Exact name of the Karaf feature to look up.

    Returns:
        bool: True if the feature is installed, False otherwise.
    """
    output, _ = execute_karaf_command(f"feature:list -i | grep {feature_name}")
    return any(line.split(" ")[0] == feature_name for line in output.splitlines())


def is_datastore_ready(shard_status: dict) -> bool:
    """Check whether a distributed-datastore shard has an elected Raft leader.

    Args:
        shard_status (dict): Parsed JSON body of a shard status query (e.g.
            via the cluster-admin RESTCONF API or the ShardManager JMX/jolokia
            MBean).

    Returns:
        bool: True if the shard has an elected leader, False otherwise.
    """
    return shard_has_leader(shard_status)
