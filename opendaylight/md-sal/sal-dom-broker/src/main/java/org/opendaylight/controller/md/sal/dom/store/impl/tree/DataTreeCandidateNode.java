/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;

/**
 * A single node within a {@link DataTreeCandidate}. The nodes are organized
 * in tree hierarchy, reflecting the modification from which this candidate
 * was created. The node itself exposes the before- and after-image of the
 * tree restricted to the modified nodes.
 */
public interface DataTreeCandidateNode {
    /**
     * Get the node identifier.
     *
     * @return The node identifier.
     */
    PathArgument getIdentifier();

    /**
     * Get an unmodifiable iterable of modified child nodes.
     *
     * @return Unmodifiable iterable of modified child nodes.
     */
    Iterable<DataTreeCandidateNode> getChildNodes();

    /**
     * Return the type of modification this node is undergoing.
     *
     * @return Node modification type.
     */
    ModificationType getModificationType();

    /**
     * Return the before-image of data corresponding to the node.
     *
     * @return Node data as they were present in the tree before
     *         the modification was applied.
     */
    Optional<NormalizedNode<?, ?>> getDataAfter();

    /**
     * Return the after-image of data corresponding to the node.
     *
     * @return Node data as they will be present in the tree after
     *         the modification is applied.
     */
    Optional<NormalizedNode<?, ?>> getDataBefore();
}
