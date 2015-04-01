package clusteringservice;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.service.provider.rev150316.ClusteringServiceProviderService;

/**
 * Created by kramesha on 3/25/15.
 */
public interface ClusteringService  extends AutoCloseable , ClusteringServiceProviderService {
    void setNotificationService(final NotificationProviderService notificationProviderService);
}
