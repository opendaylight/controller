package org.opendaylight.controller.sal.core.api;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractProvider implements BundleActivator, Provider {

    private ServiceReference<Broker> brokerRef;
    private Broker broker;

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public final void start(BundleContext context) throws Exception {
        brokerRef = context.getServiceReference(Broker.class);
        broker = context.getService(brokerRef);

        this.startImpl(context);

        broker.registerProvider(this,context);
    }

    public abstract void startImpl(BundleContext context);

    @Override
    public final void stop(BundleContext context) throws Exception {
        // TODO Auto-generated method stub

    }

}
