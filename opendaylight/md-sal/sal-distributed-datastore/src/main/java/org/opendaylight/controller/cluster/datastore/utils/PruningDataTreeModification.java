/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.NormalizedNodePruner;
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
 * before delegating it to the actual DataTreeModification
 */
public class PruningDataTreeModification implements DataTreeModification {

    private static final Logger LOG = LoggerFactory.getLogger(PruningDataTreeModification.class);
    private DataTreeModification delegate;
    private final SchemaContext schemaContext;
    private final DataTree dataTree;

    public PruningDataTreeModification(DataTreeModification delegate, DataTree dataTree, SchemaContext schemaContext) {
        this.delegate = delegate;
        this.dataTree = dataTree;
        this.schemaContext = schemaContext;
    }

    @Override
    public void delete(YangInstanceIdentifier yangInstanceIdentifier) {
        try {
            delegate.delete(yangInstanceIdentifier);
        } catch(SchemaValidationFailedException e){
            LOG.warn("Node at path : {} does not exist ignoring delete", yangInstanceIdentifier);
        }
    }

    @Override
    public void merge(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode<?, ?> normalizedNode) {
        try {
            if(YangInstanceIdentifier.EMPTY.equals(yangInstanceIdentifier)){
                pruneAndMergeNode(yangInstanceIdentifier, normalizedNode);
            } else {
                delegate.merge(yangInstanceIdentifier, normalizedNode);
            }
        } catch (SchemaValidationFailedException e){
            LOG.warn("Node at path {} was pruned during merge due to validation error: {}",
                    yangInstanceIdentifier, e.getMessage());

            pruneAndMergeNode(yangInstanceIdentifier, normalizedNode);
        }

    }

    private void pruneAndMergeNode(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode<?, ?> normalizedNode) {
        NormalizedNode<?,?> pruned = pruneNormalizedNode(yangInstanceIdentifier, normalizedNode);

        if(pruned != null) {
            delegate.merge(yangInstanceIdentifier, pruned);
        }
    }

    @Override
    public void write(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode<?, ?> normalizedNode) {
        try {
            if(YangInstanceIdentifier.EMPTY.equals(yangInstanceIdentifier)){
                pruneAndWriteNode(yangInstanceIdentifier, normalizedNode);
            } else {
                delegate.write(yangInstanceIdentifier, normalizedNode);
            }
        } catch (SchemaValidationFailedException e){
            LOG.warn("Node at path : {} was pruned during write due to validation error: {}",
                    yangInstanceIdentifier, e.getMessage());

            pruneAndWriteNode(yangInstanceIdentifier, normalizedNode);
        }
    }

    private void pruneAndWriteNode(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode<?, ?> normalizedNode) {
        NormalizedNode<?,?> pruned = pruneNormalizedNode(yangInstanceIdentifier, normalizedNode);

        if(pruned != null) {
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
    public void applyToCursor(DataTreeModificationCursor dataTreeModificationCursor) {
        delegate.applyToCursor(dataTreeModificationCursor);
    }

    @Override
    public Optional<NormalizedNode<?, ?>> readNode(YangInstanceIdentifier yangInstanceIdentifier) {
        return delegate.readNode(yangInstanceIdentifier);
    }

    @Override
    public DataTreeModification newModification() {
        return new PruningDataTreeModification(delegate.newModification(), dataTree, schemaContext);
    }

    @VisibleForTesting
    NormalizedNode<?, ?> pruneNormalizedNode(YangInstanceIdentifier path, NormalizedNode<?,?> input) {
        NormalizedNodePruner pruner = new NormalizedNodePruner(path, schemaContext);
        try {
            NormalizedNodeWriter.forStreamWriter(pruner).write(input);
        } catch (IOException ioe) {
            LOG.error("Unexpected IOException when pruning normalizedNode", ioe);
        }

        return pruner.normalizedNode();
    }

    public DataTreeModification getResultingModification(){
        return delegate;
    }

    private static class PruningDataTreeModificationCursor implements DataTreeModificationCursor {
        private final Deque<YangInstanceIdentifier> stack = new ArrayDeque<>();
        private final DataTreeModification toModification;
        private final PruningDataTreeModification pruningModification;

        PruningDataTreeModificationCursor(DataTreeModification toModification,
                PruningDataTreeModification pruningModification) {
            this.toModification = toModification;
            this.pruningModification = pruningModification;
            stack.push(YangInstanceIdentifier.EMPTY);
        }

        @Override
        public void write(PathArgument child, NormalizedNode<?, ?> data) {
            YangInstanceIdentifier path = stack.peek().node(child);
            NormalizedNode<?, ?> prunedNode = pruningModification.pruneNormalizedNode(path, data);
            if(prunedNode != null) {
                toModification.write(path, prunedNode);
            }
        }

        @Override
        public void merge(PathArgument child, NormalizedNode<?, ?> data) {
            YangInstanceIdentifier path = stack.peek().node(child);
            NormalizedNode<?, ?> prunedNode = pruningModification.pruneNormalizedNode(path, data);
            if(prunedNode != null) {
                toModification.merge(path, prunedNode);
            }
        }

        @Override
        public void delete(PathArgument child) {
            try {
                toModification.delete(stack.peek().node(child));
            } catch(SchemaValidationFailedException e) {
                // Ignoring since we would've already logged this in the call to the original modification.
            }
        }

        @Override
        public void enter(@Nonnull final PathArgument child) {
            stack.push(stack.peek().node(child));
        }

        @Override
        public void enter(@Nonnull final PathArgument... path) {
            for (PathArgument arg : path) {
                enter(arg);
            }
        }

        @Override
        public void enter(@Nonnull final Iterable<PathArgument> path) {
            for (PathArgument arg : path) {
                enter(arg);
            }
        }

        @Override
        public void exit() {
            stack.pop();
        }

        @Override
        public void exit(final int depth) {
            Preconditions.checkArgument(depth < stack.size(), "Stack holds only %s elements, cannot exit %s levels", stack.size(), depth);
            for (int i = 0; i < depth; ++i) {
                stack.pop();
            }
        }

        @Override
        public Optional<NormalizedNode<?, ?>> readNode(@Nonnull final PathArgument child) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void close() {
        }
    }
}
