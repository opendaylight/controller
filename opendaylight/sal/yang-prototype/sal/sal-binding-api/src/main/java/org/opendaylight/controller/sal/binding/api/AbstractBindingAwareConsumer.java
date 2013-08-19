package org.opendaylight.controller.sal.binding.api;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractBindingAwareConsumer implements BindingAwareConsumer,BundleActivator {

    @Override
    public final void start(BundleContext context) throws Exception {
        ServiceReference<BindingAwareBroker> brokerRef = context.getServiceReference(BindingAwareBroker.class);
        BindingAwareBroker broker = context.getService(brokerRef);
        broker.registerConsumer(this, context);
        //context.ungetService(brokerRef);
    }

    @Override
    public final  void stop(BundleContext context) throws Exception {
        // TODO Auto-generated method stub
        
    }

}
