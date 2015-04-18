/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

abstract class AbstractDataTreeCandidateNode implements DataTreeCandidateNode {
    private final ModificationType type;

    protected AbstractDataTreeCandidateNode(final ModificationType type) {
        this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public final DataTreeCandidateNode getModifiedChild(final PathArgument identifier) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final ModificationType getModificationType() {
        return type;
    }

    @Override
    public final Optional<NormalizedNode<?, ?>> getDataBefore() {
        throw new UnsupportedOperationException("Before-image not available after serialization");
    }
}
