package org.opendaylight.controller.sal.core.api;

import java.util.Collection;
import java.util.Collections;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractConsumer implements Consumer, BundleActivator {

    Broker broker;
    ServiceReference<Broker> brokerRef;
    @Override
    public final void start(BundleContext context) throws Exception {
        brokerRef = context.getServiceReference(Broker.class);
        broker = context.getService(brokerRef);

        this.startImpl(context);

        broker.registerConsumer(this,context);
    }

    public abstract void startImpl(BundleContext context);

    @Override
    public final void stop(BundleContext context) throws Exception {
        broker = null;
        if(brokerRef != null) {
            context.ungetService(brokerRef);
        }
    }

    
    @Override
    public Collection<ConsumerFunctionality> getConsumerFunctionality() {
        return Collections.emptySet();
    }

}
