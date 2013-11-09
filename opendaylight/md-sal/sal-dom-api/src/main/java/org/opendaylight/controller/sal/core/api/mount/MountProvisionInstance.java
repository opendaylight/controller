package org.opendaylight.controller.sal.core.api.mount;

import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;

public interface MountProvisionInstance extends //
        MountInstance,//
        NotificationPublishService, //
        RpcProvisionRegistry,//
        DataProviderService {

}
