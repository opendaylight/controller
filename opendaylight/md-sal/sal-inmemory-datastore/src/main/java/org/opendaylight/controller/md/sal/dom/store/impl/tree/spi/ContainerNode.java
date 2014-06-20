/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yangtools.util.MapAdaptor;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedNodeContainer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * A TreeNode capable of holding child nodes. The fact that any of the children
 * changed is tracked by the subtree version.
 */
final class ContainerNode extends AbstractTreeNode {
    private final Map<PathArgument, TreeNode> children;
    private final Version subtreeVersion;

    protected ContainerNode(final NormalizedNode<?, ?> data, final Version version,
            final Map<PathArgument, TreeNode> children, final Version subtreeVersion) {
        super(data, version);
        this.children = Preconditions.checkNotNull(children);
        this.subtreeVersion = Preconditions.checkNotNull(subtreeVersion);
    }

    @Override
    public Version getSubtreeVersion() {
        return subtreeVersion;
    }

    @Override
    public Optional<TreeNode> getChild(final PathArgument key) {
        return Optional.fromNullable(children.get(key));
    }

    @Override
    public MutableTreeNode mutable() {
        return new Mutable(this);
    }

    private static final class Mutable implements MutableTreeNode {
        private final Version version;
        private Map<PathArgument, TreeNode> children;
        private NormalizedNode<?, ?> data;
        private Version subtreeVersion;

        private Mutable(final ContainerNode parent) {
            this.data = parent.getData();
            this.children = MapAdaptor.getDefaultInstance().takeSnapshot(parent.children);
            this.subtreeVersion = parent.getSubtreeVersion();
            this.version = parent.getVersion();
        }

        @Override
        public Optional<TreeNode> getChild(final PathArgument child) {
            return Optional.fromNullable(children.get(child));
        }

        @Override
        public void setSubtreeVersion(final Version subtreeVersion) {
            this.subtreeVersion = Preconditions.checkNotNull(subtreeVersion);
        }

        @Override
        public void addChild(final TreeNode child) {
            children.put(child.getIdentifier(), child);
        }

        @Override
        public void removeChild(final PathArgument id) {
            children.remove(id);
        }

        @Override
        public TreeNode seal() {
            final TreeNode ret = new ContainerNode(data, version, MapAdaptor.getDefaultInstance().optimize(children), subtreeVersion);

            // This forces a NPE if this class is accessed again. Better than corruption.
            children = null;
            return ret;
        }

        @Override
        public void setData(final NormalizedNode<?, ?> data) {
            this.data = Preconditions.checkNotNull(data);
        }
    }

    private static ContainerNode create(final Version version, final NormalizedNode<?, ?> data,
            final Iterable<NormalizedNode<?, ?>> children) {

        final Map<PathArgument, TreeNode> map = new HashMap<>();
        for (NormalizedNode<?, ?> child : children) {
            map.put(child.getIdentifier(), TreeNodeFactory.createTreeNode(child, version));
        }

        return new ContainerNode(data, version, map, version);
    }

    public static ContainerNode create(final Version version, final NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>> container) {
        return create(version, container, container.getValue());
    }

    public static ContainerNode create(final Version version, final OrderedNodeContainer<NormalizedNode<?, ?>> container) {
        return create(version, container, container.getValue());
    }
}
