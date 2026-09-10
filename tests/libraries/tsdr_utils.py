#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import json


def build_elastic_search_json_request(query_string: str) -> str:
    """Builds an Elasticsearch query body searching for the given query string.

    Args:
        query_string (str): Elasticsearch query_string query.

    Returns:
        str: JSON-encoded Elasticsearch request body.
    """
    data = {
        "from": "0",
        "size": "1",
        "sort": [{"TimeStamp": {"order": "desc"}}],
        "query": {"query_string": {"query": query_string}},
    }
    return json.dumps(data)


def create_query_string_search(
    data_category: str, metric_name: str, node_id: str, rk_node_id: str
) -> str:
    """Builds a TSDR Elasticsearch query string for a specific metric value.

    Args:
        data_category (str): TSDR data category.
        metric_name (str): Name of the metric.
        node_id (str): Node identifier.
        rk_node_id (str): Record key node identifier.

    Returns:
        str: Elasticsearch query_string value.
    """
    query = "TSDRDataCategory:"
    query += data_category
    query += " AND MetricName:"
    query += metric_name
    query += ' AND NodeID:"'
    query += node_id
    query += '" AND RecordKeys.KeyValue:"'
    query += rk_node_id
    query += '" AND RecordKeys.KeyName:Node AND RecordKeys.KeyValue:0 AND RecordKeys.KeyName:Table'
    return query


def create_query_string_count(data_category: str) -> str:
    """Builds a TSDR Elasticsearch query string for counting records of a category.

    Args:
        data_category (str): TSDR data category.

    Returns:
        str: Elasticsearch query_string value.
    """
    query = "TSDRDataCategory:"
    query += data_category
    return query


def extract_metric_value_search(response: dict) -> str:
    """Extracts the metric value from an Elasticsearch search response.

    Args:
        response (dict): Parsed Elasticsearch response body.

    Returns:
        str: Extracted metric value.
    """
    return str(response["hits"]["hits"][0]["_source"]["MetricValue"])


def extract_metric_value_count(response: dict) -> int:
    """Extracts the total hit count from an Elasticsearch response.

    Args:
        response (dict): Parsed Elasticsearch response body.

    Returns:
        int: Total number of hits.
    """
    return int(response["hits"]["total"])
