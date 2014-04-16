/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.TreeNodeUtils;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/*
 * FIXME: the thread safety of concurrent write/delete/read/seal operations
 *        needs to be evaluated.
 */
class MutableDataTree {
    private static final Logger LOG = LoggerFactory.getLogger(MutableDataTree.class);
    private final AtomicBoolean sealed = new AtomicBoolean();
    private final ModificationApplyOperation strategyTree;
    private final DataAndMetadataSnapshot snapshot;
    private final NodeModification rootModification;

    private MutableDataTree(final DataAndMetadataSnapshot snapshot, final ModificationApplyOperation strategyTree) {
        this.snapshot = snapshot;
        this.strategyTree = strategyTree;
        this.rootModification = NodeModification.createUnmodified(snapshot.getMetadataTree());
    }

    public void write(final InstanceIdentifier path, final NormalizedNode<?, ?> value) {
        checkSealed();
        resolveModificationFor(path).write(value);
    }

    public void delete(final InstanceIdentifier path) {
        checkSealed();
        resolveModificationFor(path).delete();
    }

    public Optional<NormalizedNode<?, ?>> read(final InstanceIdentifier path) {
        Entry<InstanceIdentifier, NodeModification> modification = TreeNodeUtils.findClosestsOrFirstMatch(rootModification, path, NodeModification.IS_TERMINAL_PREDICATE);

        Optional<StoreMetadataNode> result = resolveSnapshot(modification);
        if (result.isPresent()) {
            NormalizedNode<?, ?> data = result.get().getData();
            return NormalizedNodeUtils.findNode(modification.getKey(), data, path);
        }
        return Optional.absent();
    }

    private Optional<StoreMetadataNode> resolveSnapshot(
            final Entry<InstanceIdentifier, NodeModification> keyModification) {
        InstanceIdentifier path = keyModification.getKey();
        NodeModification modification = keyModification.getValue();
        return resolveSnapshot(path, modification);
    }

    private Optional<StoreMetadataNode> resolveSnapshot(final InstanceIdentifier path,
            final NodeModification modification) {
        try {
            Optional<Optional<StoreMetadataNode>> potentialSnapshot = modification.getSnapshotCache();
            if(potentialSnapshot.isPresent()) {
                return potentialSnapshot.get();
            }
            return resolveModificationStrategy(path).apply(modification, modification.getOriginal(),
                    StoreUtils.increase(snapshot.getMetadataTree().getSubtreeVersion()));
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
        NodeModification modification = rootModification;
        // We ensure strategy is present.
        ModificationApplyOperation operation = resolveModificationStrategy(path);
        for (PathArgument pathArg : path.getPath()) {
            modification = modification.modifyChild(pathArg);
        }
        return OperationWithModification.from(operation, modification);
    }

    public static MutableDataTree from(final DataAndMetadataSnapshot snapshot, final ModificationApplyOperation resolver) {
        return new MutableDataTree(snapshot, resolver);
    }

    public void seal() {
        final boolean success = sealed.compareAndSet(false, true);
        Preconditions.checkState(success, "Attempted to seal an already-sealed Data Tree.");
        rootModification.seal();
    }

    private void checkSealed() {
        checkState(!sealed.get(), "Data Tree is sealed. No further modifications allowed.");
    }

    protected NodeModification getRootModification() {
        return rootModification;
    }

    @Override
    public String toString() {
        return "MutableDataTree [modification=" + rootModification + "]";
    }
}
