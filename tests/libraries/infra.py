#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import signal
import subprocess
import threading
import time
import urllib.request
from queue import Queue
from typing import List

import psutil

from controller_testlib.utils import (
    wait_until_function_returns_value_with_custom_value_validator,
)

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
    It provides multiple options on how to run the command.

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
                return AssertionError(f"Expected command {exec_command} to pass")
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


def retry_shell_command(
    retry_count: int, interval: int, *args, **kwargs
) -> tuple[int, str]:
    """Repeatedly runs a shell command until the return code is 0.

    Args:
        retry_count (int): Maximum number of retries
        interval (int): Number of seconds to wait until next retry.
        *args: shell positional argments
        **kwargs: shell keyword arguments

    Returns:
        tuple[int, str]: Return code and standart output of successful run
            of the command.
    """
    validator = lambda result: result[0] == 0
    rc, output = wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, shell, *args, **kwargs
    )
    return rc, output


def count_port_occurrences(port: int, state: str, name: str) -> int:
    """Counts number of occurrences of specific port types.

    Args:
        port (str): Port number.
        state (str): Port state.
        name (str): Name of the program using the port.

    Returns:
        int: Number of port occurrences.
    """
    rc, stdout = shell(
        f'ss -punta 2> /dev/null | grep -E "{state} .+:{port} .+{name}" | wc -l'
    )
    log.warn(f"{stdout=}")
    assert (
        rc == 0
    ), f"Failed to check number of occurrences for {port=} {state=} {name=}"
    return int(stdout)


def is_process_still_running(pid: int):
    """Check if provided process did not finish yet.

    Args:
        process (subprocess.Popen): Process handler.

    Returns:
        None
    """
    try:
        process = psutil.Process(pid)
    except psutil.NoSuchProcess:
        return False
    return process.is_running() and process.status() != psutil.STATUS_ZOMBIE


def get_file_content(path: str) -> str:
    """Returns text file content.

    Args:
        path (str): Text file path.

    Returns:
        str: Text file content.
    """
    with open(path, "r", encoding="utf-8") as file:
        content = file.read()

    return content


def save_text_to_a_file(file_path: str, content: str):
    """Writes text content to a file.

    Args:
        file_path (str): The destination file path.
        content (str): The text content to write.

    Returns:
        None
    """
    with open(file_path, "w", encoding="utf-8") as file:
        file.write(content)


def copy_file(
    src_dir: str,
    src_file_name: str,
    dst_dir: str,
    target_file_name: str | None = None,
):
    """Copy file from one location to another.

    By providing target_file_name parameter, file would be renamed.

     Args:
        src_dir (str): Source file directory in which it is located.
        src_file_name (str): Name of the file to be copied.
        dst_dir (str): Destination directory where the file should be copied.
        target_file_name (str): Optional target file name, set if the copied
            file needs to be stored under different name in the target directory.
            By default it keeps the file name.

    Returns:
        None
    """
    if target_file_name is None:
        target_file_name = src_file_name
    shell(f"cp {src_dir}/{src_file_name} {dst_dir}/{target_file_name}")


def copy_dir(
    src_dir: str,
    dst_dir: str,
):
    """Copy directory from one location to another.

     Args:
        src_dir (str): Source directory to be copied.
        dst_dir (str): Destination directory where the directory should be copied.

    Returns:
        None
    """
    shell(f"cp -r {src_dir} {dst_dir}")


