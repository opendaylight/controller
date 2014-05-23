/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedNodeContainer;

public final class TreeNodeFactory {
    private TreeNodeFactory() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Create a new AbstractTreeNode from a data node, descending recursively as needed.
     * This method should only ever be used for new data.
     *
     * @param data data node
     * @param version data node version
     * @return new AbstractTreeNode instance, covering the data tree provided
     */
    public static final TreeNode createTreeNode(final NormalizedNode<?, ?> data, final Version version) {
        if (data instanceof NormalizedNodeContainer<?, ?, ?>) {
            @SuppressWarnings("unchecked")
            NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>> container = (NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>>) data;
            return ContainerNode.create(version, container);

        }
        if (data instanceof OrderedNodeContainer<?>) {
            @SuppressWarnings("unchecked")
            OrderedNodeContainer<NormalizedNode<?, ?>> container = (OrderedNodeContainer<NormalizedNode<?, ?>>) data;
            return ContainerNode.create(version, container);
        }

        return new ValueNode(data, version);
    }
}
