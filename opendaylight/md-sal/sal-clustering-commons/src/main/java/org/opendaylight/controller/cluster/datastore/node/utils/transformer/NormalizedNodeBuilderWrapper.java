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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;

final class NormalizedNodeBuilderWrapper {
    private final NormalizedNodeContainerBuilder<?, ?, ?, ?> builder;
    private final PathArgument identifier;
    private final DataSchemaContextNode<?> schemaNode;

    NormalizedNodeBuilderWrapper(final NormalizedNodeContainerBuilder<?, ?, ?, ?> builder,
            final PathArgument identifier, final @Nullable DataSchemaContextNode<?> schemaNode) {
        this.builder = requireNonNull(builder);
        this.identifier = requireNonNull(identifier);
        this.schemaNode = schemaNode;
    }

    @SuppressWarnings("rawtypes")
    NormalizedNodeContainerBuilder builder() {
        return builder;
    }

    QName nodeType() {
        return identifier.getNodeType();
    }

    PathArgument identifier() {
        return identifier;
    }

    @Nullable DataSchemaContextNode<?> getSchema() {
        return schemaNode;
    }
}
