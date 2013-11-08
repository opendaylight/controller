package org.opendaylight.controller.sal.dom.broker

import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession
import org.opendaylight.controller.sal.core.api.BrokerService
import org.opendaylight.controller.sal.core.api.Consumer
import org.osgi.framework.BundleContext
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.sal.dom.broker.osgi.AbstractBrokerServiceProxy
import com.google.common.collect.ClassToInstanceMap
import com.google.common.collect.MutableClassToInstanceMap
import org.opendaylight.controller.sal.dom.broker.osgi.ProxyFactory

class ConsumerContextImpl implements ConsumerSession {

    @Property
    private val Consumer consumer;

    @Property
    private var BrokerImpl broker;

    private val ClassToInstanceMap<BrokerService> instantiatedServices = MutableClassToInstanceMap.create();
    private boolean closed = false;

    private BundleContext context;

    public new(Consumer consumer, BundleContext ctx) {
        this._consumer = consumer;
        this.context = ctx;
    }

    override rpc(QName rpc, CompositeNode input) {
        return broker.invokeRpc(rpc, input);
    }

    override <T extends BrokerService> T getService(Class<T> service) {
        val localProxy = instantiatedServices.getInstance(service);
        if(localProxy != null) {
            return localProxy;
        }
        val serviceRef = context.getServiceReference(service);
        if(serviceRef == null) {
            return null;
        }
        val serviceImpl = context.getService(serviceRef);
        
        
        val ret = ProxyFactory.createProxy(serviceRef,serviceImpl);
        if(ret != null) {
            instantiatedServices.putInstance(service, ret);
        }
        return ret;
    }

    override close() {
        val toStop = instantiatedServices.values();
        this.closed = true;
        for (BrokerService brokerService : toStop) {
            if(brokerService instanceof AbstractBrokerServiceProxy<?>) {
                (brokerService as AutoCloseable).close();
            } 
        }
        broker.consumerSessionClosed(this);
    }

    override isClosed() {
        return closed;
    }
}
