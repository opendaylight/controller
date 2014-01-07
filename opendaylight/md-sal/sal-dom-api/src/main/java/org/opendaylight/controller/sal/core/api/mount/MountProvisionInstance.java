package org.opendaylight.controller.sal.core.api.mount;

import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface MountProvisionInstance extends //
        MountInstance,//
        NotificationPublishService, //
        RpcProvisionRegistry,//
        DataProviderService {

    void setSchemaContext(SchemaContext optional);

}
