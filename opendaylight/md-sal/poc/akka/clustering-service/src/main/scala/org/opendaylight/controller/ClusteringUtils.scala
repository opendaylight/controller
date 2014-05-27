package org.opendaylight.controller

object ClusteringUtils {
  def shardAddress(memberAddress : String, shardName : String) = { memberAddress + "/user/shard-manager/" + shardName}
}
