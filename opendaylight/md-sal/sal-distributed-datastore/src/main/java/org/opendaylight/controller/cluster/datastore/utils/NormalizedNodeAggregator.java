/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Optional;
import java.util.List;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeAggregator {
    private final YangInstanceIdentifier rootIdentifier;
    private final List<Optional<NormalizedNode<?, ?>>> nodes;
    private final DataTree dataTree;

    private NormalizedNodeAggregator(final YangInstanceIdentifier rootIdentifier, final List<Optional<NormalizedNode<?, ?>>> nodes,
                             final SchemaContext schemaContext) {
        this.rootIdentifier = rootIdentifier;
        this.nodes = nodes;
        // FIXME: BUG-1014: pass down proper DataTree
        this.dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        this.dataTree.setSchemaContext(schemaContext);
    }

    /**
     * Combine data from all the nodes in the list into a tree with root as rootIdentifier
     *
     * @param nodes
     * @param schemaContext
     * @return
     * @throws DataValidationFailedException
     */
    public static Optional<NormalizedNode<?,?>> aggregate(final YangInstanceIdentifier rootIdentifier,
                                                          final List<Optional<NormalizedNode<?, ?>>> nodes,
                                                          final SchemaContext schemaContext) throws DataValidationFailedException {
        return new NormalizedNodeAggregator(rootIdentifier, nodes, schemaContext).aggregate();
    }

    private Optional<NormalizedNode<?,?>> aggregate() throws DataValidationFailedException {
        return combine().getRootNode();
    }

    private NormalizedNodeAggregator combine() throws DataValidationFailedException {
        final DataTreeModification mod = dataTree.takeSnapshot().newModification();

        for (final Optional<NormalizedNode<?,?>> node : nodes) {
            if (node.isPresent()) {
                mod.merge(rootIdentifier, node.get());
            }
        }
        mod.ready();
        dataTree.validate(mod);
        final DataTreeCandidate candidate = dataTree.prepare(mod);
        dataTree.commit(candidate);

        return this;
    }

    private Optional<NormalizedNode<?, ?>> getRootNode() {
        return dataTree.takeSnapshot().readNode(rootIdentifier);
    }
}
