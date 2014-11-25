/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.controller.md.sal.dom.broker.spi.rpc.RpcRoutingStrategy;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementationUnavailableException;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.controller.sal.dom.broker.spi.RpcRouter;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC broker responsible for routing requests to remote systems.
 */
public class SchemaAwareRpcBroker implements RpcRouter, Identifiable<String>, RoutedRpcDefaultImplementation {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaAwareRpcBroker.class);


    private final ListenerRegistry<RpcRegistrationListener> rpcRegistrationListeners = new ListenerRegistry<>();
    private final ListenerRegistry<RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>> routeChangeListeners = new ListenerRegistry<>();


    private final String identifier;
    private final ConcurrentMap<QName, RpcImplementation> implementations = new ConcurrentHashMap<>();
    private RpcImplementation defaultImplementation;
    private SchemaContextProvider schemaProvider;
    private RoutedRpcDefaultImplementation defaultDelegate;

    public SchemaAwareRpcBroker(final String identifier, final SchemaContextProvider schemaProvider) {
        super();
        this.identifier = identifier;
        this.schemaProvider = schemaProvider;
    }

    public RpcImplementation getDefaultImplementation() {
        return defaultImplementation;
    }

    public void setDefaultImplementation(final RpcImplementation defaultImplementation) {
        this.defaultImplementation = defaultImplementation;
    }

    public SchemaContextProvider getSchemaProvider() {
        return schemaProvider;
    }

    public void setSchemaProvider(final SchemaContextProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

    public RoutedRpcDefaultImplementation getRoutedRpcDefaultDelegate() {
        return defaultDelegate;
    }

    @Override
    public void setRoutedRpcDefaultDelegate(final RoutedRpcDefaultImplementation defaultDelegate) {
        this.defaultDelegate = defaultDelegate;
    }

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
        checkArgument(rpcType != null, "RPC Type should not be null");
        checkArgument(implementation != null, "RPC Implementatoin should not be null");
        return getOrCreateRoutedRpcRouter(rpcType).addRoutedRpcImplementation(rpcType, implementation);
    }

    private RoutedRpcSelector getOrCreateRoutedRpcRouter(final QName rpcType) {
        RoutedRpcSelector potential = getRoutedRpcRouter(rpcType);
        if (potential != null) {
            return potential;
        }
        synchronized (implementations) {
            potential = getRoutedRpcRouter(rpcType);
            if (potential != null) {
                return potential;
            }
            RpcDefinition definition = findRpcDefinition(rpcType);
            RpcRoutingStrategy strategy = RpcRoutingStrategy.from(definition);
            checkState(strategy.isContextBasedRouted(), "Rpc %s is not routed.", rpcType);
            potential = new RoutedRpcSelector( strategy, this);
            implementations.put(rpcType, potential);
            return potential;
        }
    }

    private RoutedRpcSelector getRoutedRpcRouter(final QName rpcType) {
        RpcImplementation potential = implementations.get(rpcType);
        if (potential != null) {
            checkState(potential instanceof RoutedRpcSelector, "Rpc %s is not routed.", rpcType);
            return (RoutedRpcSelector) potential;
        }
        return null;

    }

    @Override
    public RpcRegistration addRpcImplementation(final QName rpcType, final RpcImplementation implementation)
            throws IllegalArgumentException {
        checkArgument(rpcType != null, "RPC Type should not be null");
        checkArgument(implementation != null, "RPC Implementatoin should not be null");
        checkState(!hasRpcImplementation(rpcType), "Implementation already registered");
        RpcDefinition definition = findRpcDefinition(rpcType);
        checkArgument(!RpcRoutingStrategy.from(definition).isContextBasedRouted(), "RPC Type must not be content routed.");
        GlobalRpcRegistration reg = new GlobalRpcRegistration(rpcType, implementation, this);
        final RpcImplementation previous = implementations.putIfAbsent(rpcType, implementation);
        Preconditions.checkState(previous == null, "Rpc %s is already registered.",rpcType);
        notifyRpcAdded(rpcType);
        return reg;
    }

    private void notifyRpcAdded(final QName rpcType) {
        for (ListenerRegistration<RpcRegistrationListener> listener : rpcRegistrationListeners) {
            try {
                listener.getInstance().onRpcImplementationAdded(rpcType);
            } catch (Exception ex) {
                LOG.error("Unhandled exception during invoking listener {}", listener.getInstance(), ex);
            }

        }
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(final RpcRegistrationListener listener) {
        ListenerRegistration<RpcRegistrationListener> reg = rpcRegistrationListeners.register(listener);
        for (QName impl : implementations.keySet()) {
            listener.onRpcImplementationAdded(impl);
        }
        return reg;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return ImmutableSet.copyOf(implementations.keySet());
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final CompositeNode input) {
        return findRpcImplemention(rpc).invokeRpc(rpc, input);
    }

    private RpcImplementation findRpcImplemention(final QName rpc) {
        checkArgument(rpc != null, "Rpc name should not be null");
        RpcImplementation potentialImpl = implementations.get(rpc);
        if (potentialImpl != null) {
            return potentialImpl;
        }

        potentialImpl = defaultImplementation;
        if( potentialImpl == null ) {
            throw new UnsupportedOperationException( "No implementation for this operation is available." );
        }

        return potentialImpl;
    }

    private boolean hasRpcImplementation(final QName rpc) {
        return implementations.containsKey(rpc);
    }

    private RpcDefinition findRpcDefinition(final QName rpcType) {
        checkArgument(rpcType != null, "Rpc name must be supplied.");
        checkState(schemaProvider != null, "Schema Provider is not available.");
        SchemaContext ctx = schemaProvider.getSchemaContext();
        checkState(ctx != null, "YANG Schema Context is not available.");
        Module module = ctx.findModuleByNamespaceAndRevision(rpcType.getNamespace(), rpcType.getRevision());
        checkState(module != null, "YANG Module is not available.");
        return findRpcDefinition(rpcType, module.getRpcs());
    }

    static private RpcDefinition findRpcDefinition(final QName rpcType, final Set<RpcDefinition> rpcs) {
        checkState(rpcs != null, "Rpc schema is not available.");
        for (RpcDefinition rpc : rpcs) {
            if (rpcType.equals(rpc.getQName())) {
                return rpc;
            }
        }
        throw new IllegalArgumentException("Supplied Rpc Type is not defined.");
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final YangInstanceIdentifier route, final CompositeNode input) {
        if (defaultDelegate == null) {
            return Futures.immediateFailedCheckedFuture(new RpcImplementationUnavailableException("No RPC implementation found"));
        }

        LOG.debug("Forwarding RPC {} path {} to delegate {}", rpc, route);
        return defaultDelegate.invokeRpc(rpc, route, input);
    }

    void remove(final GlobalRpcRegistration registration) {
        implementations.remove(registration.getType(), registration);
    }

    void notifyPathAnnouncement(final QName context, final QName identifier, final YangInstanceIdentifier path) {
        RpcRoutingContext contextWrapped = RpcRoutingContext.create(context, identifier);
        RouteChange<RpcRoutingContext, YangInstanceIdentifier> change = RoutingUtils.announcementChange(contextWrapped , path);
        for(ListenerRegistration<RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>> routeListener : routeChangeListeners) {
            try {
                routeListener.getInstance().onRouteChange(change);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking onRouteChange for {}",routeListener.getInstance(),e);
            }
        }

    }

    void notifyPathWithdrawal(final QName context,final QName identifier, final YangInstanceIdentifier path) {
        RpcRoutingContext contextWrapped = RpcRoutingContext.create(context, identifier);
        RouteChange<RpcRoutingContext, YangInstanceIdentifier> change = RoutingUtils.removalChange(contextWrapped , path);
        for(ListenerRegistration<RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>> routeListener : routeChangeListeners) {
            try {
                routeListener.getInstance().onRouteChange(change);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking onRouteChange for {}",routeListener.getInstance(),e);
            }
        }
    }

    @Override
    public <L extends RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        ListenerRegistration<L> reg = routeChangeListeners.registerWithType(listener);
        RouteChange<RpcRoutingContext, YangInstanceIdentifier> initial = createInitialRouteChange();
        try {
        listener.onRouteChange(initial);
        } catch (Exception e) {
            LOG.error("Unhandled exception during sending initial route change event {} to {}",initial,listener, e);
        }
        return reg;
    }

    private RouteChange<RpcRoutingContext, YangInstanceIdentifier> createInitialRouteChange() {
        FluentIterable<RoutedRpcSelector> rpcSelectors = FluentIterable.from(implementations.values()).filter(RoutedRpcSelector.class);


        ImmutableMap.Builder<RpcRoutingContext, Set<YangInstanceIdentifier>> announcements = ImmutableMap.builder();
        ImmutableMap.Builder<RpcRoutingContext, Set<YangInstanceIdentifier>> removals = ImmutableMap.builder();
        for (RoutedRpcSelector routedRpcSelector : rpcSelectors) {
            final RpcRoutingContext context = routedRpcSelector.getIdentifier();
            final Set<YangInstanceIdentifier> paths = ImmutableSet.copyOf(routedRpcSelector.implementations.keySet());
            announcements.put(context, paths);
        }
        return RoutingUtils.change(announcements.build(), removals.build());
    }
}
