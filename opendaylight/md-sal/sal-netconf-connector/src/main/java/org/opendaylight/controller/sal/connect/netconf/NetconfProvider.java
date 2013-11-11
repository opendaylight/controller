package org.opendaylight.controller.sal.connect.netconf;

import java.util.Hashtable;

import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.osgi.framework.BundleContext;

public class NetconfProvider extends AbstractProvider {

    private NetconfDeviceManager netconfDeviceManager;

    @Override
    protected void startImpl(BundleContext context) {
        netconfDeviceManager = new NetconfDeviceManager();
        context.registerService(NetconfDeviceManager.class, netconfDeviceManager, new Hashtable<String,String>());
    }
    
    
    @Override
    public void onSessionInitiated(ProviderSession session) {
        MountProvisionService mountService = session.getService(MountProvisionService.class);
        
        
        netconfDeviceManager.setMountService(mountService);
        netconfDeviceManager.start();
    }

    @Override
    protected void stopImpl(BundleContext context) {
        
    }
}
