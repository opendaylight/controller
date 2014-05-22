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

import com.google.common.base.Preconditions;

/*
 * A very basic data tree node.
 */
abstract class AbstractTreeNode implements TreeNode {
    private final NormalizedNode<?, ?> data;
    private final Version version;

    protected AbstractTreeNode(final NormalizedNode<?, ?> data, final Version version) {
        this.data = Preconditions.checkNotNull(data);
        this.version = Preconditions.checkNotNull(version);
    }

    @Override
    public PathArgument getIdentifier() {
        return data.getIdentifier();
    }

    @Override
    public final Version getVersion() {
        return version;
    }

    @Override
    public final NormalizedNode<?, ?> getData() {
        return data;
    }
}
