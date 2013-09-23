package org.opendaylight.controller.sal.dom.broker;

import java.util.Hashtable;

import org.opendaylight.controller.sal.core.api.Broker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class BrokerActivator implements BundleActivator {

    BrokerImpl broker;
    private ServiceRegistration<Broker> brokerReg;
    
    @Override
    public void start(BundleContext context) throws Exception {
        broker = new BrokerImpl();
        broker.setBundleContext(context);
        brokerReg = context.registerService(Broker.class, broker, new Hashtable<String,String>());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if(brokerReg != null) {
            brokerReg.unregister();
        }
    }
}
