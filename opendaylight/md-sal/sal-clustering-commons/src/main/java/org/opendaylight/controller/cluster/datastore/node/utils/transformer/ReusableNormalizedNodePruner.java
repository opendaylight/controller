/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.math.BigInteger;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.Uint16TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint32TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint64TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.Uint8TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The NormalizedNodePruner removes all nodes from the input NormalizedNode that do not have a corresponding
 * schema element in the passed in SchemaContext.
 *
 * <p>
 * Unlike {@link NormalizedNodePruner}, this class can be reused multiple times and must be initialized before each use
 * through {@link #initializeForPath(YangInstanceIdentifier)}.
 */
@Beta
public abstract class ReusableNormalizedNodePruner extends AbstractNormalizedNodePruner {
    private static final class SimplePruner extends ReusableNormalizedNodePruner {
        SimplePruner(final SchemaContext schemaContext) {
            super(schemaContext);
        }

        SimplePruner(final DataSchemaContextTree tree) {
            super(tree);
        }

        @Override
        public ReusableNormalizedNodePruner duplicate() {
            return new SimplePruner(getTree());
        }
    }

    private static final class UintAdaptingPruner extends ReusableNormalizedNodePruner {
        private static final Logger LOG = LoggerFactory.getLogger(UintAdaptingPruner.class);

        UintAdaptingPruner(final DataSchemaContextTree tree) {
            super(tree);
        }

        @Override
        public ReusableNormalizedNodePruner duplicate() {
            return new UintAdaptingPruner(getTree());
        }

        @Override
        Object translateScalar(final DataSchemaContextNode<?> context, final Object value) throws IOException {
            final DataSchemaNode schema = context.getDataSchemaNode();
            if (schema instanceof TypedDataSchemaNode) {
                final TypeDefinition<?> type = ((TypedDataSchemaNode) schema).getType();
                if (value instanceof Short && type instanceof Uint8TypeDefinition) {
                    LOG.trace("Translating legacy uint8 {}", value);
                    return Uint8.valueOf((Short) value);
                } else if (value instanceof Integer && type instanceof Uint16TypeDefinition) {
                    LOG.trace("Translating legacy uint16 {}", value);
                    return Uint16.valueOf((Integer) value);
                } else if (value instanceof Long && type instanceof Uint32TypeDefinition) {
                    LOG.trace("Translating legacy uint32 {}", value);
                    return Uint32.valueOf((Long) value);
                } else if (value instanceof BigInteger && type instanceof Uint64TypeDefinition) {
                    LOG.trace("Translating legacy uint64 {}", value);
                    return Uint64.valueOf((BigInteger) value);
                }
            }
            return value;
        }
    }

    ReusableNormalizedNodePruner(final SchemaContext schemaContext) {
        super(schemaContext);
    }

    ReusableNormalizedNodePruner(final DataSchemaContextTree tree) {
        super(tree);
    }

    /**
     * Create a new pruner bound to a SchemaContext.
     *
     * @param schemaContext SchemaContext to use
     * @return A new uninitialized pruner
     * @throws NullPointerException if {@code schemaContext} is null
     */
    public static @NonNull ReusableNormalizedNodePruner forSchemaContext(final SchemaContext schemaContext) {
        return new SimplePruner(schemaContext);
    }

    /**
     * Create a new pruner bound to a DataSchemaContextTree. This is a more efficient alternative of
     * {@link #forSchemaContext(SchemaContext)}.
     *
     * @param tree DataSchemaContextTree to use
     * @return A new uninitialized pruner
     * @throws NullPointerException if {@code schemaContext} is null
     */
    public static @NonNull ReusableNormalizedNodePruner forDataSchemaContext(final DataSchemaContextTree tree) {
        return new SimplePruner(tree);
    }

    /**
     * Return a new instance, which is backed but the same DataSchemaContextTree, but does not share any state and is
     * uninitialized. This is equivalent to {@link #forDataSchemaContext(DataSchemaContextTree)} and is provided for
     * convenience.
     *
     * @return A new uninitialized pruner bound to the same SchemaContext as this one.
     */
    public abstract @NonNull ReusableNormalizedNodePruner duplicate();

    /**
     * Initialize this pruner for processing a node at specified path.
     *
     * @param path Path that will be processed next
     * @throws NullPointerException if {@code path} is null
     */
    public final void initializeForPath(final YangInstanceIdentifier path) {
        initialize(path);
    }

    public final @NonNull ReusableNormalizedNodePruner withUintAdaption() {
        return new UintAdaptingPruner(getTree());
    }
}
