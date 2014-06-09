package org.opendaylight.controller

import org.scalatest._

class ConfigurationUtilsSpec extends FlatSpec with Matchers{
  it should "find all members given a module name" in {
    val moduleName = "inventory"
    val memberNames = ConfigurationUtils.membersByModuleName(moduleName);
    memberNames should contain ("member-1", 1)
    memberNames should contain ("member-2", 2)
  }

  it should "find all shards given a member name" in {
    val memberName = "member-1";

    val shards = ConfigurationUtils.shardsByMemberName(memberName);

    shards should contain("inventory", "module-shard")
  }

  it should "find all members given a shard name" in {
    val moduleName = "inventory"
    val shardName ="module-shard";
    val members = ConfigurationUtils.membersByShardName(moduleName, shardName);

    members should contain("member-1", 1)
    members should contain("member-2", 2)
  }

}
