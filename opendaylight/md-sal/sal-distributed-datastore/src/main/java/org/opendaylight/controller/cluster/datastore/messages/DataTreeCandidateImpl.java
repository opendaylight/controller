/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

final class DataTreeCandidateImpl implements DataTreeCandidate {
    private final YangInstanceIdentifier treeId;
    private final DataTreeCandidateNode node;

    public DataTreeCandidateImpl(final YangInstanceIdentifier treeId, final DataTreeCandidateNode node) {
        this.treeId = Preconditions.checkNotNull(treeId);
        this.node = Preconditions.checkNotNull(node);
    }

    @Override
    public DataTreeCandidateNode getRootNode() {
        return node;
    }

    @Override
    public YangInstanceIdentifier getRootPath() {
        return treeId;
    }
}