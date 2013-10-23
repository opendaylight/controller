package org.opendaylight.controller.sal.binding.api.mount;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provider MountProviderService, this version allows access to MD-SAL
 * services specific for this mountpoint and registration / provision of
 * interfaces for mount point.
 * 
 * @author ttkacik
 * 
 */
public interface MountProviderService extends MountInstance {

    MountProviderInstance createMountPoint(InstanceIdentifier<?> path);
}
