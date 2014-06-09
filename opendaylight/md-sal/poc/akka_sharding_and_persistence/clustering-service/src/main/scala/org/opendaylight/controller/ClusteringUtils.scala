package org.opendaylight.controller

object ClusteringUtils {
  def shardAddress(memberAddress : String, moduleName : String , shardName : String) = { memberAddress + "/user/shard-manager/" + moduleName + "-" + shardName}
}
