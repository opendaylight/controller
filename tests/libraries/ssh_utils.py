#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

import paramiko

log = logging.getLogger(__name__)


def create_ssh_client(
    hostname: str, port: int, username: str, password: str, timeout: int = 10
) -> paramiko.SSHClient:
    """Opens SSH connection to remote server.

    Args:
        hostname (str): Target server hostname or ip address.
        port (int): Port used for ssh conenction.
        username (str): username used to log in to the ssh server
        password (str): password used to log in to the ssh server
        timeout (int): Connection timeout in seconds.

    Returns:
        paramiko.SSHClient: Connected SSH client.
    """
    ssh_client = paramiko.SSHClient()
    ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        ssh_client.connect(
            hostname=hostname,
            port=port,
            username=username,
            password=password,
            look_for_keys=False,
            allow_agent=False,
            timeout=timeout,
        )
        return ssh_client
    except Exception as e:
        raise ConnectionError(f"Failed to connect to {hostname}:{port} - {e}")
