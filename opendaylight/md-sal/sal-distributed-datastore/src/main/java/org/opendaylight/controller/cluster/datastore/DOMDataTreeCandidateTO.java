/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCandidate;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

final class DOMDataTreeCandidateTO implements DOMDataTreeCandidate {

    private final DOMDataTreeIdentifier rootPath;
    private final DataTreeCandidateNode rootNode;

    private DOMDataTreeCandidateTO(DOMDataTreeIdentifier rootPath, DataTreeCandidateNode rootNode) {
        this.rootPath = Preconditions.checkNotNull(rootPath);
        this.rootNode = Preconditions.checkNotNull(rootNode);
    }

    @Override
    public DOMDataTreeIdentifier getRootPath() {
        return rootPath;
    }

    @Override
    public DataTreeCandidateNode getRootNode() {
        return rootNode;
    }

    static DOMDataTreeCandidate create(DOMDataTreeIdentifier path, DataTreeCandidateNode node) {
        return new DOMDataTreeCandidateTO(path, node);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("rootPath", rootPath).add("rootNode", rootNode).toString();
    }

}
