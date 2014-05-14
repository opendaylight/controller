package org.opendaylight.controller.ping.plugin.internal;

import java.util.Collection;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
// import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.PingService;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;

public class PingProvider extends AbstractBindingAwareProvider {

    PingImpl pingImpl;

    public PingProvider() {
        pingImpl = new PingImpl();
    }

    @Override
    public Collection<? extends RpcService> getImplementations() {
        return null;
    }

    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return null;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        session.addRpcImplementation(PingService.class, pingImpl);
    }

    @Override
    protected void startImpl(BundleContext context) {
    }

}

