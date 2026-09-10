#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import paramiko


class RemoteSSHSessionHandler:

    def __init__(self, host: str, username: str, password: str, port: int = 22):
        """Initializes an SSH session to the host

        Args:
            host (str): Hostname of the remote SSH server.
            username (str): Client username.
            password (str): Client password.
            port (int): Remote SHH server port.

        Returns:
            None
        """
        ssh_client = paramiko.SSHClient()
        ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh_client.connect(
            hostname=host,
            port=port,
            username=username,
            password=password,
            look_for_keys=False,
            allow_agent=False,
        )
        self.ssh_client = ssh_client
        self.stdin, self.stdout, self.stderr = None, None, None

    def start_command(self, command: str):
        """Start command in an SSH session

        Enters a command on an SSH session and does not wait until it
        finishes.

        Args:
            command (str): Command to run.

        Returns:
            None
        """
        self.stdin, self.stdout, self.stderr = self.ssh_client.exec_command(
            command, get_pty=True
        )

    def stop_command(self):
        """Stop the currently running command

        Construct ctrl+c character and SSH-write it (without endline)
        to the current SSH connection.

        Args:
            None

        Returns:
            None
        """
        if self.stdin:
            self.stdin.write("\x03")
            self.stdin.flush()

        self.stdin, self.stdout, self.stderr = None, None, None
