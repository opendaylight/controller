package org.opendaylight.controller.sal.core.api.mount;

import java.util.EventListener;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface MountProvisionService extends MountService {

    @Override
    public MountProvisionInstance getMountPoint(InstanceIdentifier path);
    
    MountProvisionInstance createMountPoint(InstanceIdentifier path);
    
    MountProvisionInstance createOrGetMountPoint(InstanceIdentifier path);
    
    ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener);
    
    public  interface MountProvisionListener extends EventListener {
        
        void onMountPointCreated(InstanceIdentifier path);
        
        void onMountPointRemoved(InstanceIdentifier path);
        
    }
}
