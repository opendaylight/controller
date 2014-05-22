/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreTreeNode;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/*
 * A very basic data tree node. It has a version (when it was last modified),
 * a subtree version (when any of its children were modified) and some read-only
 * data.
 */
public interface TreeNode extends Identifiable<PathArgument>, StoreTreeNode<TreeNode> {
    /**
     * Get the data node version.
     *
     * @return Current data node version.
     */
    Version getVersion();

    /**
     * Get the subtree version.
     *
     * @return Current subtree version.
     */
    Version getSubtreeVersion();

    /**
     * Get a read-only view of the underlying data.
     *
     * @return Unmodifiable view of the underlying data.
     */
    NormalizedNode<?, ?> getData();

    /**
     * Get a mutable, isolated copy of the node.
     *
     * @return Mutable copy
     */
    MutableTreeNode mutable();
}
