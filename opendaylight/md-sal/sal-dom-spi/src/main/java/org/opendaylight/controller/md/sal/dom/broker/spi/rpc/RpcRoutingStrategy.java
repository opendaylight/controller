/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.spi.rpc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

public abstract class RpcRoutingStrategy implements Identifiable<QName> {

    private static final QName CONTEXT_REFERENCE = QName.cachedReference(QName.create("urn:opendaylight:yang:extension:yang-ext",
            "2013-07-09", "context-reference"));
    private final QName identifier;

    private RpcRoutingStrategy(final QName identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    /**
     * Returns leaf QName in which RPC Route is stored
     *
     *
     * @return leaf QName in which RPC Route is stored
     * @throws UnsupportedOperationException If RPC is not content routed.
     *  ({@link #isContextBasedRouted()} returned <code>false</code>)
     */
    public abstract QName getLeaf();

    /**
     * Returns identity QName which represents RPC Routing context
     *
     * @return identity QName which represents RPC Routing context
     * @throws UnsupportedOperationException If RPC is not content routed.
     *  ({@link #isContextBasedRouted()} returned <code>false</code>)
     */
    public abstract QName getContext();

    @Override
    public final QName getIdentifier() {
        return identifier;
    }

    /**
     * Returns true if RPC is routed by context.
     *
     * @return true if RPC is routed by content.
     */
    public abstract boolean isContextBasedRouted();

    public static RpcRoutingStrategy from(final RpcDefinition rpc) {
        ContainerSchemaNode input = rpc.getInput();
        if (input != null) {
            for (DataSchemaNode schemaNode : input.getChildNodes()) {
                Optional<QName> context = getRoutingContext(schemaNode);
                if (context.isPresent()) {
                    return new RoutedRpcStrategy(rpc.getQName(), context.get(), schemaNode.getQName());
                }
            }
        }
        return new GlobalRpcStrategy(rpc.getQName());
    }

    public static Optional<QName> getRoutingContext(final DataSchemaNode schemaNode) {
        for (UnknownSchemaNode extension : schemaNode.getUnknownSchemaNodes()) {
            if (CONTEXT_REFERENCE.equals(extension.getNodeType())) {
                return Optional.fromNullable(extension.getQName());
            }
        }
        return Optional.absent();
    }

    private static final class RoutedRpcStrategy extends RpcRoutingStrategy {
        private final QName context;
        private final QName leaf;

        private RoutedRpcStrategy(final QName identifier, final QName ctx, final QName leaf) {
            super(identifier);
            this.context = Preconditions.checkNotNull(ctx);
            this.leaf = Preconditions.checkNotNull(leaf);
        }

        @Override
        public QName getContext() {
            return context;
        }

        @Override
        public QName getLeaf() {
            return leaf;
        }

        @Override
        public boolean isContextBasedRouted() {
            return true;
        }
    }

    private static final class GlobalRpcStrategy extends RpcRoutingStrategy {

        public GlobalRpcStrategy(final QName identifier) {
            super(identifier);
        }

        @Override
        public boolean isContextBasedRouted() {
            return false;
        }

        @Override
        public QName getContext() {
            throw new UnsupportedOperationException("Non-routed strategy does not have a context");
        }

        @Override
        public QName getLeaf() {
            throw new UnsupportedOperationException("Non-routed strategy does not have a context");
        }
    }
}