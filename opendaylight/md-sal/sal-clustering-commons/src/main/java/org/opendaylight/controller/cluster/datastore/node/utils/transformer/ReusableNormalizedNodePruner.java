/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

/**
 * The NormalizedNodePruner removes all nodes from the input NormalizedNode that do not have a corresponding
 * schema element in the passed in SchemaContext. Instances of this class can be reused multiple times and must be
 * initialized before each use through {@link #initializeForPath(YangInstanceIdentifier)}.
 */
@Beta
public final class ReusableNormalizedNodePruner extends AbstractNormalizedNodePruner {
    ReusableNormalizedNodePruner(final EffectiveModelContext schemaContext) {
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
    public static @NonNull ReusableNormalizedNodePruner forSchemaContext(final EffectiveModelContext schemaContext) {
        return new ReusableNormalizedNodePruner(schemaContext);
    }

    /**
     * Create a new pruner bound to a DataSchemaContextTree. This is a more efficient alternative of
     * {@link #forSchemaContext(EffectiveModelContext)}.
     *
     * @param tree DataSchemaContextTree to use
     * @return A new uninitialized pruner
     * @throws NullPointerException if {@code schemaContext} is null
     */
    public static @NonNull ReusableNormalizedNodePruner forDataSchemaContext(final DataSchemaContextTree tree) {
        return new ReusableNormalizedNodePruner(tree);
    }

    /**
     * Return a new instance, which is backed but the same DataSchemaContextTree, but does not share any state and is
     * uninitialized. This is equivalent to {@link #forDataSchemaContext(DataSchemaContextTree)} and is provided for
     * convenience.
     *
     * @return A new uninitialized pruner bound to the same SchemaContext as this one.
     */
    public @NonNull ReusableNormalizedNodePruner duplicate() {
        return new ReusableNormalizedNodePruner(getTree());
    }

    /**
     * Initialize this pruner for processing a node at specified path.
     *
     * @param path Path that will be processed next
     * @throws NullPointerException if {@code path} is null
     */
    public void initializeForPath(final YangInstanceIdentifier path) {
        initialize(path);
    }
}
