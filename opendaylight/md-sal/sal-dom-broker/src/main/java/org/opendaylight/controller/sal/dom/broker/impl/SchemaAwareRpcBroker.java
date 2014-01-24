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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.md.sal.common.impl.routing.RoutingUtils;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.controller.sal.dom.broker.spi.RpcRouter;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class SchemaAwareRpcBroker implements RpcRouter, Identifiable<String>, RoutedRpcDefaultImplementation {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaAwareRpcBroker.class);

    private static final QName CONTEXT_REFERENCE = QName.create("urn:opendaylight:yang:extension:yang-ext",
            "2013-07-09", "context-reference");
    private final ListenerRegistry<RpcRegistrationListener> rpcRegistrationListeners = new ListenerRegistry<>();
    private final ListenerRegistry<RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> routeChangeListeners = new ListenerRegistry<>();
    

    private final String identifier;
    private final ConcurrentMap<QName, RpcImplementation> implementations = new ConcurrentHashMap<>();
    private RpcImplementation defaultImplementation;
    private SchemaContextProvider schemaProvider;
    private RoutedRpcDefaultImplementation defaultDelegate;

    public SchemaAwareRpcBroker(String identifier, SchemaContextProvider schemaProvider) {
        super();
        this.identifier = identifier;
        this.schemaProvider = schemaProvider;
    }

    public RpcImplementation getDefaultImplementation() {
        return defaultImplementation;
    }

    public void setDefaultImplementation(RpcImplementation defaultImplementation) {
        this.defaultImplementation = defaultImplementation;
    }

    public SchemaContextProvider getSchemaProvider() {
        return schemaProvider;
    }

    public void setSchemaProvider(SchemaContextProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

  public RoutedRpcDefaultImplementation getRoutedRpcDefaultDelegate() {
    return defaultDelegate;
  }

    @Override
  public void setRoutedRpcDefaultDelegate(RoutedRpcDefaultImplementation defaultDelegate) {
    this.defaultDelegate = defaultDelegate;
  }

  @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        checkArgument(rpcType != null, "RPC Type should not be null");
        checkArgument(implementation != null, "RPC Implementatoin should not be null");
        return getOrCreateRoutedRpcRouter(rpcType).addRoutedRpcImplementation(rpcType, implementation);
    }

    private RoutedRpcSelector getOrCreateRoutedRpcRouter(QName rpcType) {
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
            RoutingStrategy strategy = getRoutingStrategy(definition);
            checkState(strategy instanceof RoutedRpcStrategy, "Rpc %s is not routed.", rpcType);
            potential = new RoutedRpcSelector((RoutedRpcStrategy) strategy, this);
            implementations.put(rpcType, potential);
            return potential;
        }
    }

    private RoutedRpcSelector getRoutedRpcRouter(QName rpcType) {
        RpcImplementation potential = implementations.get(rpcType);
        if (potential != null) {
            checkState(potential instanceof RoutedRpcSelector, "Rpc %s is not routed.", rpcType);
            return (RoutedRpcSelector) potential;
        }
        return null;

    }

    @Override
    public RpcRegistration addRpcImplementation(QName rpcType, RpcImplementation implementation)
            throws IllegalArgumentException {
        checkArgument(rpcType != null, "RPC Type should not be null");
        checkArgument(implementation != null, "RPC Implementatoin should not be null");
        checkState(!hasRpcImplementation(rpcType), "Implementation already registered");
        RpcDefinition definition = findRpcDefinition(rpcType);
        checkArgument(!isRoutedRpc(definition), "RPC Type must not be routed.");
        GlobalRpcRegistration reg = new GlobalRpcRegistration(rpcType, implementation, this);
        implementations.putIfAbsent(rpcType, implementation);
        return reg;
    }

    private boolean isRoutedRpc(RpcDefinition definition) {
        return getRoutingStrategy(definition) instanceof RoutedRpcStrategy;
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(RpcRegistrationListener listener) {
        return rpcRegistrationListeners.register(listener);
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
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
        return findRpcImplemention(rpc).invokeRpc(rpc, input);
    }

    private RpcImplementation findRpcImplemention(QName rpc) {
        checkArgument(rpc != null, "Rpc name should not be null");
        RpcImplementation potentialImpl = implementations.get(rpc);
        if (potentialImpl != null) {
            return potentialImpl;
        }
        potentialImpl = defaultImplementation;
        checkState(potentialImpl != null, "Implementation is not available.");
        return potentialImpl;
    }

    private boolean hasRpcImplementation(QName rpc) {
        return implementations.containsKey(rpc);
    }

    private RpcDefinition findRpcDefinition(QName rpcType) {
        checkArgument(rpcType != null, "Rpc name must be supplied.");
        checkState(schemaProvider != null, "Schema Provider is not available.");
        SchemaContext ctx = schemaProvider.getSchemaContext();
        checkState(ctx != null, "YANG Schema Context is not available.");
        Module module = ctx.findModuleByNamespaceAndRevision(rpcType.getNamespace(), rpcType.getRevision());
        checkState(module != null, "YANG Module is not available.");
        return findRpcDefinition(rpcType, module.getRpcs());
    }

    static private RpcDefinition findRpcDefinition(QName rpcType, Set<RpcDefinition> rpcs) {
        checkState(rpcs != null, "Rpc schema is not available.");
        for (RpcDefinition rpc : rpcs) {
            if (rpcType.equals(rpc.getQName())) {
                return rpc;
            }
        }
        throw new IllegalArgumentException("Supplied Rpc Type is not defined.");
    }

    private RoutingStrategy getRoutingStrategy(RpcDefinition rpc) {
        ContainerSchemaNode input = rpc.getInput();
        if (input != null) {
            for (DataSchemaNode schemaNode : input.getChildNodes()) {
                Optional<QName> context = getRoutingContext(schemaNode);
                if (context.isPresent()) {
                    return createRoutedStrategy(rpc, context.get(), schemaNode.getQName());
                }
            }
        }
        return createGlobalStrategy(rpc);
    }

    private static RoutingStrategy createRoutedStrategy(RpcDefinition rpc, QName context, QName leafNode) {
        return new RoutedRpcStrategy(rpc.getQName(), context, leafNode);
    }

    private Optional<QName> getRoutingContext(DataSchemaNode schemaNode) {
        for (UnknownSchemaNode extension : schemaNode.getUnknownSchemaNodes()) {
            if (CONTEXT_REFERENCE.equals(extension.getNodeType())) {
                return Optional.fromNullable(extension.getQName());
            }
            ;
        }
        return Optional.absent();
    }

    private static RoutingStrategy createGlobalStrategy(RpcDefinition rpc) {
        GlobalRpcStrategy ret = new GlobalRpcStrategy(rpc.getQName());
        return ret;
    }

    @Override
    public RpcResult<CompositeNode> invokeRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input) {
      checkState(defaultDelegate != null);
      return defaultDelegate.invokeRpc(rpc, identifier, input);
    }

    private static abstract class RoutingStrategy implements Identifiable<QName> {

        private final QName identifier;

        public RoutingStrategy(QName identifier) {
            super();
            this.identifier = identifier;
        }

        @Override
        public QName getIdentifier() {
            return identifier;
        }
    }

    private static class GlobalRpcStrategy extends RoutingStrategy {

        public GlobalRpcStrategy(QName identifier) {
            super(identifier);
        }
    }

    private static class RoutedRpcStrategy extends RoutingStrategy {

        private final QName context;
        private final QName leaf;

        public RoutedRpcStrategy(QName identifier, QName ctx, QName leaf) {
            super(identifier);
            this.context = ctx;
            this.leaf = leaf;
        }

        public QName getContext() {
            return context;
        }

        public QName getLeaf() {
            return leaf;
        }
    }

    private static class RoutedRpcSelector implements RpcImplementation, AutoCloseable, Identifiable<QName> {

        private final RoutedRpcStrategy strategy;
        private final Set<QName> supportedRpcs;
        private RpcImplementation defaultDelegate;
        private final ConcurrentMap<InstanceIdentifier, RoutedRpcRegImpl> implementations = new ConcurrentHashMap<>();
        private SchemaAwareRpcBroker router;

        public RoutedRpcSelector(RoutedRpcStrategy strategy, SchemaAwareRpcBroker router) {
            super();
            this.strategy = strategy;
            supportedRpcs = ImmutableSet.of(strategy.getIdentifier());
            this.router = router;
        }

        @Override
        public QName getIdentifier() {
            return strategy.getIdentifier();
        }

        @Override
        public void close() throws Exception {

        }

        @Override
        public Set<QName> getSupportedRpcs() {
            return supportedRpcs;
        }

        public RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
            return new RoutedRpcRegImpl(rpcType, implementation, this);
        }

        @Override
        public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
            CompositeNode inputContainer = input.getFirstCompositeByName(QName.create(rpc,"input"));
            checkArgument(inputContainer != null, "Rpc payload must contain input element");
            SimpleNode<?> routeContainer = inputContainer.getFirstSimpleByName(strategy.getLeaf());
            checkArgument(routeContainer != null, "Leaf %s must be set with value", strategy.getLeaf());
            Object route = routeContainer.getValue();
            checkArgument(route instanceof InstanceIdentifier);
            RpcImplementation potential = null;
            if (route != null) {
                RoutedRpcRegImpl potentialReg = implementations.get(route);
                if (potentialReg != null) {
                    potential = potentialReg.getInstance();
                }
            }
            if (potential == null) {
                return router.invokeRpc(rpc, (InstanceIdentifier) route, input);
            }
            checkState(potential != null, "No implementation is available for rpc:%s path:%s", rpc, route);
            return potential.invokeRpc(rpc, input);
        }

        public void addPath(QName context, InstanceIdentifier path, RoutedRpcRegImpl routedRpcRegImpl) {
            //checkArgument(strategy.getContext().equals(context),"Supplied context is not supported.");
            RoutedRpcRegImpl previous = implementations.put(path, routedRpcRegImpl);
            if (previous == null) {
                router.notifyPathAnnouncement(context,strategy.getIdentifier(), path);
            }

        }

        public void removePath(QName context, InstanceIdentifier path, RoutedRpcRegImpl routedRpcRegImpl) {
            boolean removed = implementations.remove(path, routedRpcRegImpl);
            if (removed) {
                router.notifyPathWithdrawal(context, strategy.getIdentifier(), path);
            }
        }
    }

    private static class GlobalRpcRegistration extends AbstractObjectRegistration<RpcImplementation> implements
            RpcRegistration {
        private final QName type;
        private SchemaAwareRpcBroker router;

        public GlobalRpcRegistration(QName type, RpcImplementation instance, SchemaAwareRpcBroker router) {
            super(instance);
            this.type = type;
            this.router = router;
        }

        @Override
        public QName getType() {
            return type;
        }

        @Override
        protected void removeRegistration() {
            if (router != null) {
                router.remove(this);
                router = null;
            }
        }
    }

    private static class RoutedRpcRegImpl extends AbstractObjectRegistration<RpcImplementation> implements
            RoutedRpcRegistration {

        private final QName type;
        private RoutedRpcSelector router;

        public RoutedRpcRegImpl(QName rpcType, RpcImplementation implementation, RoutedRpcSelector routedRpcSelector) {
            super(implementation);
            this.type = rpcType;
            router = routedRpcSelector;
        }

        @Override
        public void registerPath(QName context, InstanceIdentifier path) {
            router.addPath(context, path, this);
        }

        @Override
        public void unregisterPath(QName context, InstanceIdentifier path) {
            router.removePath(context, path, this);
        }

        @Override
        protected void removeRegistration() {

        }

        @Override
        public QName getType() {
            return type;
        }

    }

    private void remove(GlobalRpcRegistration registration) {
        implementations.remove(registration.getType(), registration);
    }

    private void notifyPathAnnouncement(QName context, QName identifier, InstanceIdentifier path) {
        RpcRoutingContext contextWrapped = RpcRoutingContext.create(context, identifier);
        RouteChange<RpcRoutingContext, InstanceIdentifier> change = RoutingUtils.announcementChange(contextWrapped , path);
        for(ListenerRegistration<RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> routeListener : routeChangeListeners) {
            try {
                routeListener.getInstance().onRouteChange(change);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking onRouteChange for {}",routeListener.getInstance(),e);
                
            }
        }
        
    }

    

    private void notifyPathWithdrawal(QName context,QName identifier, InstanceIdentifier path) {
        RpcRoutingContext contextWrapped = RpcRoutingContext.create(context, identifier);
        RouteChange<RpcRoutingContext, InstanceIdentifier> change = RoutingUtils.removalChange(contextWrapped , path);
        for(ListenerRegistration<RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> routeListener : routeChangeListeners) {
            try {
                routeListener.getInstance().onRouteChange(change);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking onRouteChange for {}",routeListener.getInstance(),e);
            }
        }
    }
    
    @Override
    public <L extends RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(
            L listener) {
        return routeChangeListeners.registerWithType(listener);
    }
}
