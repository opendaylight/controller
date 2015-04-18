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

final class PartialDataTreeCandidateNode implements DataTreeCandidateNode {
    private final Collection<DataTreeCandidateNode> children;
    private final Optional<NormalizedNode<?, ?>> data;
    private final PathArgument identifier;
    private final ModificationType type;

    PartialDataTreeCandidateNode() {
        this.type = ModificationType.DELETE;
        this.identifier = null;
        this.children = Collections.emptyList();
        this.data = Optional.absent();
    }

    PartialDataTreeCandidateNode(final PathArgument identifier) {
        this.type = ModificationType.DELETE;
        this.identifier = Preconditions.checkNotNull(identifier);
        this.children = Collections.emptyList();
        this.data = Optional.absent();
    }

    PartialDataTreeCandidateNode(final PathArgument identifier, final Collection<DataTreeCandidateNode> children) {
        this.type = ModificationType.SUBTREE_MODIFIED;
        this.identifier = Preconditions.checkNotNull(identifier);
        this.children = Preconditions.checkNotNull(children);
        this.data = null;
    }

    @Override
    public PathArgument getIdentifier() {
        return identifier;
    }

    @Override
    public Collection<DataTreeCandidateNode> getChildNodes() {
        return children;
    }

    @Override
    public DataTreeCandidateNode getModifiedChild(final PathArgument identifier) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ModificationType getModificationType() {
        return type;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataAfter() {
        return data;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataBefore() {
        return null;
    }

}
