"""
Utility library for retrieving entity related data from ODL.
"""

from logging import info
from requests import codes as status_codes
from requests import get
from requests import post
from sys import argv


def get_entities(restconf_url):
    resp = post(
        url=restconf_url + "/operations/odl-entity-owners:get-entities",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "csit agent",
        },
        auth=("admin", "admin"),
        timeout=10.0,
    )

    info(
        "Response %s",
        resp,
    )
    info(
        "Response JSON %s",
        resp.json(),
    )

    if resp.status_code == status_codes["bad_request"]:
        info(
            "Status code is '%s' - trying operational data instead.",
            resp.status_code,
        )
        result = get_entities_data(restconf_url)
    else:
        result = resp.json()

    return result


def get_entity_name(e_type, e_name):
    """
    Get the effective entity name for the given entity type.
    If the entity type is not for odl-general-entity, entity name
    should be the full instance identifier.
    :param e_type: entity type
    :param e_name: entity name
    :return: updated entity name
    """
    name_templates = {
        "ovsdb": "/network-topology:network-topology/topology[topology-id='ovsdb:1']/node[node-id='%s']",
    }

    if e_type in name_templates:
        return name_templates[e_type] % e_name
    else:
        return e_name


def get_entity(restconf_url, e_type, e_name):
    """Calls the get-entity rpc on the controller and returns the result in a
    dictionary that contains the parsed response in two keys:
    "candidates" and "owner"
    """

    data = """
    {
        "odl-entity-owners:input" : {
            "type": "%s",
            "name": "%s"
        }
    }
    """ % (
        e_type,
        get_entity_name(e_type, e_name),
    )

    info("Data %s", data)

    resp = post(
        url=restconf_url + "/operations/odl-entity-owners:get-entity",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "csit agent",
        },
        data=data,
        auth=("admin", "admin"),
        timeout=10.0,
    )

    info(
        "Response %s",
        resp,
    )
    info(
        "Response JSON %s",
        resp.json(),
    )

    if resp.status_code == status_codes["bad_request"]:
        info(
            "Status code is '%s' - trying operational data instead.",
            resp.status_code,
        )
        result = get_entity_data(restconf_url, e_type, e_name)
    else:
        result = {
            "candidates": resp.json()["odl-entity-owners:output"]["candidate-nodes"],
            "owner": resp.json()["odl-entity-owners:output"]["owner-node"],
        }

    return result


def get_entity_owner(restconf_url, e_type, e_name):
    data = """
    {
        "odl-entity-owners:input" : {
            "type": "%s",
            "name": "%s"
        }
    }
    """ % (
        e_type,
        get_entity_name(e_type, e_name),
    )

    info("Data %s", data)

    resp = post(
        url=restconf_url + "/operations/odl-entity-owners:get-entity-owner",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "csit agent",
        },
        data=data,
        auth=("admin", "admin"),
        timeout=10.0,
    )

    info(
        "Response %s",
        resp,
    )
    info(
        "Response JSON %s",
        resp.json(),
    )

    if resp.status_code == status_codes["bad_request"]:
        info(
            "Status code is '%s' - trying operational data instead.",
            resp.status_code,
        )
        result = get_entity_owner_data(restconf_url, e_type, e_name)
    else:
        result = resp.json()["odl-entity-owners:output"]["owner-node"]

    return result


def get_entities_data(restconf_url):
    """
    Get the entity information from the datastore for Silicon
    or earlier versions.
    :param restconf_url: RESTCONF URL up to the RESTCONF root
    """
    resp = get(
        url=restconf_url + "/data/entity-owners:entity-owners",
        headers={
            "Accept": "application/yang-data+json",
            "User-Agent": "csit agent",
        },
        auth=("admin", "admin"),
        timeout=10.0,
    )

    info(
        "Response %s",
        resp,
    )
    info(
        "Response JSON %s",
        resp.json(),
    )

    return resp.json()


def get_entity_type_data(restconf_url, e_type):
    """
    Get the entity information for the given entity type from the datastore
    for Silicon or earlier versions.
    :param restconf_url: RESTCONF URL up to the RESTCONF root
    :param e_type: entity type
    :return: entity-type
    """
    resp = get(
        url=restconf_url
        + "/data/entity-owners:entity-owners"
        + "/entity-type=%s" % e_type,
        headers={
            "Accept": "application/yang-data+json",
            "User-Agent": "csit agent",
        },
        auth=("admin", "admin"),
        timeout=10.0,
    )

    info(
        "Response %s",
        resp,
    )
    info(
        "Response JSON %s",
        resp.json(),
    )

    return resp.json()["entity-owners:entity-type"][0]


def get_entity_data(restconf_url, e_type, e_name):
    """
    Get the entity owner & candidates for the given entity type
    and entity name from the datastore for Silicon or earlier versions
    :param restconf_url: RESTCONF URL up to the RESTCONF root
    :param e_type: entity type
    :param e_name: entity name
    :return: entity owner & candidates
    """
    id_templates = {
        "org.opendaylight.mdsal.ServiceEntityType": "/odl-general-entity:entity[name='%s']",
        "org.opendaylight.mdsal.AsyncServiceCloseEntityType": "/odl-general-entity:entity[name='%s']",
        "ovsdb": "/network-topology:network-topology/topology[topology-id='ovsdb:1']/node[node-id='%s']",
    }
    id_template = id_templates[e_type]

    entity_type = get_entity_type_data(restconf_url, e_type)
    entity = [e for e in entity_type["entity"] if e["id"] == id_template % e_name][0]

    result = {
        "candidates": [c["name"] for c in entity["candidate"]],
        "owner": entity["owner"],
    }

    return result


def get_entity_owner_data(restconf_url, e_type, e_name):
    """
    Get the entity owner for the given entity type and entity name
    from the datastore for Silicon or earlier versions
    :param restconf_url: RESTCONF URL up to the RESTCONF root
    :param e_type: entity type
    :param e_name: entity name
    :return: entity owner
    """
    entity = get_entity_data(restconf_url, e_type, e_name)
    return entity["owner"]


def main(args):
    if args[0] == "get-entities":
        json = get_entities(
            restconf_url="http://127.0.0.1:8181/rests",
        )
        print(json)
    elif args[0] == "get-entity":
        json = get_entity(
            restconf_url="http://127.0.0.1:8181/rests",
            e_type=args[1],
            e_name=args[2],
        )
        print(json)
    elif args[0] == "get-entity-owner":
        json = get_entity_owner(
            restconf_url="http://127.0.0.1:8181/rests",
            e_type=args[1],
            e_name=args[2],
        )
        print(json)
    else:
        raise Exception("Unhandled argument %s" % args[0])


if __name__ == "__main__":
    # i.e. main does not depend on name of the binary
    main(argv[1:])
