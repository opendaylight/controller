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

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.primitives.UnsignedLong;

public class StoreMetadataNode implements Immutable, Identifiable<PathArgument>, StoreTreeNode<StoreMetadataNode> {

    private final UnsignedLong nodeVersion;
    private final UnsignedLong subtreeVersion;
    private final NormalizedNode<?, ?> data;

    private final Map<PathArgument, StoreMetadataNode> children;

    /**
     *
     * @param data
     * @param nodeVersion
     * @param subtreeVersion
     * @param children Map of children, must not be modified externally
     */
    protected StoreMetadataNode(final NormalizedNode<?, ?> data, final UnsignedLong nodeVersion,
            final UnsignedLong subtreeVersion, final Map<PathArgument, StoreMetadataNode> children) {
        this.nodeVersion = nodeVersion;
        this.subtreeVersion = subtreeVersion;
        this.data = data;
        this.children = Preconditions.checkNotNull(children);
    }

    public static Builder builder() {
        return new Builder();
    }

    public UnsignedLong getNodeVersion() {
        return this.nodeVersion;
    }

    @Override
    public PathArgument getIdentifier() {
        return data.getIdentifier();
    }

    public UnsignedLong getSubtreeVersion() {
        return subtreeVersion;
    }

    public NormalizedNode<?, ?> getData() {
        return this.data;
    }

    public Iterable<StoreMetadataNode> getChildren() {
        return Iterables.unmodifiableIterable(children.values());
    }

    @Override
    public Optional<StoreMetadataNode> getChild(final PathArgument key) {
        return Optional.fromNullable(children.get(key));
    }

    @Override
    public String toString() {
        return "StoreMetadataNode [identifier=" + getIdentifier() + ", nodeVersion=" + nodeVersion + "]";
    }

    public static Optional<UnsignedLong> getVersion(final Optional<StoreMetadataNode> currentMetadata) {
        if (currentMetadata.isPresent()) {
            return Optional.of(currentMetadata.get().getNodeVersion());
        }
        return Optional.absent();
    }

    public static Optional<StoreMetadataNode> getChild(final Optional<StoreMetadataNode> parent,
            final PathArgument child) {
        if (parent.isPresent()) {
            return parent.get().getChild(child);
        }
        return Optional.absent();
    }

    public static final StoreMetadataNode createRecursively(final NormalizedNode<?, ?> node,
            final UnsignedLong nodeVersion, final UnsignedLong subtreeVersion) {
        Builder builder = builder() //
                .setNodeVersion(nodeVersion) //
                .setSubtreeVersion(subtreeVersion) //
                .setData(node);
        if (node instanceof NormalizedNodeContainer<?, ?, ?>) {

            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>> nodeContainer = (NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>>) node;
            for (NormalizedNode<?, ?> subNode : nodeContainer.getValue()) {
                builder.add(createRecursively(subNode, nodeVersion, subtreeVersion));
            }
        }
        return builder.build();
    }

    public static class Builder {

        private UnsignedLong nodeVersion;
        private UnsignedLong subtreeVersion;
        private NormalizedNode<?, ?> data;
        private Map<PathArgument, StoreMetadataNode> children = new LinkedHashMap<>();
        private boolean dirty = false;

        private Builder() {}


        public UnsignedLong getVersion() {
            return nodeVersion;

        }

        public Builder setNodeVersion(final UnsignedLong version) {
            this.nodeVersion = version;
            return this;
        }

        public Builder setSubtreeVersion(final UnsignedLong version) {
            this.subtreeVersion = version;
            return this;
        }

        public Builder setData(final NormalizedNode<?, ?> data) {
            this.data = data;
            return this;
        }

        public Builder add(final StoreMetadataNode node) {
            if (dirty) {
                children = new LinkedHashMap<>(children);
                dirty = false;
            }
            children.put(node.getIdentifier(), node);
            return this;
        }

        public StoreMetadataNode build() {
            checkState(data != null, "Data node should not be null.");
            checkState(subtreeVersion.compareTo(nodeVersion) >= 0,
                    "Subtree version must be equals or greater than node version.");
            dirty = true;
            return new StoreMetadataNode(data, nodeVersion, subtreeVersion, children);
        }
    }

    public static StoreMetadataNode createRecursively(final NormalizedNode<?, ?> node, final UnsignedLong version) {
        return createRecursively(node, version, version);
    }

}
