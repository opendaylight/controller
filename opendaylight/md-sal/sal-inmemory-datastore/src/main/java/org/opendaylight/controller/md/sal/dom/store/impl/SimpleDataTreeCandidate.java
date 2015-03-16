/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

final class SimpleDataTreeCandidate implements DataTreeCandidate {
    private final YangInstanceIdentifier rootPath;
    private final DataTreeCandidateNode rootNode;

    SimpleDataTreeCandidate(final YangInstanceIdentifier rootPath, final DataTreeCandidateNode rootNode) {
        this.rootPath = Preconditions.checkNotNull(rootPath);
        this.rootNode = Preconditions.checkNotNull(rootNode);
    }

    @Override
    public DataTreeCandidateNode getRootNode() {
        return rootNode;
    }

    @Override
    public YangInstanceIdentifier getRootPath() {
        return rootPath;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("rootPath", rootPath).add("rootNode", rootNode).toString();
    }
}