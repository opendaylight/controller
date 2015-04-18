/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import java.util.Collection;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

abstract class RootDataTreeCandidateNode extends AbstractDataTreeCandidateNode {
    private RootDataTreeCandidateNode(final ModificationType type) {
        super(type);
    }

    @Override
    public final PathArgument getIdentifier() {
        throw new UnsupportedOperationException("Root node does not have an identifier");
    }

    static DataTreeCandidateNode createDeleted() {
        return new RootDataTreeCandidateNode(ModificationType.DELETE) {
            @Override
            public Optional<NormalizedNode<?, ?>> getDataAfter() {
                return Optional.absent();
            }

            @Override
            public Collection<DataTreeCandidateNode> getChildNodes() {
                throw new UnsupportedOperationException("Children not available after serialization");
            }
        };
    }

    static DataTreeCandidateNode createModified(final Collection<DataTreeCandidateNode> children) {
        return new RootDataTreeCandidateNode(ModificationType.SUBTREE_MODIFIED) {
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

    static DataTreeCandidateNode createUnmodified() {
        return new RootDataTreeCandidateNode(ModificationType.UNMODIFIED) {
            @Override
            public Optional<NormalizedNode<?, ?>> getDataAfter() {
                throw new UnsupportedOperationException("After-image not available after serialization");
            }

            @Override
            public Collection<DataTreeCandidateNode> getChildNodes() {
                throw new UnsupportedOperationException("Children not available after serialization");
            }
        };
    }
}
