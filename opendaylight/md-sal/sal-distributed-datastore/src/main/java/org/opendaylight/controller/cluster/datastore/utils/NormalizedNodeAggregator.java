/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;

public final class NormalizedNodeAggregator {
    private final YangInstanceIdentifier rootIdentifier;
    private final List<Optional<NormalizedNode>> nodes;
    private final DataTree dataTree;

    private NormalizedNodeAggregator(final DataTree dataTree, final YangInstanceIdentifier rootIdentifier,
            final List<Optional<NormalizedNode>> nodes) {
        this.dataTree = requireNonNull(dataTree);
        this.rootIdentifier = requireNonNull(rootIdentifier);
        this.nodes = nodes;
    }

    /**
     * Combine data from all the nodes in the list into a tree with root as rootIdentifier.
     */
    public static Optional<NormalizedNode> aggregate(final YangInstanceIdentifier rootIdentifier,
            final DataTree dataTree, final List<Optional<NormalizedNode>> nodes)
                throws DataValidationFailedException {
        return new NormalizedNodeAggregator(dataTree, rootIdentifier, nodes).aggregate();
    }

    private Optional<NormalizedNode> aggregate() throws DataValidationFailedException {
        final var mod = dataTree.takeSnapshot().newModification();
        boolean nodePresent = false;

        for (var node : nodes) {
            if (node.isPresent()) {
                mod.merge(rootIdentifier, node.orElseThrow());
                nodePresent = true;
            }
        }

        if (!nodePresent) {
            return Optional.empty();
        }

        mod.ready();
        dataTree.validate(mod);
        final var candidate = dataTree.prepare(mod);
        dataTree.commit(candidate);

        return dataTree.takeSnapshot().readNode(rootIdentifier);
    }
}
