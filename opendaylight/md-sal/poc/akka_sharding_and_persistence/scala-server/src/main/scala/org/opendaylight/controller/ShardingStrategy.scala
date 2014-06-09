package org.opendaylight.controller



trait ShardingStrategy {
  def findShard(identifier : String) : String
}

/**
 * Sharding Strategy which stores all of a modules data in a single shard
 */
class ModuleShardingStrategy(moduleName : String) extends ShardingStrategy {

  override def findShard(identifier: String): String = {
    ConfigurationUtils.firstShardByModuleName(moduleName)
  }

}
/**
 * Sharding Strategy which stores a modules data into a shard based on the hashCode of the identifier
 */
class HashShardingStrategy(moduleName : String) extends ShardingStrategy {

  override def findShard(identifier: String): String = {
    val shards = ConfigurationUtils.shardsByModuleName(moduleName);
    shards(identifier.hashCode % shards.length)
  }

}
