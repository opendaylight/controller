package org.opendaylight.controller

import com.typesafe.config.{ConfigObject, ConfigFactory, Config}
import scala.collection.JavaConversions

object ShardingStrategyManager {
  val moduleShardingStrategiesConfig : Config = ConfigFactory.load("module-sharding-strategies.conf");
  val moduleShardingStrategies = moduleShardingStrategiesConfig.getObjectList("module-sharding-strategies");
  val moduleShardingStrategiesIterable = JavaConversions.collectionAsScalaIterable(moduleShardingStrategies);

  val shardingStrategies : scala.collection.mutable.Map[String, (String) => ShardingStrategy] = scala.collection.mutable.Map();

  def findShardingStrategy(moduleName : String) : ShardingStrategy = {
    var strategy : Option[String] = None
    val moduleStrategy = moduleShardingStrategiesIterable.find((f : ConfigObject) => {
      f.get("module-name").unwrapped().asInstanceOf[String] == moduleName
    })

    if(moduleStrategy != None){
      strategy = Some(moduleStrategy.get.get("strategy").unwrapped().asInstanceOf[String])
    }
    val strategyFn = shardingStrategies(strategy.get)

    strategyFn(moduleName)
  }

  def registerShardingStrategy(strategyName : String, f : (String) => ShardingStrategy) = {
    shardingStrategies(strategyName) = f;
  }
}
