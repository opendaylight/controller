/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Set
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.opendaylight.controller.sal.core.api.Broker
import org.opendaylight.controller.sal.core.api.BrokerService
import org.opendaylight.controller.sal.core.api.Consumer
import org.opendaylight.controller.sal.core.api.Provider
import org.opendaylight.controller.sal.core.spi.BrokerModule
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.osgi.framework.BundleContext
import org.slf4j.LoggerFactory
import org.opendaylight.controller.sal.dom.broker.spi.RpcRouter
import org.opendaylight.yangtools.concepts.ListenerRegistration
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener

public class BrokerImpl implements Broker, AutoCloseable {
    private static val log = LoggerFactory.getLogger(BrokerImpl);

    // Broker Generic Context
    private val Set<ConsumerContextImpl> sessions = Collections.synchronizedSet(new HashSet<ConsumerContextImpl>());
    private val Set<ProviderContextImpl> providerSessions = Collections.synchronizedSet(
        new HashSet<ProviderContextImpl>());

    // Implementation specific
    @Property
    private var ExecutorService executor = Executors.newFixedThreadPool(5);
    @Property
    private var BundleContext bundleContext;
    
    @Property
    private var AutoCloseable deactivator;

    @Property
    private var RpcRouter router;

    override registerConsumer(Consumer consumer, BundleContext ctx) {
        checkPredicates(consumer);
        log.info("Registering consumer " + consumer);
        val session = newSessionFor(consumer, ctx);
        consumer.onSessionInitiated(session);
        sessions.add(session);
        return session;
    }

    override registerProvider(Provider provider, BundleContext ctx) {
        checkPredicates(provider);

        val session = newSessionFor(provider, ctx);
        provider.onSessionInitiated(session);
        providerSessions.add(session);
        return session;
    }

    protected def Future<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
        val result = executor.submit([|router.invokeRpc(rpc, input)] as Callable<RpcResult<CompositeNode>>);
        return result;
    }

    // Validation
    private def void checkPredicates(Provider prov) {
        if (prov == null)
            throw new IllegalArgumentException("Provider should not be null.");
        for (ProviderContextImpl session : providerSessions) {
            if (prov.equals(session.getProvider()))
                throw new IllegalStateException("Provider already registered");
        }

    }

    private def void checkPredicates(Consumer cons) {
        if (cons == null)
            throw new IllegalArgumentException("Consumer should not be null.");
        for (ConsumerContextImpl session : sessions) {
            if (cons.equals(session.getConsumer()))
                throw new IllegalStateException("Consumer already registered");
        }
    }

    // Private Factory methods
    private def ConsumerContextImpl newSessionFor(Consumer provider, BundleContext ctx) {
        val ret = new ConsumerContextImpl(provider, ctx);
        ret.broker = this;
        return ret;
    }

    private def ProviderContextImpl newSessionFor(Provider provider, BundleContext ctx) {
        val ret = new ProviderContextImpl(provider, ctx);
        ret.broker = this;
        return ret;
    }

    protected def void consumerSessionClosed(ConsumerContextImpl consumerContextImpl) {
        sessions.remove(consumerContextImpl);
        providerSessions.remove(consumerContextImpl);
    }
    
    override close() throws Exception {
        deactivator?.close();
    }
    
}
