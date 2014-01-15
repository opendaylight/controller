package org.opendaylight.controller.sal.binding.api.mount;

import java.util.EventListener;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provider MountProviderService, this version allows access to MD-SAL services
 * specific for this mountpoint and registration / provision of interfaces for
 * mount point.
 * 
 * @author ttkacik
 * 
 */
public interface MountProviderService extends MountService {
    
    @Override
    public MountProviderInstance getMountPoint(InstanceIdentifier<?> path);

    MountProviderInstance createMountPoint(InstanceIdentifier<?> path);

    MountProviderInstance createOrGetMountPoint(InstanceIdentifier<?> path);

    ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener);

    public interface MountProvisionListener extends EventListener {

        void onMountPointCreated(InstanceIdentifier<?> path);

        void onMountPointRemoved(InstanceIdentifier<?> path);

    }
}
