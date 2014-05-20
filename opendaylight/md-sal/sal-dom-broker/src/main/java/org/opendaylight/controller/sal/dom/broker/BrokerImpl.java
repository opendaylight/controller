/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.sal.dom.broker.spi.RpcRouter;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;

public class BrokerImpl implements Broker, RpcProvisionRegistry, AutoCloseable {
    private final static Logger log = LoggerFactory.getLogger(BrokerImpl.class);

    // Broker Generic Context
    private final Set<ConsumerContextImpl> sessions = Collections
            .synchronizedSet(new HashSet<ConsumerContextImpl>());
    private final Set<ProviderContextImpl> providerSessions = Collections
            .synchronizedSet(new HashSet<ProviderContextImpl>());

    private BundleContext bundleContext = null;

    private AutoCloseable deactivator = null;

    private RpcRouter router = null;

    @Override
    public ConsumerSession registerConsumer(final Consumer consumer,
            final BundleContext ctx) {
        checkPredicates(consumer);
        log.trace("Registering consumer {}", consumer);
        final ConsumerContextImpl session = newSessionFor(consumer, ctx);
        consumer.onSessionInitiated(session);
        sessions.add(session);
        return session;
    }

    @Override
    public ProviderSession registerProvider(final Provider provider,
            final BundleContext ctx) {
        checkPredicates(provider);
        final ProviderContextImpl session = newSessionFor(provider, ctx);
        provider.onSessionInitiated(session);
        providerSessions.add(session);
        return session;
    }

    protected Future<RpcResult<CompositeNode>> invokeRpcAsync(final QName rpc,
            final CompositeNode input) {
        return router.invokeRpc(rpc, input);
    }

    // Validation
    private void checkPredicates(final Provider prov) {
        Preconditions.checkNotNull(prov, "Provider should not be null.");
        for (ProviderContextImpl session : providerSessions) {
            if (prov.equals(session.getProvider()))
                throw new IllegalStateException("Provider already registered");
        }

    }

    private void checkPredicates(final Consumer cons) {
        Preconditions.checkNotNull(cons, "Consumer should not be null.");
        for (ConsumerContextImpl session : sessions) {
            if (cons.equals(session.getConsumer()))
                throw new IllegalStateException("Consumer already registered");
        }
    }

    // Private Factory methods
    private ConsumerContextImpl newSessionFor(final Consumer provider,
            final BundleContext ctx) {
        ConsumerContextImpl ret = new ConsumerContextImpl(provider, ctx);
        ret.setBroker(this);
        return ret;
    }

    private ProviderContextImpl newSessionFor(final Provider provider,
            final BundleContext ctx) {
        ProviderContextImpl ret = new ProviderContextImpl(provider, ctx);
        ret.setBroker(this);
        return ret;
    }

    protected void consumerSessionClosed(
            final ConsumerContextImpl consumerContextImpl) {
        sessions.remove(consumerContextImpl);
        providerSessions.remove(consumerContextImpl);
    }

    @Override
    public void close() throws Exception {
        if (deactivator != null) {
            deactivator.close();
            deactivator = null;
        }
    }

    @Override
    public RpcRegistration addRpcImplementation(final QName rpcType,
            final RpcImplementation implementation)
            throws IllegalArgumentException {
        return router.addRpcImplementation(rpcType, implementation);
    }

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(
            final QName rpcType, final RpcImplementation implementation) {
        return router.addRoutedRpcImplementation(rpcType, implementation);
    }

    @Override
    public void setRoutedRpcDefaultDelegate(
            final RoutedRpcDefaultImplementation defaultImplementation) {
        router.setRoutedRpcDefaultDelegate(defaultImplementation);
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(
            final RpcRegistrationListener listener) {
        return router.addRpcRegistrationListener(listener);
    }

    @Override
    public <L extends RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return router.registerRouteChangeListener(listener);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return router.getSupportedRpcs();
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(
            final QName rpc, final CompositeNode input) {
        return router.invokeRpc(rpc, input);
    }

    /**
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * @param bundleContext
     *            the bundleContext to set
     */
    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @return the deactivator
     */
    public AutoCloseable getDeactivator() {
        return deactivator;
    }

    /**
     * @param deactivator
     *            the deactivator to set
     */
    public void setDeactivator(final AutoCloseable deactivator) {
        this.deactivator = deactivator;
    }

    /**
     * @return the router
     */
    public RpcRouter getRouter() {
        return router;
    }

    /**
     * @param router
     *            the router to set
     */
    public void setRouter(final RpcRouter router) {
        this.router = router;
    }
}
