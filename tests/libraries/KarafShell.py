#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import time
import re

from libraries import ssh_utils

log = logging.getLogger(__name__)


class KarafShell:
    """
    A persistent SSH client for Karaf that keeps a single shell open.
    It auto-connects, executes commands, and cleans up ANSI control codes
    while preserving the prompt at the end.
    """

    KARAF_PROMPT_REGEX = r"[a-zA-Z0-9_\-\.]+@[a-zA-Z0-9_\-\.]+>\s*"

    def __init__(self, host="127.0.0.1", port=8101, user="karaf", password="karaf"):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.client = None
        self.shell = None
        self.prompt_regex = self.KARAF_PROMPT_REGEX

    def connect(self) -> str:
        """Connects to ODL karaf CLI using SSH.

        Args:
            None

        Returns:
            str: Initial connection output.
        """
        if self.client is None or self.shell is None:
            log.debug(
                f"Opening interactive SSH connection to {self.host}:{self.port}..."
            )
            self.client = ssh_utils.create_ssh_client(
                self.host, self.port, self.user, self.password
            )
            self.shell = self.client.invoke_shell(term="dumb", width=500, height=200)
            raw_output = self._read_until_prompt()
            cleaned_output = self._clean_terminal_noise(raw_output)
            log.debug("Successfully connected.")

            return cleaned_output

    def close(self):
        """Closes SSH connection to ODL karaf CLI.

        Args:
            None

        Returns:
            None
        """
        log.debug(f"Closing interactive KarafShell to {self.host}:{self.port}...")
        if self.shell:
            self.shell.close()
            self.shell = None
        if self.client:
            self.client.close()
            self.client = None
        log.debug("KarafShell successfully disconnected.")

    def execute(self, command: str, timeout: int = 30) -> str:
        """Executes command on karaf CLI, auto-connecting if necessary.

        Args:
            command (str): Command to be executed.
            timeout (int): Timeout in seconds.

        Returns:
            str: Command output.
        """
        if not self.shell or not self.client:
            self.connect()

        output = self.send(command, timeout)

        return output

    def send(self, command: str, timeout: int = 30) -> str:
        """Sends command to an already active karaf CLI.

        Args:
            command (str): Command to be executed.
            timeout (int): Timeout in seconds.

        Returns:
            str: Command output.
        """
        # Check if properly connected
        if not self.shell or not self.client:
            raise Exception("SSH client is not connected")

        try:
            # Read all the output left from previous command
            while self.shell.recv_ready():
                self.shell.recv(4096)

            # Send command
            self.shell.send(f"{command}\n")
            raw_output = self._read_until_prompt(timeout)

            # Remove the echoed command from output (first line)
            raw_output = "\n".join(raw_output.split("\n")[1:])

            # Return the final output
            cleaned_output = self._clean_terminal_noise(raw_output)
            return cleaned_output

        except Exception as e:
            log.warning(f"KarafShell error during executing command: {command} - {e}.")
            self.close()
            raise e

    def _clean_terminal_noise(self, raw_text: str) -> str:
        """
        Cleanes terminal text from messy artifacts hidden by carriage return character
        '\r'.

        Args:
            raw_text (int): Raw text as read from SSH output.

        Returns:
            str: Cleaned output.
        """
        filtered_lines = []
        for line in raw_text.split("\n"):
            final_line_state = line.split("\r")[-1]
            if not final_line_state.strip():
                continue
            filtered_lines.append(final_line_state)
        return "\n".join(filtered_lines)

    def _read_until_prompt(self, timeout=30) -> str:
        """Reads stdout from karaf CLI until prompt is recognized.

        Args:
            timeout (int): Timeout in seconds.

        Returns:
            str: Raw buffer output.
        """
        buffer = ""
        start_time = time.time()
        while True:
            if time.time() - start_time > timeout:
                raise TimeoutError(
                    f"Timed out waiting for Karaf prompt. Buffer:\n{buffer}"
                )

            if self.shell.recv_ready():
                chunk = self.shell.recv(4096).decode("utf-8", errors="ignore")
                buffer += chunk
                if re.search(self.prompt_regex, buffer):
                    return buffer

            time.sleep(0.05)
