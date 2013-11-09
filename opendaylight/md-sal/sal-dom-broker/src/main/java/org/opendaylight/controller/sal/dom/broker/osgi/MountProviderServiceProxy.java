package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class MountProviderServiceProxy extends AbstractBrokerServiceProxy<MountProvisionService> implements MountProvisionService{

    
    public MountProviderServiceProxy(ServiceReference<MountProvisionService> ref, MountProvisionService delegate) {
        super(ref, delegate);
    }

    public MountProvisionInstance getMountPoint(InstanceIdentifier path) {
        return getDelegate().getMountPoint(path);
    }

    public MountProvisionInstance createMountPoint(InstanceIdentifier path) {
        return getDelegate().createMountPoint(path);
    }

    public MountProvisionInstance createOrGetMountPoint(InstanceIdentifier path) {
        return getDelegate().createOrGetMountPoint(path);
    }
}
