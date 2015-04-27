package org.opendaylight.controller.md.sal.clustering.api;

import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRoleChange;


public interface ClusteringRoleService {

    void changeShardRole(String memberId, String shardId, ShardRole oldRole, ShardRole newRole);

    ShardRoleChange getLastShardRoleChanged(String shardId);

    Map<String, ShardRoleChange> getLastShardRoleChangedForAllShards();

}