def stop_process_by_pid(pid: int, gracefully: bool = True, timeout: int | None = 5):
    """Stops process by sending signal and verifies it is not running.

    Args:
        pid (int): The operating system PID of the target process.
        gracefully (bool): Determines which signal should be sent,
            for gracefully it sends SIGTERM, otherwise SIGKILL
        timeout (int | None): Seconds to wait for termination. None skips verification.

    Returns:
        None
    """
    log.info(f"Stopping process with PID {pid}")
    signal_to_be_sent = signal.SIGTERM if gracefully else signal.SIGKILL
    log.info(f"Sending signal {signal_to_be_sent} to process with PID {pid}")
    process = psutil.Process(pid)
    process.send_signal(signal_to_be_sent)

    if timeout is not None:
        # check if it is still running
        interval = 1
        for _ in range(timeout // interval):
            if not is_process_still_running(process.pid):
                return
            time.sleep(interval)
        raise AssertionError(
            f"Was not able to stop process with PID {process.pid}, "
            f"it is still running."
        )


def read_until(process: subprocess.Popen, expected_text: str, timeout: int = 10) -> str:
    """Reads process stdout until expected string is found.

    This exepcted text must be within one line, it can not spread across
    mutliple lines. Command needs to finish within specific time otherwise
    it would time out.

    Args:
        process (str): Process which stdout should be observerd.
        expected_text (str): Pattern to be read until.
        timeout (int): Timeout in seconds.

    Returns:
        str: Process stdout until expected string occurence.
    """
    found_event = threading.Event()
    output_lines = []

    def threaded_read(stream, expected_text, queue):
        while True:
            line = stream.readline()
            output_lines.append(line)
            if line and expected_text in line:
                found_event.set()
                return

    queue = Queue()
    thread = threading.Thread(
        target=threaded_read, args=(process.stdout, expected_text, queue), daemon=True
    )
    thread.start()
    text_wasfound = found_event.wait(timeout=timeout)

    whole_text = "\n".join(output_lines)

    if not text_wasfound:
        raise AssertionError(
            (
                f"Unable to find expected text:' {expected_text}' withing {timeout} "
                f"seconds. Captured output: '{whole_text}'"
            )
        )

    log.warn(whole_text)

    return whole_text


def get_children_processes_pids(
    process: subprocess.Popen, command: str = ""
) -> List[int]:
    """Returns pids of a child processes which are running certain command.

    Args:
        process (str): Parent process handler.
        command (str): Command which child process is running.

    Returns:
        List[int]: List of matching children processes pids.
    """
    process = psutil.Process(process.pid)
    children = process.children(recursive=True)
    log.warn(process)
    log.warn([child.name() for child in children])
    matching_pids = [
        child.pid for child in children if child.name().startswith(command)
    ]

    return matching_pids


def wait_for_string_in_file(
    retry_count: int, interval: int, string: str, file_name: str, threshold: int = 1
) -> int:
    """Repeatedly reads text file until it finds specific substring.

    Args:
        retry_count (int): Maximum number of retries
        interval (int): Number of seconds to wait until next retry.
        string (str): Subsring expected to be present in text file.
        file_name (str): Name of the text file to be checked for
            presence of substring.
        threshold (int): Minimum number of occurences of the expected
            substring.

    Returns:
        int: number of substring occurences in text file
    """
    validator = lambda result: result[0] == 0 and int(result[1].strip()) >= threshold
    rc, output = wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, shell, f"grep -c '{string}' '{file_name}'"
    )
    return int(output.strip())


def get_string_occurence_count_in_file(string: str, file_name: str) -> int:
    """Counts number of occurences of specific string in a text file.

    Args:
        string (str): Pattern to be matched.
        file_name (str): Name of the text file to be searched.

    Returns:
        int: Number of occurences.
    """
    rc, output = shell(f"grep -c '{string}' '{file_name}'")

    return int(output)


def verify_string_occurence_count_in_file(
    string: str, file_name: str, count: int, exact: bool = True
):
    """Verifies number of occurences of specific string in a text file.

    Args:
        string (str): Pattern to be matched.
        file_name (str): Name of the text file to be searched.
        count (int): Expected number of string occurences.
        exact (bool): If set, number of occurences must exactly match
            the expected count, otherwise must be greater then or equal to
            the expected count.

    Returns:
        None
    """
    found_occurences = get_string_occurence_count_in_file(string, file_name)
    if exact:
        assert (
            found_occurences == count
        ), f"Did not find {count} times str: {string} in {file_name}"
    else:
        assert (
            found_occurences >= count
        ), f"Did not find at least {count} times str: {string} in {file_name}"


def stop_process(process: subprocess.Popen, gracefully=True):
    """Stop process by sending proper signal, but print stdout first.

    Args:
        process (subprocess.Popen): Process handler.
        gracefully (bool): Determines which signal should be sent for
            stopping process.

    Returns:
        None
    """
    output = process.stdout
    log.debug(f"Process output: {output=}")
    stop_process_by_pid(process.pid, gracefully=gracefully)


def download_file(url: str):
    """Download file from specified url.

    Stores downloaded file under tmp folder.

    Args:
        url (str): File URL location.

    Returns:
        None.
    """
    file_name = url.split("/")[-1]
    urllib.request.urlretrieve(url, f"tmp/{file_name}")


def backup_file(
    src_file_name: str,
    target_file_name: str | None = None,
    src_dir: str = "tmp",
    dst_dir: str = "results",
):
    """Backup test file by copiyng it to the destination directory

    Files from tmp/ folder are removed during start of the test, to keep
    these files persistantly they need to be moved to the results folder.

     Args:
        tmp_file_name (str): Path to the tmp file, which should be
            persistantly stored.
        target_file_name (str): Optional target file name, set if the copied
            file needs to be stored under different name in results folder.
            By default it keeps the file name.
        src_dir (str): Source file directory in which it is located.
        dst_dir (str): Destination directory where the file should be copied.

    Returns:
        None
    """
    if src_file_name == "":
        raise ValueError("tmp_file_name value can not be empty string")
    if target_file_name == "":
        raise ValueError("target_file_name value can not be empty string")
    if target_file_name is None:
        target_file_name = src_file_name
    shell(f"cp {src_dir}/{src_file_name} {dst_dir}/{target_file_name}")
