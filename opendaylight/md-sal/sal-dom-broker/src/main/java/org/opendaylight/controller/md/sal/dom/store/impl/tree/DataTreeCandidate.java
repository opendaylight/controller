/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

/**
 * An encapsulation of a validated data tree modification. This candidate
 * is ready for atomic commit to the datastore. It allows access to before-
 * and after-state as it will be seen in to subsequent commit. This capture
 * can be accessed for reference, but cannot be modified and the content
 * is limited to nodes which were affected by the modification from which
 * this instance originated.
 */
public interface DataTreeCandidate {
    /**
     * Get the candidate tree root node.
     *
     * @return Candidate tree root node
     */
    DataTreeCandidateNode getRootNode();

    /**
     * Get the candidate tree root path. This is the path of the root node
     * relative to the root of InstanceIdentifier namespace.
     *
     * @return Relative path of the root node
     */
    InstanceIdentifier getRootPath();
}
