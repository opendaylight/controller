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
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

abstract class ChildDataTreeCandidateNode extends AbstractDataTreeCandidateNode {
    private final PathArgument identifier;

    private ChildDataTreeCandidateNode(final ModificationType type, final PathArgument identifier) {
        super(type);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public final PathArgument getIdentifier() {
        return identifier;
    }

    static DataTreeCandidateNode createDeleted(final PathArgument identifier) {
        return new ChildDataTreeCandidateNode(ModificationType.DELETE, identifier) {
            @Override
            public Optional<NormalizedNode<?, ?>> getDataAfter() {
                return Optional.absent();
            }

            @Override
            public Collection<DataTreeCandidateNode> getChildNodes() {
                return Collections.emptyList();
            }
        };
    }

    static DataTreeCandidateNode createModified(final PathArgument identifier, final Collection<DataTreeCandidateNode> children) {
        return new ChildDataTreeCandidateNode(ModificationType.SUBTREE_MODIFIED, identifier) {
            @Override
            public Optional<NormalizedNode<?, ?>> getDataAfter() {
                throw new UnsupportedOperationException("After-image not available after serialization");
            }

            @Override
            public Collection<DataTreeCandidateNode> getChildNodes() {
                return children;
            }
        };
    }
}
