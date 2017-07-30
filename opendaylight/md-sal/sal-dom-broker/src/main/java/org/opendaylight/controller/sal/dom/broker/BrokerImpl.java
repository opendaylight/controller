/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerImpl implements Broker, DOMRpcProviderService, DOMRpcService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerImpl.class);

    // Broker Generic Context
    private final Set<ConsumerContextImpl> sessions = Collections
            .synchronizedSet(new HashSet<ConsumerContextImpl>());
    private final Set<ProviderContextImpl> providerSessions = Collections
            .synchronizedSet(new HashSet<ProviderContextImpl>());

    private AutoCloseable deactivator;

    private final DOMRpcService rpcService;
    private final DOMRpcProviderService rpcProvider;

    private final ClassToInstanceMap<BrokerService> services;

    public BrokerImpl(final DOMRpcRouter router,final ClassToInstanceMap<BrokerService> services) {
        this(router, router, services);
    }

    public BrokerImpl(final DOMRpcService rpcService, final DOMRpcProviderService rpcProvider,
            final ClassToInstanceMap<BrokerService> services) {
        this.rpcService = Preconditions.checkNotNull(rpcService, "DOMRpcService must not be null");
        this.rpcProvider = Preconditions.checkNotNull(rpcProvider, "DOMRpcProviderService must not be null");
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

    // Validation
    private void checkPredicates(final Provider prov) {
        Preconditions.checkNotNull(prov, "Provider should not be null.");
        for (final ProviderContextImpl session : providerSessions) {
            if (prov.equals(session.getProvider())) {
                throw new IllegalStateException("Provider already registered");
            }
        }

    }

    private void checkPredicates(final Consumer cons) {
        Preconditions.checkNotNull(cons, "Consumer should not be null.");
        for (final ConsumerContextImpl session : sessions) {
            if (cons.equals(session.getConsumer())) {
                throw new IllegalStateException("Consumer already registered");
            }
        }
    }

    // Private Factory methods
    private ConsumerContextImpl newSessionFor(final Consumer provider) {
        final ConsumerContextImpl ret = new ConsumerContextImpl(provider, this);
        return ret;
    }

    private ProviderContextImpl newSessionFor(final Provider provider) {
        final ProviderContextImpl ret = new ProviderContextImpl(provider, this);
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

    protected <T extends BrokerService> Optional<T> getGlobalService(final Class<T> service) {
        return Optional.fromNullable(services.getInstance(service));
    }


    @Override
    public ConsumerSession registerConsumer(final Consumer consumer) {
        checkPredicates(consumer);
        LOG.trace("Registering consumer {}", consumer);
        final ConsumerContextImpl session = newSessionFor(consumer);
        consumer.onSessionInitiated(session);
        sessions.add(session);
        return session;
    }


    @Override
    public ProviderSession registerProvider(final Provider provider) {
        checkPredicates(provider);
        final ProviderContextImpl session = newSessionFor(provider);
        provider.onSessionInitiated(session);
        providerSessions.add(session);
        return session;
    }

    @Nonnull
    @Override
    public <T extends DOMRpcImplementation> DOMRpcImplementationRegistration<T> registerRpcImplementation(@Nonnull final T implementation, @Nonnull final Set<DOMRpcIdentifier> rpcs) {
        return rpcProvider.registerRpcImplementation(implementation, rpcs);
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final SchemaPath type, @Nullable final NormalizedNode<?, ?> input) {
        return rpcService.invokeRpc(type, input);
    }

    @Nonnull
    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull final T listener) {
        return rpcService.registerRpcListener(listener);
    }
}
