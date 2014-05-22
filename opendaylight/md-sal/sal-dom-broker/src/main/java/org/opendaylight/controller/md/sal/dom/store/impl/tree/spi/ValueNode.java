/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedLong;

final class ValueNode extends AbstractTreeNode {
    private static final Logger LOG = LoggerFactory.getLogger(ValueNode.class);

    protected ValueNode(final NormalizedNode<?, ?> data, final UnsignedLong version) {
        super(data, version);
    }

    @Override
    public Optional<TreeNode> getChild(final PathArgument childId) {
        LOG.warn("Attempted to access child {} of value-node {}", childId, this);
        return Optional.absent();
    }

    @Override
    public UnsignedLong getSubtreeVersion() {
        return getVersion();
    }

    @Override
    public MutableTreeNode mutable() {
        /**
         * Value nodes can only we read/written/delete, which does a straight
         * replace. That means they don't haver need to be made mutable.
         */
        throw new UnsupportedOperationException(String.format("Attempted to mutate value-node %s", this));
    }
}
