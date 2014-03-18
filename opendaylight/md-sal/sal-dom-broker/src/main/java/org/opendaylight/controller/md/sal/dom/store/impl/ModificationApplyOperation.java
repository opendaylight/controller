/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreTreeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;

import com.google.common.base.Optional;

/**
 *
 * Operation responsible for applying {@link NodeModification} on tree.
 *
 * Operation is composite - operation on top level node consists of
 * suboperations on child nodes. This allows to walk operation hierarchy and
 * invoke suboperations independently.
 *
 * <b>Implementation notes</b>
 * <ul>
 * <li>
 * Implementations MUST expose all nested suboperations which operates on child
 * nodes expose via {@link #getChild(PathArgument)} method.
 * <li>Same suboperations SHOULD be used when invoked via
 * {@link #apply(NodeModification, Optional)} if applicable.
 *
 *
 * Hierarchical composite operation which is responsible for applying
 * modification on particular subtree and creating updated subtree
 *
 *
 */
public interface ModificationApplyOperation extends StoreTreeNode<ModificationApplyOperation> {

    /**
     *
     * Implementation of this operation must be stateless and must not change
     * state of this object.
     *
     * @param modification
     *            NodeModification to be applied
     * @param storeMeta
     *            Store Metadata Node on which NodeModification should be
     *            applied
     * @throws IllegalArgumentException
     *             If it is not possible to apply Operation on provided Metadata
     *             node
     * @return new {@link StoreMetadataNode} if operation resulted in updating
     *         node, {@link Optional#absent()} if {@link NodeModification}
     *         resulted in deletion of this node.
     */
    Optional<StoreMetadataNode> apply(NodeModification modification, Optional<StoreMetadataNode> storeMeta);

    /**
     * Returns a suboperation for specified tree node
     *
     * @return Reference to suboperation for specified tree node, {@link Optional#absent()}
     *    if suboperation is not supported for specified tree node.
     */
    @Override
    public Optional<ModificationApplyOperation> getChild(PathArgument child);

}
