package org.opendaylight.controller

import com.typesafe.config.{ConfigValue, ConfigObject, ConfigFactory, Config}
import scala.collection.JavaConversions

/** ConfigurationUtils provides utility methods for reading cluster configuration
 *
 */
object ConfigurationUtils {

  val modulesConfig : Config = ConfigFactory.load("modules.conf");
  val modules = modulesConfig.getObjectList("modules");
  val modulesIterable = JavaConversions.collectionAsScalaIterable(modules);

  /**
   * Find a module given it's name
   *
   * @param moduleName
   * @return
   */
  private def moduleByName(moduleName : String) =
    modulesIterable.find((module : ConfigObject) => {
      moduleName == module.get("name").unwrapped().asInstanceOf[String];
    })

  /**
   * Find a shard given the module and it's name
   * @param module
   * @param shardName
   * @return
   */
  private def shardByName(module : ConfigObject, shardName : String) =
    JavaConversions.collectionAsScalaIterable(module.get("shards").asInstanceOf[java.util.List[ConfigObject]]).find((shard : ConfigObject) => {
      shard.get("name").unwrapped().asInstanceOf[String] == shardName
    })


  /**
   * Find all the shards that belong on the member
   * This could be used to find all the shards for the 'current' member
   *
   * @param memberName
   * @return
   */
  def shardsByMemberName(memberName : String) : Set[(String, String)] = {
    var shards = Set[(String, String)]()

    modulesIterable.foreach((module : ConfigObject) => {
      val listShards : java.util.List[ConfigObject] = module.get("shards").asInstanceOf[java.util.List[ConfigObject]]

      JavaConversions.collectionAsScalaIterable(listShards).foreach((shard : ConfigObject) => {

        val listReplicas : java.util.List[ConfigValue] = shard.get("replicas").asInstanceOf[java.util.List[ConfigValue]]

        JavaConversions.collectionAsScalaIterable(listReplicas).foreach((replica : ConfigValue) => {

          if(replica.unwrapped().asInstanceOf[String] == memberName){
            val shardName = shard.get("name").unwrapped().asInstanceOf[String];
            val moduleName = module.get("name").unwrapped().asInstanceOf[String];
            shards += Tuple2(moduleName, shardName)
          }

        })
      })
    })

    shards
  }

  /**
   * Find all the members on which a given modules shards exist
   *
   * @param moduleName
   * @return
   */
  def membersByModuleName(moduleName : String) = membersByShardName(moduleName, firstShardByModuleName(moduleName))

  /**
   * Find all members given a module name and shard name.
   * This can be used to find all members which host the replica of a shard
   *
   * @param moduleName
   * @param shardName
   * @return
   */
  def membersByShardName(moduleName : String , shardName : String) = {
    var memberNames = List[(String, Int)]();

    val module = moduleByName(moduleName);

    if( module != None ) {
      val shard = shardByName(module.get, shardName);

      if(shard != None){
        val listReplicas: java.util.List[ConfigValue] = shard.get.get("replicas").asInstanceOf[java.util.List[ConfigValue]]
        var i: Int = 0;
        JavaConversions.collectionAsScalaIterable(listReplicas).foreach((replica: ConfigValue) => {
          i = i + 1
          val memberName: String = replica.unwrapped().asInstanceOf[String]
          memberNames ::=(memberName, i)
        })
      }
    }

    memberNames.sortBy((f: (String, Int)) => { f._2} )
  }

  def firstShardByModuleName(moduleName : String) : String = {
    var shardName : String = null;

    val module = moduleByName(moduleName)

    if(module != None){
      val listShards: java.util.List[ConfigObject] = module.get.get("shards").asInstanceOf[java.util.List[ConfigObject]]
      val firstShard = listShards.get(0).unwrapped().asInstanceOf[java.util.HashMap[String, ConfigObject]]
      shardName  = firstShard.get("name").toString
    }

    return shardName;
  }

  def shardsByModuleName(moduleName : String) : List[String] = {
    var shards : List[String] = List();

    val module = moduleByName(moduleName)

    if(module != None){
      val listShards: java.util.List[ConfigObject] = module.get.get("shards").asInstanceOf[java.util.List[ConfigObject]]
      JavaConversions.collectionAsScalaIterable(listShards).foreach((f : ConfigObject) => {
        shards ::= f.get("name").unwrapped().asInstanceOf[String]
      })
    }

    shards

  }

}
