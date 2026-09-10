#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import collections
import errno
import logging
import os
import re

# Make sure to have unique matches in different lines
# Order the list in alphabetical order based on the "issue" key
_whitelist = [
    {
        "issue": "https://jira.opendaylight.org/browse/OPNFLWPLUG-917",
        "id": "IllegalStateException",
        "context": [
            "java.lang.IllegalStateException: Deserializer for key: msgVersion: 4 objectClass: "
            + "org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entries.grouping.MatchEntry "
            + "msgType: 1 oxm_field: 33 experimenterID: null was not found "
            + "- please verify that all needed deserializers ale loaded correctly"
        ],
    },
]

_re_ts = re.compile(r"^[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2},[0-9]{3}")
_re_ts_we = re.compile(
    r"^[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2},[0-9]{3}"
    r"( \| ERROR \| | \| WARN  \| )"
)
_re_ex = re.compile(r"(?i)exception")
_ex_map = collections.OrderedDict()
_ts_list = []
_fail = []


def get_exceptions(lines):
    """
    Create a map of exceptions that also has a list of warnings and errors preceeding
    the exception to use as context.

    The lines are parsed to create a list where all lines related to a timestamp
    are aggregated. Timestamped lines with exception (case insensitive) are copied
    to the exception map keyed to the index of the timestamp line. Each exception value
    also has a list containing WARN and ERROR lines proceeding the exception.

    :param list lines:
    :return OrderedDict _ex_map: map of exceptions
    """
    global _ex_map
    _ex_map = collections.OrderedDict()
    global _ts_list
    _ts_list = []
    cur_list = []
    warnerr_deq = collections.deque(maxlen=5)

    for line in lines:
        ts = _re_ts.search(line)

        # Check if this is the start or continuation of a timestamp line
        if ts:
            cur_list = [line]
            _ts_list.append(cur_list)
            ts_we = _re_ts_we.search(line)
            # Track WARN and ERROR lines
            if ts_we:
                warn_err_index = len(_ts_list) - 1
                warnerr_deq.append(warn_err_index)
        # Append to current timestamp line since this is not a timestamp line
        else:
            cur_list.append(line)

        # Add the timestamp line to the exception map if it has an exception
        ex = _re_ex.search(line)
        if ex:
            index = len(_ts_list) - 1
            if index not in _ex_map:
                _ex_map[index] = {"warnerr_list": list(warnerr_deq), "lines": cur_list}
                # reset the deque to only track new ERROR and WARN lines
                warnerr_deq.clear()

    return _ex_map


def check_exceptions():
    """
    Return a list of exceptions that were not in the whitelist.

    Each exception found is compared against all the patterns
    in the whitelist.

    :return list _fail: list of exceptions not in the whitelist
    """
    global _fail
    _fail = []
    _match = []
    for ex_idx, ex in _ex_map.items():
        ex_str = "__".join(ex.get("lines"))
        for whitelist in _whitelist:
            # skip the current whitelist exception if not in the current exception
            if whitelist.get("id") not in ex_str:
                continue
            whitelist_contexts = whitelist.get("context")
            num_context_matches = 0
            for whitelist_context in whitelist_contexts:
                for exwe_index in reversed(ex.get("warnerr_list")):
                    exwe_str = "__".join(_ts_list[exwe_index])
                    if whitelist_context in exwe_str:
                        num_context_matches += 1
            # Mark this exception as a known issue if all the context's matched
            if num_context_matches >= len(whitelist_contexts):
                ex["issue"] = whitelist.get("issue")
                _match.append(ex)
                logging.info("known exception was seen: {}".format(ex["issue"]))
                break
        # A new exception when it isn't marked with a known issue.
        if "issue" not in ex:
            _fail.append(ex)
    return _fail, _match


def verify_exceptions(lines):
    """
    Return a list of exceptions not in the whitelist for the given lines.

    :param list lines: list of lines from a log
    :return list, list: one list of exceptions not in the whitelist, and a second
        with matching issues
    """
    if not lines:
        return
    get_exceptions(lines)
    return check_exceptions()


def write_exceptions_map_to_file(testname, filename, mode="a+"):
    """
    Write the exceptions map to a file under the testname header. The output
    will include all lines in the exception itself as well as any previous
    contextual warning or error lines. The output will be appended or overwritten
    depending on the mode parameter. It is assumed that the caller has called
    verify_exceptions() earlier to populate the exceptions map, otherwise only
    the testname and header will be printed to the file.

    :param str testname: The name of the test
    :param str filename: The file to open for writing
    :param str mode: Append (a+) or overwrite (w+)
    """
    try:
        os.makedirs(os.path.dirname(filename))
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise

    with open(filename, mode) as fp:
        fp.write("{}\n".format("=" * 60))
        fp.write("Starting test: {}\n".format(testname))
        for ex_idx, ex in _ex_map.items():
            fp.write("{}\n".format("-" * 40))
            if "issue" in ex:
                fp.write("Exception was matched to: {}\n".format(ex.get("issue")))
            else:
                fp.write("Exception is new\n")
            for exwe_index in ex.get("warnerr_list")[:-1]:
                for line in _ts_list[exwe_index]:
                    fp.write("{}\n".format(line))
            fp.writelines(ex.get("lines"))
            fp.write("\n")
