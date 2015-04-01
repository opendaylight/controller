package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.ChangeRoleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.ChangeRoleInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.ClusteringServiceProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.ShardRoleEnum;

/**
 * Created by kramesha on 3/31/15.
 */
public class ShardRoleChangeListener extends AbstractUntypedActor implements AutoCloseable {
    private ClusteringServiceProviderService clusteringService;

    public ShardRoleChangeListener(RpcProviderRegistry rpcProviderRegistry) {
        //if (rpcProviderRegistry != null) {
            this.clusteringService = rpcProviderRegistry.getRpcService(ClusteringServiceProviderService.class);
        //}

    }

    public static Props getProps(final RpcProviderRegistry rpcProviderRegistry) {
        return Props.create(ShardRoleChangeListener.class, rpcProviderRegistry);
    }
    @Override
    protected void handleReceive(Object message) throws Exception {
        if(message instanceof RoleChangeNotification) {
            onRoleChangeNotification((RoleChangeNotification) message);
        }
    }

    @Override
    public void close() throws Exception {

    }

    private void onRoleChangeNotification(RoleChangeNotification roleChanged) {
        LOG.info("Received role changed for {} from {} to {}", roleChanged.getMemberId(),
                roleChanged.getOldRole(), roleChanged.getNewRole());

        ChangeRoleInput changeRoleInput = new ChangeRoleInputBuilder()
                .setShardId(roleChanged.getMemberId())
                .setShardNewRole(ShardRoleEnum.valueOf(roleChanged.getNewRole()))
                .setShardOldRole(ShardRoleEnum.valueOf(roleChanged.getOldRole()))
                .build();
        this.clusteringService.changeRole(changeRoleInput);
    }
}
