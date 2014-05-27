package org.opendaylight.controller

import com.typesafe.config.{ConfigValue, ConfigObject, ConfigFactory, Config}
import scala.collection.JavaConversions

object ConfigurationUtils {
  val shardsConfig : Config = ConfigFactory.load("shards.conf");
  val shards = shardsConfig.getObjectList("shards");
  val shardsIterable = JavaConversions.collectionAsScalaIterable(shards);

  val rolesConfig : Config = ConfigFactory.load("roles.conf");
  val roles = rolesConfig.getObjectList("roles");
  val rolesIterable = JavaConversions.collectionAsScalaIterable(roles);

  val membersConfig : Config = ConfigFactory.load("members.conf");
  val members = membersConfig.getObjectList("members");
  val membersIterable = JavaConversions.collectionAsScalaIterable(members);
  val memberNames = membersIterable.map(_.get("name").unwrapped());

  def shardByModuleName(moduleName : String) : String = {
    val x : Option[ConfigObject] = shardsIterable.find((p: ConfigObject) => {
      p.get("yang-module-name").unwrapped() == moduleName
    })

    if(x.get != null){
      return x.get.get("name").unwrapped().asInstanceOf[String]
    }

    return null;
  }

  def moduleByShardName(shardName : String) : String = {
    val x : Option[ConfigObject] = shardsIterable.find((p: ConfigObject) => {
      p.get("name").unwrapped() == shardName
    })

    if(x.get != null){
      return x.get.get("name").unwrapped().asInstanceOf[String]
    }

    return null;
  }

  // Assuming that only one role contains a given shard
  private def roleByShardName(shardName : String) : String = {
    rolesIterable.foreach((y: ConfigObject) => {
      val list : java.util.List[ConfigValue] = y.get("shards").asInstanceOf[java.util.List[ConfigValue]];
      val roleName : ConfigValue = y.get("name");

      val roleShards = JavaConversions.collectionAsScalaIterable(list);

      val found = roleShards.exists((p : ConfigValue) => {
        p.unwrapped() == shardName
      })

      if(found){
        return roleName.unwrapped().asInstanceOf[String];
      }

    })
    return null;
  }


  private def membersByRoleName(roleName : String)  = {
    var memberNames = collection.mutable.MutableList[(String, Int)]();

    membersIterable.foreach((member : ConfigObject) => {

      val list : java.util.List[ConfigObject] = member.get("roles").asInstanceOf[java.util.List[ConfigObject]];
      val roles = JavaConversions.collectionAsScalaIterable(list);
      val memberName = member.get("name").unwrapped();

      val found = roles.find((p : ConfigObject) => {
        p.get("name").unwrapped() == roleName
      })

      if(found != null){
        memberNames += Tuple2(memberName.asInstanceOf[String], found.get.get("priority").unwrapped().asInstanceOf[Int]);
      }

    })

    memberNames.sortBy((f: (String, Int)) => { f._2} )
  }

  private def rolesByMemberName(memberName : String) : Set[String] = {
    var roles = Set[String]();

    membersIterable.foreach((member : ConfigObject) => {

      val list : java.util.List[ConfigObject] = member.get("roles").asInstanceOf[java.util.List[ConfigObject]];
      roles ++= JavaConversions.collectionAsScalaIterable(list).map((f: ConfigObject) => {
        f.get("name").unwrapped().asInstanceOf[String]
      });
    })

    roles
  }


  private def shardsByRoleName(roleName : String ) : Set[String] = {
    var shards = Set[String]()

    val role = rolesIterable.find( _.get("name").unwrapped() == roleName);

    if(role.isDefined){
      val list : java.util.List[ConfigValue] = role.get.get("shards").asInstanceOf[java.util.List[ConfigValue]];
      shards ++= JavaConversions.collectionAsScalaIterable(list).map((configValue: ConfigValue) => {
        configValue.unwrapped().asInstanceOf[String]
      });
    }

   shards
  }

  def shardsByMemberName(memberName : String) : Set[String] = {
    var shards = Set[String]();

    val roles = rolesByMemberName(memberName);

    roles.foreach((f : String) => {
      shards ++= shardsByRoleName(f)
    })

    shards
  }

  def membersByModuleName(moduleName : String) = membersByShardName(shardByModuleName(moduleName))

  def membersByShardName(shardName : String) = {
    var memberNames = List[(String, Int)]();
    if(shardName != null){
      val roleName = roleByShardName(shardName)
      memberNames ++= membersByRoleName(roleName)
    }
    memberNames
  }



}
