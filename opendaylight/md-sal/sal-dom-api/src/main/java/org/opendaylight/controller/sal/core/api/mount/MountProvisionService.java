package org.opendaylight.controller.sal.core.api.mount;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface MountProvisionService extends MountService {

    @Override
    public MountProvisionInstance getMountPoint(InstanceIdentifier path);
    
    MountProvisionInstance createMountPoint(InstanceIdentifier path);
    
    MountProvisionInstance createOrGetMountPoint(InstanceIdentifier path);
}
