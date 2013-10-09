package org.opendaylight.controller.sal.dom.broker

import java.util.Collections
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession
import java.util.HashMap
import org.opendaylight.controller.sal.core.api.BrokerService
import org.opendaylight.controller.sal.core.api.Consumer
import org.osgi.framework.BundleContext
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.CompositeNode

class ConsumerContextImpl implements ConsumerSession {

    @Property
    private val Consumer consumer;

    @Property
    private var BrokerImpl broker;

    private val instantiatedServices = Collections.synchronizedMap(
        new HashMap<Class<? extends BrokerService>, BrokerService>());
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
        val potential = instantiatedServices.get(service);
        if(potential != null) {
            val ret = potential as T;
            return ret;
        }
        val ret = broker.serviceFor(service, this);
        if(ret != null) {
            instantiatedServices.put(service, ret);
        }
        return ret;
    }

    override close() {
        val toStop = instantiatedServices.values();
        this.closed = true;
        for (BrokerService brokerService : toStop) {
            //brokerService.closeSession();
        }
        broker.consumerSessionClosed(this);
    }

    override isClosed() {
        return closed;
    }
}
