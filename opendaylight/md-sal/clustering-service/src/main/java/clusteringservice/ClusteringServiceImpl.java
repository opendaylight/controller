package clusteringservice;

import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.ChangeRoleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.RoleChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.RoleChangedBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Created by kramesha on 3/25/15.
 */
public class ClusteringServiceImpl implements ClusteringService {
    private NotificationProviderService notificationProviderService;

    @Override
    public Future<RpcResult<Void>> changeRole(ChangeRoleInput input) {
        RoleChanged roleChangedNotification = new RoleChangedBuilder()
                .setShardId(input.getShardId())
                .setShardOldRole(input.getShardOldRole())
                .setShardNewRole(input.getShardNewRole())
                .build();
        notificationProviderService.publish(roleChangedNotification);

        SettableFuture<RpcResult<Void>> future = SettableFuture.create();
        future.set(RpcResultBuilder.<Void>success().build());
        return future;
    }


    @Override
    public void setNotificationService(NotificationProviderService notificationProviderService) {
        this.notificationProviderService = notificationProviderService;
    }

    @Override
    public void close() throws Exception {

    }
}
