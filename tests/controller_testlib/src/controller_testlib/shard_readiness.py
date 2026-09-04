def shard_has_leader(shard_status: dict) -> bool:
    """Return True if a distributed-datastore shard status report includes an
    elected Raft leader.

    shard_status is the parsed JSON body of a shard status query (e.g. via the
    cluster-admin RESTCONF API or the ShardManager JMX/jolokia MBean).
    """
    return bool(shard_status.get("leader"))
