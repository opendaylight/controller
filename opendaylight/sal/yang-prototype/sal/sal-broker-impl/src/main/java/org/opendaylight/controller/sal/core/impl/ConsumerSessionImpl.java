/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;


public class ConsumerSessionImpl implements ConsumerSession {

    private final BrokerImpl broker;
    private final Consumer consumer;

    private Map<Class<? extends BrokerService>, BrokerService> instantiatedServices = new HashMap<Class<? extends BrokerService>, BrokerService>();
    private boolean closed = false;

    public Consumer getConsumer() {
        return consumer;
    }

    public ConsumerSessionImpl(BrokerImpl broker, Consumer consumer) {
        this.broker = broker;
        this.consumer = consumer;
    }

    @Override
    public Future<RpcResult<CompositeNode>> rpc(QName rpc, CompositeNode input) {
        return broker.invokeRpc(rpc, input);
    }

    @Override
    public <T extends BrokerService> T getService(Class<T> service) {
        BrokerService potential = instantiatedServices.get(service);
        if (potential != null) {
            @SuppressWarnings("unchecked")
            T ret = (T) potential;
            return ret;
        }
        T ret = this.broker.serviceFor(service, this);
        if (ret != null) {
            instantiatedServices.put(service, ret);
        }
        return ret;
    }

    @Override
    public void close() {
        Collection<BrokerService> toStop = instantiatedServices.values();
        this.closed  = true;
        for (BrokerService brokerService : toStop) {
            brokerService.closeSession();
        }
        broker.consumerSessionClosed(this);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

}
