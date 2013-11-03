package org.opendaylight.controller.sal.dom.broker


import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import static com.google.common.base.Preconditions.*;

class MountPointManagerImpl implements MountProvisionService {
    
    ConcurrentMap<InstanceIdentifier,MountPointImpl> mounts = new ConcurrentHashMap();
    
    override createMountPoint(InstanceIdentifier path) {
        checkState(!mounts.containsKey(path),"Mount already created");
        val mount = new MountPointImpl(path);
        mounts.put(path,mount);
    }
    
    
    override createOrGetMountPoint(InstanceIdentifier path) {
        val mount = mounts.get(path);
        if(mount === null) {
            return createMountPoint(path)
        }
        return mount;
    }
    
    
    override getMountPoint(InstanceIdentifier path) {
        mounts.get(path);
    }
    
    
}
