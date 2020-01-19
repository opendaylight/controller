/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ForwardingObject;
import java.io.IOException;
import java.util.Optional;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.ReusableNormalizedNodePruner;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PruningDataTreeModification first removes all entries from the data which do not belong in the schemaContext
 * before delegating it to the actual DataTreeModification.
 */
public class PruningDataTreeModification extends ForwardingObject implements DataTreeModification {
    private static final Logger LOG = LoggerFactory.getLogger(PruningDataTreeModification.class);

    private final ReusableNormalizedNodePruner pruner;
    private final DataTree dataTree;

    private DataTreeModification delegate;

    public PruningDataTreeModification(final DataTreeModification delegate, final DataTree dataTree,
            final ReusableNormalizedNodePruner pruner) {
        this.delegate = requireNonNull(delegate);
        this.dataTree = requireNonNull(dataTree);
        this.pruner = requireNonNull(pruner);
    }

    @Override
    protected DataTreeModification delegate() {
        return delegate;
    }

    @Override
    public SchemaContext getSchemaContext() {
        return delegate.getSchemaContext();
    }

    @Override
    public void delete(final YangInstanceIdentifier yangInstanceIdentifier) {
        try {
            delegate.delete(yangInstanceIdentifier);
        } catch (SchemaValidationFailedException e) {
            LOG.warn("Node at path : {} does not exist ignoring delete", yangInstanceIdentifier);
        }
    }

    @Override
    public void merge(final YangInstanceIdentifier yangInstanceIdentifier, final NormalizedNode<?, ?> normalizedNode) {
        try {
            if (YangInstanceIdentifier.empty().equals(yangInstanceIdentifier)) {
                pruneAndMergeNode(yangInstanceIdentifier, normalizedNode);
            } else {
                delegate.merge(yangInstanceIdentifier, normalizedNode);
            }
        } catch (SchemaValidationFailedException e) {
            LOG.warn("Node at path {} was pruned during merge due to validation error: {}",
                    yangInstanceIdentifier, e.getMessage());

            pruneAndMergeNode(yangInstanceIdentifier, normalizedNode);
        }

    }

    private void pruneAndMergeNode(final YangInstanceIdentifier yangInstanceIdentifier,
            final NormalizedNode<?, ?> normalizedNode) {
        NormalizedNode<?, ?> pruned = pruneNormalizedNode(yangInstanceIdentifier, normalizedNode);

        if (pruned != null) {
            delegate.merge(yangInstanceIdentifier, pruned);
        }
    }

    @Override
    public void write(final YangInstanceIdentifier yangInstanceIdentifier, final NormalizedNode<?, ?> normalizedNode) {
        try {
            if (YangInstanceIdentifier.empty().equals(yangInstanceIdentifier)) {
                pruneAndWriteNode(yangInstanceIdentifier, normalizedNode);
            } else {
                delegate.write(yangInstanceIdentifier, normalizedNode);
            }
        } catch (SchemaValidationFailedException e) {
            LOG.warn("Node at path : {} was pruned during write due to validation error: {}",
                    yangInstanceIdentifier, e.getMessage());

            pruneAndWriteNode(yangInstanceIdentifier, normalizedNode);
        }
    }

    private void pruneAndWriteNode(final YangInstanceIdentifier yangInstanceIdentifier,
            final NormalizedNode<?, ?> normalizedNode) {
        NormalizedNode<?,?> pruned = pruneNormalizedNode(yangInstanceIdentifier, normalizedNode);

        if (pruned != null) {
            delegate.write(yangInstanceIdentifier, pruned);
        }
    }

    @Override
    public void ready() {
        try {
            delegate.ready();
        } catch (SchemaValidationFailedException e) {
            DataTreeModification newModification = dataTree.takeSnapshot().newModification();
            delegate.applyToCursor(new PruningDataTreeModificationCursor(newModification, this));

            delegate = newModification;
            delegate.ready();
        }
    }

    @Override
    public void applyToCursor(final DataTreeModificationCursor dataTreeModificationCursor) {
        delegate.applyToCursor(dataTreeModificationCursor);
    }

    @Override
    public Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier yangInstanceIdentifier) {
        return delegate.readNode(yangInstanceIdentifier);
    }

    @Override
    public DataTreeModification newModification() {
        return new PruningDataTreeModification(delegate.newModification(), dataTree, pruner.duplicate());
    }

    @VisibleForTesting
    NormalizedNode<?, ?> pruneNormalizedNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> input) {
        pruner.initializeForPath(path);
        try {
            NormalizedNodeWriter.forStreamWriter(pruner).write(input);
        } catch (IOException ioe) {
            LOG.error("Unexpected IOException when pruning normalizedNode", ioe);
            return null;
        }

        return pruner.getResult().orElse(null);
    }

    private static class PruningDataTreeModificationCursor extends AbstractDataTreeModificationCursor {
        private final DataTreeModification toModification;
        private final PruningDataTreeModification pruningModification;

        PruningDataTreeModificationCursor(final DataTreeModification toModification,
                final PruningDataTreeModification pruningModification) {
            this.toModification = toModification;
            this.pruningModification = pruningModification;
        }

        @Override
        public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
            YangInstanceIdentifier path = current().node(child);
            NormalizedNode<?, ?> prunedNode = pruningModification.pruneNormalizedNode(path, data);
            if (prunedNode != null) {
                toModification.write(path, prunedNode);
            }
        }

        @Override
        public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
            YangInstanceIdentifier path = current().node(child);
            NormalizedNode<?, ?> prunedNode = pruningModification.pruneNormalizedNode(path, data);
            if (prunedNode != null) {
                toModification.merge(path, prunedNode);
            }
        }

        @Override
        public void delete(final PathArgument child) {
            try {
                toModification.delete(current().node(child));
            } catch (SchemaValidationFailedException e) {
                // Ignoring since we would've already logged this in the call to the original modification.
            }
        }
    }
}
