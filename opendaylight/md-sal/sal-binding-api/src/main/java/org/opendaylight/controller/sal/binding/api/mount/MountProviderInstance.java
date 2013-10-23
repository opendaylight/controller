package org.opendaylight.controller.sal.binding.api.mount;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;

/**
 * Provider's version of Mount Point, this version allows access to MD-SAL
 * services specific for this mountpoint and registration / provision of
 * interfaces for mount point.
 * 
 * @author ttkacik
 * 
 */
public interface MountProviderInstance //
        extends //
        MountInstance, //
        DataProviderService, //
        RpcProviderRegistry, //
        NotificationProviderService {

}
