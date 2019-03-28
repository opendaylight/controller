/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class NormalizedNodeBuilderWrapper<I extends PathArgument, T extends NormalizedNodeBuilder<I, ?, ?>> {
    static final class Container<I extends PathArgument, T extends NormalizedNodeContainerBuilder<I, ?, ?, ?>>
            extends NormalizedNodeBuilderWrapper<I, T> {

        Container(final T builder, final I identifier, final DataSchemaContextNode<?> schemaNode) {
            super(builder, identifier, schemaNode);
        }

        @Override
        void addChild(final NormalizedNode<?, ?> child) {
            ((NormalizedNodeContainerBuilder)builder()).addChild(child);
        }

        @Override
        void setValue(final Object value) {
            throw new IllegalStateException("Attempted to set value " + value + " on container builder " + builder());
        }

        @Override
        DataSchemaContextNode<?> startChild(final DataSchemaContextNode<?> schemaNode, final PathArgument child) {
            return schemaNode == null ? null : schemaNode.getChild(child);
        }
    }

    static final class Value<I extends PathArgument, T extends NormalizedNodeBuilder<I, ?, ?>>
            extends NormalizedNodeBuilderWrapper<I, T> {

        Value(final T builder, final I identifier, final DataSchemaContextNode<?> schemaNode) {
            super(builder, identifier, schemaNode);
        }

        @Override
        void addChild(final NormalizedNode<?, ?> child) {
            throw new IllegalStateException("Attempted to add child " + child + " to non-container builder "
                    + builder());
        }

        @Override
        void setValue(final Object value) {
            builder().withValue(value);
        }

        @Override
        DataSchemaContextNode<?> startChild(final DataSchemaContextNode<?> schemaNode, final PathArgument child) {
            throw new IllegalStateException("Attempted to lookup child " + child + " in non-container " + schemaNode);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeBuilderWrapper.class);

    private final T builder;
    private final I identifier;
    private final DataSchemaContextNode<?> schemaNode;

    NormalizedNodeBuilderWrapper(final T builder, final I identifier,
            final @Nullable DataSchemaContextNode<?> schemaNode) {
        this.builder = requireNonNull(builder);
        this.identifier = requireNonNull(identifier);
        builder.withNodeIdentifier(identifier);
        this.schemaNode = schemaNode;
    }

    @SuppressWarnings("rawtypes")
    final NormalizedNodeBuilder builder() {
        return builder;
    }

    final @Nullable DataSchemaContextNode<?> findChildSchema(final PathArgument child) {
        return startChild(schemaNode, child);
    }

    final @Nullable NormalizedNode<?, ?> build() {
        if (schemaNode == null) {
            LOG.debug("Schema not found for {}", identifier);
            return null;
        }

        return builder.build();
    }

    abstract void addChild(NormalizedNode<?, ?> child);

    abstract void setValue(Object value);

    abstract DataSchemaContextNode<?> startChild(@Nullable DataSchemaContextNode<?> schemaNode, PathArgument child);
}
