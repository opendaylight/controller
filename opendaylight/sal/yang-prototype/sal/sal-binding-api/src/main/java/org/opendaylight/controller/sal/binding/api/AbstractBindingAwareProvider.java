package org.opendaylight.controller.sal.binding.api;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractBindingAwareProvider implements BindingAwareProvider, BundleActivator {

    @Override
    public final void start(BundleContext context) throws Exception {
            ServiceReference<BindingAwareBroker> brokerRef = context.getServiceReference(BindingAwareBroker.class);
            BindingAwareBroker broker = context.getService(brokerRef);
            broker.registerProvider(this, context);
    }

    @Override
    public final void stop(BundleContext context) throws Exception {
            
            
    }
}
