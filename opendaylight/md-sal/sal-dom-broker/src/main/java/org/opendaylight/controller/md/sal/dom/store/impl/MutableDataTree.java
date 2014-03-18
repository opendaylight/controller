/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Map.Entry;

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

class MutableDataTree {

    private static final Logger log = LoggerFactory.getLogger(MutableDataTree.class);

    final DataAndMetadataSnapshot snapshot;
    final NodeModification rootModification;
    final ModificationApplyOperation strategyTree;

    private  boolean sealed = false;

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
        Entry<InstanceIdentifier, NodeModification> modification = TreeNodeUtils.findClosest(rootModification, path);
        return getModifiedVersion(path, modification);
    }

    private Optional<NormalizedNode<?, ?>> getModifiedVersion(final InstanceIdentifier path, final Entry<InstanceIdentifier, NodeModification> modification) {
        Optional<StoreMetadataNode> result = resolveSnapshot(modification);
        if(result.isPresent()) {
            NormalizedNode<?, ?> data = result.get().getData();
            return NormalizedNodeUtils.findNode(modification.getKey(),data, path);
        }
        return Optional.absent();

    }

    private Optional<StoreMetadataNode> resolveSnapshot(final Entry<InstanceIdentifier, NodeModification> keyModification) {
        InstanceIdentifier path = keyModification.getKey();
        NodeModification modification = keyModification.getValue();
        return resolveSnapshot(path,modification);
    }

    private Optional<StoreMetadataNode> resolveSnapshot(final InstanceIdentifier path, final NodeModification modification) {
        try {
            return resolveModificationStrategy(path).apply(modification,modification.getOriginal());
        } catch (Exception e) {
            log.error("Could not create snapshot for {},",e);
            throw e;
        }
    }

    private ModificationApplyOperation resolveModificationStrategy(final InstanceIdentifier path) {
        log.trace("Resolving modification apply strategy for {}",path);
        Optional<ModificationApplyOperation> strategy = TreeNodeUtils.findNode(strategyTree, path);
        checkArgument(strategy.isPresent(),"Provided path %s is not supported by data store. No schema available for it.",path);
        return strategy.get();
    }

    private NodeModification resolveModificationFor(final InstanceIdentifier path) {
        NodeModification modification = rootModification;
        // We ensure strategy is present.
        resolveModificationStrategy(path);
        for (PathArgument pathArg : path.getPath()) {
            modification = modification.modifyChild(pathArg);
        }
        return modification;
    }

    public static MutableDataTree from(final DataAndMetadataSnapshot snapshot, final ModificationApplyOperation resolver) {
        return new MutableDataTree(snapshot, resolver);
    }

    public void seal() {
        sealed = true;
        rootModification.seal();
    }

    private void checkSealed() {
        checkState(!sealed , "Data Tree is sealed. No further modifications allowed.");
    }
}
