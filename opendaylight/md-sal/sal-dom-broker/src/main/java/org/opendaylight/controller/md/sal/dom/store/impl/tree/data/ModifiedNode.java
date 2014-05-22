/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreTreeNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.TreeNode;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/**
 * Node Modification Node and Tree
 *
 * Tree which structurally resembles data tree and captures client modifications
 * to the data store tree.
 *
 * This tree is lazily created and populated via {@link #modifyChild(PathArgument)}
 * and {@link StoreMetadataNode} which represents original state {@link #getOriginal()}.
 */
final class ModifiedNode implements StoreTreeNode<ModifiedNode>, Identifiable<PathArgument>, NodeModification {

    public static final Predicate<ModifiedNode> IS_TERMINAL_PREDICATE = new Predicate<ModifiedNode>() {
        @Override
        public boolean apply(final ModifiedNode input) {
            switch (input.getType()) {
            case DELETE:
            case MERGE:
            case WRITE:
                return true;
            case SUBTREE_MODIFIED:
            case UNMODIFIED:
                return false;
            }

            throw new IllegalArgumentException(String.format("Unhandled modification type %s", input.getType()));
        }
    };

    private final Map<PathArgument, ModifiedNode> children = new LinkedHashMap<>();
    private final Optional<TreeNode> original;
    private final PathArgument identifier;
    private ModificationType modificationType = ModificationType.UNMODIFIED;
    private Optional<TreeNode> snapshotCache;
    private NormalizedNode<?, ?> value;

    private ModifiedNode(final PathArgument identifier, final Optional<TreeNode> original) {
        this.identifier = identifier;
        this.original = original;
    }

    /**
     *
     *
     * @return
     */
    public NormalizedNode<?, ?> getWrittenValue() {
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
    @Override
    public Optional<TreeNode> getOriginal() {
        return original;
    }

    /**
     * Returns modification type
     *
     * @return modification type
     */
    @Override
    public ModificationType getType() {
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
    public Optional<ModifiedNode> getChild(final PathArgument child) {
        return Optional.<ModifiedNode> fromNullable(children.get(child));
    }

    /**
     *
     * Returns child modification if child was modified, creates {@link ModifiedNode}
     * for child otherwise.
     *
     * If this node's {@link ModificationType} is {@link ModificationType#UNMODIFIED}
     * changes modification type to {@link ModificationType#SUBTREE_MODIFIED}
     *
     * @param child
     * @return {@link ModifiedNode} for specified child, with {@link #getOriginal()}
     *         containing child metadata if child was present in original data.
     */
    public ModifiedNode modifyChild(final PathArgument child) {
        clearSnapshot();
        if (modificationType == ModificationType.UNMODIFIED) {
            updateModificationType(ModificationType.SUBTREE_MODIFIED);
        }
        final ModifiedNode potential = children.get(child);
        if (potential != null) {
            return potential;
        }

        final Optional<TreeNode> currentMetadata;
        if (original.isPresent()) {
            final TreeNode orig = original.get();
            currentMetadata = orig.getChild(child);
        } else {
            currentMetadata = Optional.absent();
        }

        ModifiedNode newlyCreated = new ModifiedNode(child, currentMetadata);
        children.put(child, newlyCreated);
        return newlyCreated;
    }

    /**
     *
     * Returns all recorded direct child modification
     *
     * @return all recorded direct child modifications
     */
    @Override
    public Iterable<ModifiedNode> getChildren() {
        return children.values();
    }

    /**
     *
     * Records a delete for associated node.
     *
     */
    public void delete() {
        clearSnapshot();
        updateModificationType(ModificationType.DELETE);
        children.clear();
        this.value = null;
    }

    /**
     *
     * Records a write for associated node.
     *
     * @param value
     */
    public void write(final NormalizedNode<?, ?> value) {
        clearSnapshot();
        updateModificationType(ModificationType.WRITE);
        children.clear();
        this.value = value;
    }

    public void merge(final NormalizedNode<?, ?> data) {
        clearSnapshot();
        updateModificationType(ModificationType.MERGE);
        // FIXME: Probably merge with previous value.
        this.value = data;
    }

    void seal() {
        clearSnapshot();
        for (ModifiedNode child : children.values()) {
            child.seal();
        }
    }

    private void clearSnapshot() {
        snapshotCache = null;
    }

    public Optional<TreeNode> storeSnapshot(final Optional<TreeNode> snapshot) {
        snapshotCache = snapshot;
        return snapshot;
    }

    public Optional<Optional<TreeNode>> getSnapshotCache() {
        return Optional.fromNullable(snapshotCache);
    }

    @GuardedBy("this")
    private void updateModificationType(final ModificationType type) {
        modificationType = type;
        clearSnapshot();
    }

    @Override
    public String toString() {
        return "NodeModification [identifier=" + identifier + ", modificationType="
                + modificationType + ", childModification=" + children + "]";
    }

    public static ModifiedNode createUnmodified(final TreeNode metadataTree) {
        return new ModifiedNode(metadataTree.getIdentifier(), Optional.of(metadataTree));
    }
}
