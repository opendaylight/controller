#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import re
import subprocess

from libraries import utils
from libraries.KarafShell import KarafShell

KARAF_SHELL_INSTANCE = None

log = logging.getLogger(__name__)


def shell(
    command: str | list | tuple,
    joiner="; ",
    cwd: str | None = None,
    use_shell=True,
    run_in_background: bool = False,
    timeout: int = None,
    check_rc=False,
):
    """Runs single or multiple shell commands.

    Multiple shell command are concatenated together by using joiner.
    It provides mutliple options on how to run the command.

    Args:
        command (str | list | tuple): Shell command(s) to be run.
        joiner (str): Joiner for concatenating multiple commands.
        cwd (str): Current working directory from where the command
            needs to be executed.
        run_in_backgroud (bool): If the command should be started as background
            process without tty.
        timeotu (int): Timeout in seconds for the foreground command.

    Returns:
        tuple[int, str] | subprocess.Popen :
            For foreground process it returns final return code and stdout,
            for backgroud process it returns process handler.
    """
    exec_command = command
    if isinstance(command, (list, tuple)):
        exec_command = joiner.join(command)

    try:
        log.info(exec_command)
        if run_in_background:
            if use_shell:
                process = subprocess.Popen(
                    f"exec {exec_command}",
                    shell=True,
                    text=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    stdin=subprocess.DEVNULL,
                    bufsize=1,
                    cwd=cwd,
                )
            else:
                process = subprocess.Popen(
                    exec_command.split(" "), shell=False, text=True, cwd=cwd
                )
            return process
        else:
            result = subprocess.run(
                exec_command,
                shell=use_shell,
                check=True,
                capture_output=True,
                text=True,
                timeout=timeout,
                cwd=cwd,
            )
            log.debug(f"{result.returncode:3d} |--| {result.stdout}")
            if check_rc and result.returncode != 0:
                raise AssertionError(f"Expected command {exec_command} to pass")
            return result.returncode, result.stdout
    except subprocess.CalledProcessError as e:
        std_error = e.stderr.strip()
        log.error(
            f"ERROR while command execution '{exec_command}'"
            f"{':\n' + std_error if std_error else ''}"
        )
        return e.returncode, e.stdout
    except FileNotFoundError:
        log.error(f"ERROR command not found: {exec_command}")
        return None, None


def start_odl_with_features(features: tuple[str]):
    """Starts ODL with installed provided features.

    Args:
        features (tuple[str]): Features to be installed in ODL.

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


def wait_for_shard_ready(
    shard_name: str = "default", store_type: str = "operational", timeout: int = 60
) -> str:
    """Blocks until the given datastore shard has a resolved Raft role.

    Uses the cluster-admin:get-shard-role karaf command, which only resolves
    once the datastore, cluster wiring, and leader election for that shard
    have actually completed. it works the same way for a single-node instance
    (which self-elects as Leader) and a multi-member cluster.

    Args:
        shard_name (str): Name of the shard to check, e.g. "default".
        store_type (str): Datastore type, "config" or "operational".
        timeout (int): Seconds to wait before failing.

    Returns:
        str: The resolved shard role, e.g. "Leader" or "Follower".
    """
    interval = 2

    def get_role() -> str:
        stdout, stderr = execute_karaf_command(
            f"cluster-admin:get-shard-role {shard_name} {store_type}"
        )
        match = re.search(r"Role\s+(\w+)", stdout)
        if not match:
            raise AssertionError(
                f"Could not find a resolved role for shard '{shard_name}' "
                f"({store_type}) in output:\n{stdout}{stderr}"
            )
        return match.group(1)

    return utils.wait_until_function_returns_value_with_custom_value_validator(
        max(1, timeout // interval),
        interval,
        lambda role: role in ("Leader", "Follower"),
        get_role,
    )


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
