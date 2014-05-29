/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.TreeNode;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;

import com.google.common.base.Optional;

/**
 * Internal interface representing a modification action of a particular node.
 * It is used by the validation code to allow for a read-only view of the
 * modification tree as we should never modify that during validation.
 */
interface NodeModification extends Identifiable<PathArgument> {
    /**
     * Get the type of modification.
     *
     * @return Modification type.
     */
    ModificationType getType();

    /**
     * Get the original tree node to which the modification is to be applied.
     *
     * @return The original node, or {@link Optional#absent()} if the node is
     *         a new node.
     */
    Optional<TreeNode> getOriginal();

    /**
     * Get a read-only view of children nodes.
     *
     * @return Iterable of all children nodes.
     */
    Iterable<? extends NodeModification> getChildren();
}
