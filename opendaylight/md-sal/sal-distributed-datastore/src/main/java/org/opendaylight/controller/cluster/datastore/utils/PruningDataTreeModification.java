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
public abstract class PruningDataTreeModification extends ForwardingObject implements DataTreeModification {
    /**
     * A PruningDataTreeModification which always performs pruning before attempting an operation. This sacrifices
     * performance to ensure all data has passed through the pruner -- such that data adaptations are performed.
     */
    public static final class Proactive extends PruningDataTreeModification {
        public Proactive(final DataTreeModification delegate, final DataTree dataTree,
                final ReusableNormalizedNodePruner pruner) {
            super(delegate, dataTree, pruner);
        }

        @Override
        public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            pruneAndMergeNode(path, data);
        }

        @Override
        public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            pruneAndWriteNode(path, data);
        }

        @Override
        PruningDataTreeModification createNew(final DataTreeModification delegate, final DataTree dataTree,
                final ReusableNormalizedNodePruner pruner) {
            return new Proactive(delegate, dataTree, pruner);
        }
    }

    /**
     * A PruningDataTreeModification which performs pruning only when an operation results in an
     * {@link SchemaValidationFailedException}. This offers superior performance in the normal case of not needing
     * pruning.
     */
    public static final class Reactive extends PruningDataTreeModification {
        public Reactive(final DataTreeModification delegate, final DataTree dataTree,
                final ReusableNormalizedNodePruner pruner) {
            super(delegate, dataTree, pruner);
        }

        @Override
        public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            if (path.isEmpty()) {
                pruneAndMergeNode(path, data);
                return;
            }

            try {
                delegate().merge(path, data);
            } catch (SchemaValidationFailedException e) {
                LOG.warn("Node at path {} was pruned during merge due to validation error: {}", path, e.getMessage());
                pruneAndMergeNode(path, data);
            }
        }

        @Override
        public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            if (path.isEmpty()) {
                pruneAndWriteNode(path, data);
                return;
            }

            try {
                delegate().write(path, data);
            } catch (SchemaValidationFailedException e) {
                LOG.warn("Node at path : {} was pruned during write due to validation error: {}", path, e.getMessage());
                pruneAndWriteNode(path, data);
            }
        }

        @Override
        PruningDataTreeModification createNew(final DataTreeModification delegate, final DataTree dataTree,
                final ReusableNormalizedNodePruner pruner) {
            return new Reactive(delegate, dataTree, pruner);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PruningDataTreeModification.class);

    private final ReusableNormalizedNodePruner pruner;
    private final DataTree dataTree;

    private DataTreeModification delegate;

    PruningDataTreeModification(final DataTreeModification delegate, final DataTree dataTree,
            final ReusableNormalizedNodePruner pruner) {
        this.delegate = requireNonNull(delegate);
        this.dataTree = requireNonNull(dataTree);
        this.pruner = requireNonNull(pruner);
    }

    @Override
    protected final DataTreeModification delegate() {
        return delegate;
    }

    @Override
    public final SchemaContext getSchemaContext() {
        return delegate.getSchemaContext();
    }

    @Override
    public final void delete(final YangInstanceIdentifier path) {
        try {
            delegate.delete(path);
        } catch (SchemaValidationFailedException e) {
            LOG.warn("Node at path : {} does not exist ignoring delete", path);
        }
    }

    final void pruneAndMergeNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        final NormalizedNode<?, ?> pruned = pruneNormalizedNode(path, data);
        if (pruned != null) {
            delegate.merge(path, pruned);
        }
    }

    final void pruneAndWriteNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        final NormalizedNode<?, ?> pruned = pruneNormalizedNode(path, data);
        if (pruned != null) {
            delegate.write(path, pruned);
        }
    }

    @Override
    public final void ready() {
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
    public final void applyToCursor(final DataTreeModificationCursor dataTreeModificationCursor) {
        delegate.applyToCursor(dataTreeModificationCursor);
    }

    @Override
    public final Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier yangInstanceIdentifier) {
        return delegate.readNode(yangInstanceIdentifier);
    }

    @Override
    public final DataTreeModification newModification() {
        return createNew(delegate.newModification(), dataTree, pruner.duplicate());
    }

    @VisibleForTesting
    final NormalizedNode<?, ?> pruneNormalizedNode(final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> input) {
        pruner.initializeForPath(path);
        try {
            NormalizedNodeWriter.forStreamWriter(pruner).write(input);
        } catch (IOException ioe) {
            LOG.error("Unexpected IOException when pruning normalizedNode", ioe);
            return null;
        }

        return pruner.getResult().orElse(null);
    }

    abstract PruningDataTreeModification createNew(DataTreeModification delegate, DataTree dataTree,
            ReusableNormalizedNodePruner pruner);

    private static final class PruningDataTreeModificationCursor extends AbstractDataTreeModificationCursor {
        private final DataTreeModification toModification;
        private final PruningDataTreeModification pruningModification;

        PruningDataTreeModificationCursor(final DataTreeModification toModification,
                final PruningDataTreeModification pruningModification) {
            this.toModification = toModification;
            this.pruningModification = pruningModification;
        }

        @Override
        public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
            final YangInstanceIdentifier path = current().node(child);
            final NormalizedNode<?, ?> prunedNode = pruningModification.pruneNormalizedNode(path, data);
            if (prunedNode != null) {
                toModification.write(path, prunedNode);
            }
        }

        @Override
        public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
            final YangInstanceIdentifier path = current().node(child);
            final NormalizedNode<?, ?> prunedNode = pruningModification.pruneNormalizedNode(path, data);
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
