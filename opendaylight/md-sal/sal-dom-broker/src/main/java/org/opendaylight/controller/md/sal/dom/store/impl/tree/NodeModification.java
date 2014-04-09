/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import static com.google.common.base.Preconditions.checkState;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.primitives.UnsignedLong;

/**
 * Node Modification Node and Tree
 *
 * Tree which structurally resembles data tree and captures client modifications
 * to the data store tree.
 *
 * This tree is lazily created and populated via {@link #modifyChild(PathArgument)}
 * and {@link StoreMetadataNode} which represents original state {@link #getOriginal()}.
 *
 */
public class NodeModification implements StoreTreeNode<NodeModification>, Identifiable<PathArgument> {

    public static final Predicate<NodeModification> IS_TERMINAL_PREDICATE = new Predicate<NodeModification>() {
        @Override
        public boolean apply(final NodeModification input) {
            return input.getModificationType() == ModificationType.WRITE || input.getModificationType() == ModificationType.DELETE;
        }
    };
    private final PathArgument identifier;
    private ModificationType modificationType = ModificationType.UNMODIFIED;


    private final Optional<StoreMetadataNode> original;

    private NormalizedNode<?, ?> value;

    private UnsignedLong subtreeVersion;
    private Optional<StoreMetadataNode> snapshotCache;

    private final Map<PathArgument, NodeModification> childModification;

    @GuardedBy("this")
    private boolean sealed = false;

    protected NodeModification(final PathArgument identifier, final Optional<StoreMetadataNode> original) {
        this.identifier = identifier;
        this.original = original;
        childModification = new LinkedHashMap<>();
    }

    /**
     *
     *
     * @return
     */
    public NormalizedNode<?, ?> getWritenValue() {
        return value;
    }

    @Override
    public PathArgument getIdentifier() {
        return identifier;
    }

    /**
     *
     * Returns original store metadata
     * @return original store metadata
     */
    public final Optional<StoreMetadataNode> getOriginal() {
        return original;
    }

    /**
     * Returns modification type
     *
     * @return modification type
     */
    public final ModificationType getModificationType() {
        return modificationType;
    }

    /**
     *
     * Returns child modification if child was modified
     *
     * @return Child modification if direct child or it's subtree
     *  was modified.
     *
     */
    @Override
    public Optional<NodeModification> getChild(final PathArgument child) {
        return Optional.<NodeModification> fromNullable(childModification.get(child));
    }

    /**
     *
     * Returns child modification if child was modified, creates {@link NodeModification}
     * for child otherwise.
     *
     * If this node's {@link ModificationType} is {@link ModificationType#UNMODIFIED}
     * changes modification type to {@link ModificationType#SUBTREE_MODIFIED}
     *
     * @param child
     * @return {@link NodeModification} for specified child, with {@link #getOriginal()}
     *  containing child metadata if child was present in original data.
     */
    public synchronized NodeModification modifyChild(final PathArgument child) {
        checkSealed();
        clearSnapshot();
        if(modificationType == ModificationType.UNMODIFIED) {
            updateModificationType(ModificationType.SUBTREE_MODIFIED);
        }
        final NodeModification potential = childModification.get(child);
        if (potential != null) {
            return potential;
        }
        Optional<StoreMetadataNode> currentMetadata = Optional.absent();
        if(original.isPresent()) {
            currentMetadata = original.get().getChild(child);
        }
        NodeModification newlyCreated = new NodeModification(child,currentMetadata);
        childModification.put(child, newlyCreated);
        return newlyCreated;
    }

    /**
     *
     * Returns all recorded direct child modification
     *
     * @return all recorded direct child modifications
     */
    public Iterable<NodeModification> getModifications() {
        return childModification.values();
    }


    /**
     *
     * Records a delete for associated node.
     *
     */
    public synchronized void delete() {
        checkSealed();
        clearSnapshot();
        updateModificationType(ModificationType.DELETE);
        childModification.clear();
        this.value = null;
    }

    /**
     *
     * Records a write for associated node.
     *
     * @param value
     */
    public synchronized void write(final NormalizedNode<?, ?> value) {
        checkSealed();
        clearSnapshot();
        updateModificationType(ModificationType.WRITE);
        childModification.clear();
        this.value = value;
    }

    @GuardedBy("this")
    private void checkSealed() {
        checkState(!sealed, "Node Modification is sealed. No further changes allowed.");
    }

    public synchronized void seal() {
        sealed = true;
        clearSnapshot();
        for(NodeModification child : childModification.values()) {
            child.seal();
        }
    }

    private void clearSnapshot() {
        snapshotCache = null;
    }

    public Optional<StoreMetadataNode> storeSnapshot(final Optional<StoreMetadataNode> snapshot) {
        snapshotCache = snapshot;
        return snapshot;
    }

    public Optional<Optional<StoreMetadataNode>> getSnapshotCache() {
        return Optional.fromNullable(snapshotCache);
    }

    public boolean hasAdditionalModifications() {
        return !childModification.isEmpty();
    }

    @GuardedBy("this")
    private void updateModificationType(final ModificationType type) {
        modificationType = type;
        clearSnapshot();
    }

    @Override
    public String toString() {
        return "NodeModification [identifier=" + identifier + ", modificationType="
                + modificationType + ", childModification=" + childModification + "]";
    }

    public static NodeModification createUnmodified(final StoreMetadataNode metadataTree) {
        return new NodeModification(metadataTree.getIdentifier(), Optional.of(metadataTree));
    }

}
