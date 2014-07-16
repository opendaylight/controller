/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

import com.google.common.base.Optional;

abstract class RpcRoutingStrategy implements Identifiable<QName> {

    private final QName identifier;
    private static final QName CONTEXT_REFERENCE = QName.create("urn:opendaylight:yang:extension:yang-ext",
            "2013-07-09", "context-reference");

    private RpcRoutingStrategy(final QName identifier) {
        super();
        this.identifier = identifier;
    }

    public abstract QName getLeaf();

    public abstract QName getContext();

    @Override
    public QName getIdentifier() {
        return identifier;
    }

    public abstract boolean isContentBasedRouted();

    public static RpcRoutingStrategy from(final RpcDefinition rpc) {
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

    public static  Optional<QName> getRoutingContext(final DataSchemaNode schemaNode) {
        for (UnknownSchemaNode extension : schemaNode.getUnknownSchemaNodes()) {
            if (CONTEXT_REFERENCE.equals(extension.getNodeType())) {
                return Optional.fromNullable(extension.getQName());
            }
        }
        return Optional.absent();
    }

    private static RpcRoutingStrategy createRoutedStrategy(final RpcDefinition rpc, final QName context, final QName leafNode) {
        return new RoutedRpcStrategy(rpc.getQName(), context, leafNode);
    }



    private static RpcRoutingStrategy createGlobalStrategy(final RpcDefinition rpc) {
        GlobalRpcStrategy ret = new GlobalRpcStrategy(rpc.getQName());
        return ret;
    }

    static class RoutedRpcStrategy extends RpcRoutingStrategy {

        final QName context;
        private final QName leaf;

        private RoutedRpcStrategy(final QName identifier, final QName ctx, final QName leaf) {
            super(identifier);
            this.context = ctx;
            this.leaf = leaf;
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
        public boolean isContentBasedRouted() {
            return true;
        }
    }

    private static class GlobalRpcStrategy extends RpcRoutingStrategy {

        public GlobalRpcStrategy(final QName identifier) {
            super(identifier);
        }

        @Override
        public boolean isContentBasedRouted() {
            return false;
        }

        @Override
        public QName getContext() {
            throw new UnsupportedOperationException("Not routed strategy does not have context.");
        }

        @Override
        public QName getLeaf() {
            throw new UnsupportedOperationException("Not routed strategy does not have context.");
        }
    }
}