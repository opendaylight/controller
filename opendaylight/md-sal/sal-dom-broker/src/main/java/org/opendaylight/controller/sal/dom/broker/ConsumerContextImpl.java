/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import java.util.Collection;
import java.util.concurrent.Future;

import javax.annotation.concurrent.GuardedBy;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.dom.broker.osgi.AbstractBrokerServiceProxy;
import org.opendaylight.controller.sal.dom.broker.osgi.ProxyFactory;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class ConsumerContextImpl implements ConsumerSession {

    private final ClassToInstanceMap<BrokerService> instantiatedServices = MutableClassToInstanceMap
            .create();
    private final Consumer consumer;

    private BrokerImpl broker = null;
    @GuardedBy("this")
    private boolean closed = false;

    public ConsumerContextImpl(final Consumer provider, final BrokerImpl brokerImpl) {
        broker = brokerImpl;
        consumer = provider;
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> rpc(DOMRpcIdentifier rpc, NormalizedNode<?, ?> input) {
        checkNotClosed();
        return broker.invokeRpcAsync(rpc, input);
    }

    @Override
    public <T extends BrokerService> T getService(final Class<T> service) {
        checkNotClosed();
        final T localProxy = instantiatedServices.getInstance(service);
        if (localProxy != null) {
            return localProxy;
        }
        final Optional<T> serviceImpl = broker.getGlobalService(service);
        if(serviceImpl.isPresent()) {
            final T ret = ProxyFactory.createProxy(null,serviceImpl.get());
            instantiatedServices.putInstance(service, ret);
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            this.closed = true;
        }

        Collection<BrokerService> toStop = instantiatedServices.values();
        for (BrokerService brokerService : toStop) {
            if (brokerService instanceof AbstractBrokerServiceProxy<?>) {
                ((AbstractBrokerServiceProxy<?>) brokerService).close();
            }
        }
        broker.consumerSessionClosed(this);
        broker = null;
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * @return the broker
     */
    protected final  BrokerImpl getBrokerChecked() {
        checkNotClosed();
        return broker;
    }

    /**
     * @return the _consumer
     */
    public Consumer getConsumer() {
        return consumer;
    }

    protected final void checkNotClosed()  {
        Preconditions.checkState(!closed, "Session is closed.");
    }
}
