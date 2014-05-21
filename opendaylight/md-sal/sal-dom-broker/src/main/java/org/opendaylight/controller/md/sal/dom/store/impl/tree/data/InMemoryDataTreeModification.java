/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreUtils;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.TreeNodeUtils;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

final class InMemoryDataTreeModification implements DataTreeModification {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDataTreeModification.class);

    /*
     * FIXME: the thread safety of concurrent write/delete/read/seal operations
     *        needs to be evaluated.
     */
    private static final AtomicIntegerFieldUpdater<InMemoryDataTreeModification> SEALED_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(InMemoryDataTreeModification.class, "sealed");
    private volatile int sealed = 0;

    private final ModificationApplyOperation strategyTree;
    private final InMemoryDataTreeSnapshot snapshot;
    private final NodeModification rootNode;

    InMemoryDataTreeModification(final InMemoryDataTreeSnapshot snapshot, final ModificationApplyOperation resolver) {
        this.snapshot = Preconditions.checkNotNull(snapshot);
        this.strategyTree = Preconditions.checkNotNull(resolver);
        this.rootNode = NodeModification.createUnmodified(snapshot.getRootNode());
    }

    NodeModification getRootModification() {
        return rootNode;
    }

    ModificationApplyOperation getStrategy() {
        return strategyTree;
    }

    @Override
    public void write(final InstanceIdentifier path, final NormalizedNode<?, ?> value) {
        checkSealed();
        resolveModificationFor(path).write(value);
    }

    @Override
    public void merge(final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkSealed();
        mergeImpl(resolveModificationFor(path),data);
    }

    private void mergeImpl(final OperationWithModification op,final NormalizedNode<?,?> data) {

        if(data instanceof NormalizedNodeContainer<?,?,?>) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            NormalizedNodeContainer<?,?,NormalizedNode<PathArgument, ?>> dataContainer = (NormalizedNodeContainer) data;
            for(NormalizedNode<PathArgument, ?> child : dataContainer.getValue()) {
                PathArgument childId = child.getIdentifier();
                mergeImpl(op.forChild(childId), child);
            }
        }
        op.merge(data);
    }

    @Override
    public void delete(final InstanceIdentifier path) {
        checkSealed();
        resolveModificationFor(path).delete();
    }

    @Override
    public Optional<NormalizedNode<?, ?>> readNode(final InstanceIdentifier path) {
        /*
         * Walk the tree from the top, looking for the first node between root and
         * the requested path which has been modified. If no such node exists,
         * we use the node itself.
         */
        final Entry<InstanceIdentifier, NodeModification> entry = TreeNodeUtils.findClosestsOrFirstMatch(rootNode, path, NodeModification.IS_TERMINAL_PREDICATE);
        final InstanceIdentifier key = entry.getKey();
        final NodeModification mod = entry.getValue();

        final Optional<StoreMetadataNode> result = resolveSnapshot(key, mod);
        if (result.isPresent()) {
            NormalizedNode<?, ?> data = result.get().getData();
            return NormalizedNodeUtils.findNode(key, data, path);
        } else {
            return Optional.absent();
        }
    }

    private Optional<StoreMetadataNode> resolveSnapshot(final InstanceIdentifier path,
            final NodeModification modification) {
        try {
            Optional<Optional<StoreMetadataNode>> potentialSnapshot = modification.getSnapshotCache();
            if(potentialSnapshot.isPresent()) {
                return potentialSnapshot.get();
            }
            return resolveModificationStrategy(path).apply(modification, modification.getOriginal(),
                    StoreUtils.increase(snapshot.getRootNode().getSubtreeVersion()));
        } catch (Exception e) {
            LOG.error("Could not create snapshot for {}:{}", path,modification,e);
            throw e;
        }
    }

    private ModificationApplyOperation resolveModificationStrategy(final InstanceIdentifier path) {
        LOG.trace("Resolving modification apply strategy for {}", path);
        return TreeNodeUtils.findNodeChecked(strategyTree, path);
    }

    private OperationWithModification resolveModificationFor(final InstanceIdentifier path) {
        NodeModification modification = rootNode;
        // We ensure strategy is present.
        ModificationApplyOperation operation = resolveModificationStrategy(path);
        for (PathArgument pathArg : path.getPath()) {
            modification = modification.modifyChild(pathArg);
        }
        return OperationWithModification.from(operation, modification);
    }

    @Override
    public void seal() {
        final boolean success = SEALED_UPDATER.compareAndSet(this, 0, 1);
        Preconditions.checkState(success, "Attempted to seal an already-sealed Data Tree.");
        rootNode.seal();
    }

    private void checkSealed() {
        checkState(sealed == 0, "Data Tree is sealed. No further modifications allowed.");
    }

    @Override
    public String toString() {
        return "MutableDataTree [modification=" + rootNode + "]";
    }

    @Override
    public DataTreeModification newModification() {
        // FIXME: transaction chaining
        throw new UnsupportedOperationException("Implement this as part of transaction chaining");
    }
}
