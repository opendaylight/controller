"""This module contains single a function for normalizing JSON strings."""

# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import collections as _collections
import jmespath

try:
    import simplejson as _json
except ImportError:  # Python2.7 calls it json.
    import json as _json


__author__ = "Vratko Polak"
__copyright__ = "Copyright(c) 2015-2016, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "vrpolak@cisco.com"


# Internal details; look down below for Robot Keywords.


class _Hsfl(list):
    """
    Hashable sorted frozen list implementation stub.

    Supports only __init__, __repr__ and __hash__ methods.
    Other list methods are available, but they may break contract.
    """

    def __init__(self, *args, **kwargs):
        """Contruct super, sort and compute repr and hash cache values."""
        sup = super(_Hsfl, self)
        sup.__init__(*args, **kwargs)
        sup.sort(key=repr)
        self.__repr = repr(tuple(self))
        self.__hash = hash(self.__repr)

    def __repr__(self):
        """Return cached repr string."""
        return self.__repr

    def __hash__(self):
        """Return cached hash."""
        return self.__hash


class _Hsfod(_collections.OrderedDict):
    """
    Hashable sorted (by key) frozen OrderedDict implementation stub.

    Supports only __init__, __repr__ and __hash__ methods.
    Other OrderedDict methods are available, but they may break contract.
    """

    def __init__(self, *args, **kwargs):
        """Put arguments to OrderedDict, sort, pass to super, cache values."""
        self_unsorted = _collections.OrderedDict(*args, **kwargs)
        items_sorted = sorted(list(self_unsorted.items()), key=repr)
        sup = super(_Hsfod, self)  # possibly something else than OrderedDict
        sup.__init__(items_sorted)
        # Repr string is used for sorting, keys are more important than values.
        self.__repr = (
            "{" + repr(list(self.keys())) + ":" + repr(list(self.values())) + "}"
        )
        self.__hash = hash(self.__repr)

    def __repr__(self):
        """Return cached repr string."""
        return self.__repr

    def __hash__(self):
        """Return cached hash."""
        return self.__hash


def _hsfl_array(s_and_end, scan_once, **kwargs):
    """Scan JSON array as usual, but return hsfl instead of list."""
    values, end = _json.decoder.JSONArray(s_and_end, scan_once, **kwargs)
    return _Hsfl(values), end


class _Decoder(_json.JSONDecoder):
    """Private class to act as customized JSON decoder.

    Based on: http://stackoverflow.com/questions/10885238/
    python-change-list-type-for-json-decoding"""

    def __init__(self, **kwargs):
        """Initialize decoder with special array implementation."""
        _json.JSONDecoder.__init__(self, **kwargs)
        # Use the custom JSONArray
        self.parse_array = _hsfl_array
        # Use the python implemenation of the scanner
        self.scan_once = _json.scanner.py_make_scanner(self)


# Robot Keywords; look above for internal details.


def loads_sorted(text, strict=False):
    """Return Python object with sorted arrays and dictionary keys."""
    object_decoded = _json.loads(text, cls=_Decoder, object_hook=_Hsfod)
    return object_decoded


def dumps_indented(obj, indent=1):
    """
    Wrapper for json.dumps with default indentation level.

    The main value is that BuiltIn.Evaluate cannot easily accept Python object
    as part of its argument.
    Also, allows to use something different from RequestsLibrary.To_Json

    """
    pretty_json = _json.dumps(obj, separators=(",", ": "), indent=indent)
    return pretty_json + "\n"  # to avoid diff "no newline" warning line


def sort_bits(obj, keys_with_bits=[]):
    """
    Rearrange string values of list bits names in alphabetical order.

    This function looks at dict items with known keys.
    If the value is string, space-separated names are sorted.
    This function is recursive over dicts and lists.
    Current implementation performs re-arranging in-place (to save memory),
    so it is not required to store the return value.

    The intended usage is for composite objects which contain
    OrderedDict elements. The implementation makes sure that ordering
    (dictated by keys) is preserved. Support for generic dicts is an added value.

    Sadly, dict (at least in Python 2.7) does not have __updatevalue__(key) method
    which would guarantee iteritems() is not affected when value is updated.
    Current "obj[key] = value" implementation is safe for dict and OrderedDict,
    but it may be not safe for other subclasses of dict.

    TODO: Should this docstring include links to support dict and OrderedDict safety?
    """
    if isinstance(obj, dict):
        for key, value in obj.items():
            # Unicode is not str and vice versa, isinstance has to check for both.
            # Luckily, "in" recognizes equivalent strings in different encodings.
            # Type "bytes" is added for Python 3 compatibility.
            if key in keys_with_bits and isinstance(value, (str, bytes)):
                obj[key] = " ".join(sorted(value.split(" ")))
            else:
                sort_bits(value, keys_with_bits)
    # A string is not a list, so there is no risk of recursion over characters.
    elif isinstance(obj, list):
        for item in obj:
            sort_bits(item, keys_with_bits)
    return obj


def hide_volatile(obj, keys_with_volatiles=[]):
    """
    Takes list of keys with volatile values, and replaces them with generic "*"

    :param obj: python dict from json
    :param keys_with_volatiles: list of volatile keys
    :return: corrected
    """
    if isinstance(obj, dict):
        for key, value in obj.items():
            # Unicode is not str and vice versa, isinstance has to check for both.
            # Luckily, "in" recognizes equivalent strings in different encodings.
            # Type "bytes" is added for Python 3 compatibility.
            if key in keys_with_volatiles and isinstance(
                value, (str, bytes, int, bool)
            ):
                obj[key] = "*"
            else:
                hide_volatile(value, keys_with_volatiles)
    # A string is not a list, so there is no risk of recursion over characters.
    elif isinstance(obj, list):
        for item in obj:
            hide_volatile(item, keys_with_volatiles)
    return obj


def normalize_json_text(
    text,
    strict=False,
    indent=1,
    keys_with_bits=[],
    keys_with_volatiles=[],
    jmes_path=None,
):
    """
    Attempt to return sorted indented JSON string.

    If jmes_path is set the related subset of JSON data is returned as
    indented JSON string if the subset exists. Empty string is returned if the
    subset doesn't exist.
    Empty string is returned if text is not passed.
    If parse error happens:
    If strict is true, raise the exception.
    If strict is not true, return original text with error message.
    If keys_with_bits is non-empty, run sort_bits on intermediate Python object.
    """

    if not text:
        return ""

    if jmes_path:
        json_obj = _json.loads(text)
        subset = jmespath.search(jmes_path, json_obj)
        if not subset:
            return ""
        text = _json.dumps(subset)

    try:
        object_decoded = loads_sorted(text)
    except ValueError as err:
        if strict:
            raise err
        else:
            return str(err) + "\n" + text
    if keys_with_bits:
        sort_bits(object_decoded, keys_with_bits)
    if keys_with_volatiles:
        hide_volatile(object_decoded, keys_with_volatiles)
        # re-sort the json data based on the hidden volatile values
        masked_text = dumps_indented(object_decoded, indent=indent)
        object_decoded = loads_sorted(masked_text)

    pretty_json = dumps_indented(object_decoded, indent=indent)

    return pretty_json
