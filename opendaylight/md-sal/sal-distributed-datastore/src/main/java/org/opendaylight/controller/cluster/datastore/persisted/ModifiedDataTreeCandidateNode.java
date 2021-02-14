/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Optional;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

/**
 * A deserialized {@link DataTreeCandidateNode} which represents a modification in
 * one of its children.
 */
abstract class ModifiedDataTreeCandidateNode extends AbstractDataTreeCandidateNode {
    private final Collection<DataTreeCandidateNode> children;

    private ModifiedDataTreeCandidateNode(final ModificationType type,
            final Collection<DataTreeCandidateNode> children) {
        super(type);
        this.children = requireNonNull(children);
    }

    static DataTreeCandidateNode create(final ModificationType type, final Collection<DataTreeCandidateNode> children) {
        return new ModifiedDataTreeCandidateNode(type, children) {
            @Override
            public PathArgument getIdentifier() {
                throw new UnsupportedOperationException("Root node does not have an identifier");
            }
        };
    }

    static DataTreeCandidateNode create(final PathArgument identifier, final ModificationType type,
            final Collection<DataTreeCandidateNode> children) {
        return new ModifiedDataTreeCandidateNode(type, children) {
            @Override
            public PathArgument getIdentifier() {
                return identifier;
            }
        };
    }

    @Override
    public final Optional<NormalizedNode> getDataAfter() {
        throw new UnsupportedOperationException("After-image not available after serialization");
    }

    @Override
    public final Collection<DataTreeCandidateNode> getChildNodes() {
        return children;
    }
}
