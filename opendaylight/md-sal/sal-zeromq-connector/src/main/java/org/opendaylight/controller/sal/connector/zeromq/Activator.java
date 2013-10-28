package org.opendaylight.controller.sal.connector.zeromq;

import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractProvider {
    
    ZeroMqRpcRouter router;
    
    @Override
    public void onSessionInitiated(ProviderSession session) {
        router = ZeroMqRpcRouter.getInstance();
        router.setBrokerSession(session);
        router.start();
    }
    
    @Override
    protected void stopImpl(BundleContext context) {
       router.stop();
    }

}
