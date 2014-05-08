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

import org.opendaylight.controller.md.sal.dom.broker.spi.rpc.RpcRoutingStrategy;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;

class RoutedRpcSelector implements RpcImplementation, AutoCloseable, Identifiable<RpcRoutingContext> {

    private final RpcRoutingStrategy strategy;
    private final Set<QName> supportedRpcs;
    private final RpcRoutingContext identifier;
    final ConcurrentMap<InstanceIdentifier, RoutedRpcRegImpl> implementations = new ConcurrentHashMap<>();
    private final SchemaAwareRpcBroker router;

    public RoutedRpcSelector(final RpcRoutingStrategy strategy, final SchemaAwareRpcBroker router) {
        super();
        this.strategy = strategy;
        supportedRpcs = ImmutableSet.of(strategy.getIdentifier());
        identifier = RpcRoutingContext.create(strategy.getContext(), strategy.getIdentifier());
        this.router = router;
    }

    @Override
    public RpcRoutingContext getIdentifier() {
        return identifier;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return supportedRpcs;
    }

    public RoutedRpcRegistration addRoutedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
        return new RoutedRpcRegImpl(rpcType, implementation, this);
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final CompositeNode input) {
        CompositeNode inputContainer = input.getFirstCompositeByName(QName.create(rpc,"input"));
        checkArgument(inputContainer != null, "Rpc payload must contain input element");
        SimpleNode<?> routeContainer = inputContainer.getFirstSimpleByName(strategy.getLeaf());
        checkArgument(routeContainer != null, "Leaf %s must be set with value", strategy.getLeaf());
        Object route = routeContainer.getValue();
        checkArgument(route instanceof InstanceIdentifier,
                      "The routed node %s is not an instance identifier", route);
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

    public void addPath(final QName context, final InstanceIdentifier path, final RoutedRpcRegImpl routedRpcRegImpl) {
        //checkArgument(strategy.getContext().equals(context),"Supplied context is not supported.");
        RoutedRpcRegImpl previous = implementations.put(path, routedRpcRegImpl);
        if (previous == null) {
            router.notifyPathAnnouncement(context,strategy.getIdentifier(), path);
        }

    }

    public void removePath(final QName context, final InstanceIdentifier path, final RoutedRpcRegImpl routedRpcRegImpl) {
        boolean removed = implementations.remove(path, routedRpcRegImpl);
        if (removed) {
            router.notifyPathWithdrawal(context, strategy.getIdentifier(), path);
        }
    }
}