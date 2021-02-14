/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import java.util.Collection;
import java.util.Optional;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

/**
 * A deserialized {@link DataTreeCandidateNode} which represents a deletion.
 */
abstract class DeletedDataTreeCandidateNode extends AbstractDataTreeCandidateNode {
    private DeletedDataTreeCandidateNode() {
        super(ModificationType.DELETE);
    }

    static DataTreeCandidateNode create() {
        return new DeletedDataTreeCandidateNode() {
            @Override
            public PathArgument getIdentifier() {
                throw new UnsupportedOperationException("Root node does not have an identifier");
            }
        };
    }

    static DataTreeCandidateNode create(final PathArgument identifier) {
        return new DeletedDataTreeCandidateNode() {
            @Override
            public PathArgument getIdentifier() {
                return identifier;
            }
        };
    }

    @Override
    public final Optional<NormalizedNode> getDataAfter() {
        return Optional.empty();
    }

    @Override
    public final Collection<DataTreeCandidateNode> getChildNodes() {
        // We would require the before-image to reconstruct the list of nodes which
        // were deleted.
        throw new UnsupportedOperationException("Children not available after serialization");
    }
}
