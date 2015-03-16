/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.controller.sal.dom.broker.spi.RpcRouter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BrokerImpl implements Broker, RpcProvisionRegistry, AutoCloseable {
    private final static Logger log = LoggerFactory.getLogger(BrokerImpl.class);

    // Broker Generic Context
    private final Set<ConsumerContextImpl> sessions = Collections
            .synchronizedSet(new HashSet<ConsumerContextImpl>());
    private final Set<ProviderContextImpl> providerSessions = Collections
            .synchronizedSet(new HashSet<ProviderContextImpl>());

    private AutoCloseable deactivator = null;

    private RpcRouter router = null;

    private final ClassToInstanceMap<BrokerService> services;

    public  BrokerImpl(final RpcRouter router,final ClassToInstanceMap<BrokerService> services) {
        this.router = Preconditions.checkNotNull(router, "RPC Router must not be null");
        this.services = ImmutableClassToInstanceMap.copyOf(services);
    }


    @Override
    public ConsumerSession registerConsumer(final Consumer consumer,
            final BundleContext ctx) {
        return registerConsumer(consumer);
    }

    @Override
    public ProviderSession registerProvider(final Provider provider,
            final BundleContext ctx) {
        return registerProvider(provider);
    }

    protected CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpcAsync(DOMRpcIdentifier rpc, NormalizedNode<?, ?> input) {
        return router.invokeRpc(rpc, input);
    }

    // Validation
    private void checkPredicates(final Provider prov) {
        Preconditions.checkNotNull(prov, "Provider should not be null.");
        for (ProviderContextImpl session : providerSessions) {
            if (prov.equals(session.getProvider())) {
                throw new IllegalStateException("Provider already registered");
            }
        }

    }

    private void checkPredicates(final Consumer cons) {
        Preconditions.checkNotNull(cons, "Consumer should not be null.");
        for (ConsumerContextImpl session : sessions) {
            if (cons.equals(session.getConsumer())) {
                throw new IllegalStateException("Consumer already registered");
            }
        }
    }

    // Private Factory methods
    private ConsumerContextImpl newSessionFor(final Consumer provider) {
        ConsumerContextImpl ret = new ConsumerContextImpl(provider, this);
        return ret;
    }

    private ProviderContextImpl newSessionFor(final Provider provider) {
        ProviderContextImpl ret = new ProviderContextImpl(provider, this);
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
    public <L extends RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return router.registerRouteChangeListener(listener);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return router.getSupportedRpcs();
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final DOMRpcIdentifier rpc, @Nullable final NormalizedNode<?, ?> input) {
        return router.invokeRpc(rpc, input);
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

    protected <T extends BrokerService> Optional<T> getGlobalService(final Class<T> service) {
        return Optional.fromNullable(services.getInstance(service));
    }


    @Override
    public ConsumerSession registerConsumer(Consumer consumer) {
        checkPredicates(consumer);
        log.trace("Registering consumer {}", consumer);
        final ConsumerContextImpl session = newSessionFor(consumer);
        consumer.onSessionInitiated(session);
        sessions.add(session);
        return session;
    }


    @Override
    public ProviderSession registerProvider(Provider provider) {
        checkPredicates(provider);
        final ProviderContextImpl session = newSessionFor(provider);
        provider.onSessionInitiated(session);
        providerSessions.add(session);
        return session;
    }


}
