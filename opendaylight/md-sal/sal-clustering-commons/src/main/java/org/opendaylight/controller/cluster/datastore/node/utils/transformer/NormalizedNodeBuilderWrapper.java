/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;

final class NormalizedNodeBuilderWrapper {
    @SuppressWarnings("rawtypes")
    private final NormalizedNodeBuilder builder;
    private final PathArgument identifier;
    private final DataSchemaContextNode<?> schemaNode;

    NormalizedNodeBuilderWrapper(final NormalizedNodeBuilder<?, ?, ?> builder,
            final PathArgument identifier, final @Nullable DataSchemaContextNode<?> schemaNode) {
        this.builder = requireNonNull(builder);
        this.identifier = requireNonNull(identifier);
        this.schemaNode = schemaNode;
    }

    PathArgument identifier() {
        return identifier;
    }

    @Nullable DataSchemaContextNode<?> getSchema() {
        return schemaNode;
    }

    @Nullable DataSchemaContextNode<?> childSchema(final PathArgument child) {
        if (schemaNode == null) {
            return null;
        }

        checkState(builder instanceof NormalizedNodeContainerBuilder,
            "Attempted to lookup child %s in non-container %s", schemaNode);
        return schemaNode.getChild(child);
    }

    NormalizedNode<?, ?> build() {
        return builder.build();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void addChild(final NormalizedNode<?, ?> child) {
        checkState(builder instanceof NormalizedNodeContainerBuilder,
            "Attempted to add child %s to non-container builder %s", child, builder);
        ((NormalizedNodeContainerBuilder) builder).addChild(child);
    }

    @SuppressWarnings("unchecked")
    void setValue(final Object value) {
        checkState(!(builder instanceof NormalizedNodeContainerBuilder),
            "Attempted to set value %s on container builder %s", value, builder);
        builder.withValue(value);
    }
}
