/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreTreeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.primitives.UnsignedLong;

public interface MutableTreeNode extends StoreTreeNode<TreeNode> {
    void setData(NormalizedNode<?, ?> data);
    void setSubtreeVersion(UnsignedLong subtreeVersion);
    void addChild(TreeNode child);
    void removeChild(PathArgument id);
    TreeNode seal();
}
