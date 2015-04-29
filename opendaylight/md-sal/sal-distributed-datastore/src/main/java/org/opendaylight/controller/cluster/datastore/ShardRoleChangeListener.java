package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.md.sal.clustering.api.ClusteringRoleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShardRoleChangeListener extends AbstractUntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(ShardRoleChangeListener.class);
    private final ClusteringRoleService clusteringService;
    private final Map<String, String> shardToMemberMap = new ConcurrentHashMap<>();

    public ShardRoleChangeListener(ClusteringRoleService clusteringService) {
        this.clusteringService = clusteringService;
    }

    public static Props getProps(final ClusteringRoleService clusteringService) {
        return Props.create(ShardRoleChangeListener.class, clusteringService);
    }
    @Override
    protected void handleReceive(Object message) throws Exception {
        if(message instanceof RoleChangeNotification) {
            RoleChangeNotification rcn = (RoleChangeNotification) message;
            LOG.debug("RoleChangeNotification message received:{}", rcn);
            onRoleChangeNotification(rcn);
        }
    }

    private void onRoleChangeNotification(RoleChangeNotification roleChanged) {
        LOG.info("Received role changed for {} from {} to {}", roleChanged.getMemberId(),
                roleChanged.getOldRole(), roleChanged.getNewRole());

        this.clusteringService.changeShardRole(getMemberForShard(roleChanged.getMemberId()), roleChanged.getMemberId(),
                ShardRole.valueOf(roleChanged.getOldRole()), ShardRole.valueOf(roleChanged.getNewRole()));
    }

    private String getMemberForShard(String shardId) {
        String memberId = shardToMemberMap.get(shardId);
        if (memberId == null) {
            ShardIdentifier shardIdentifier = ShardIdentifier.builder().fromShardIdString(shardId).build();
            memberId = shardIdentifier.getMemberName();
            shardToMemberMap.put(shardId, memberId);
        }
        return memberId;
    }
}
