/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

final class DataTreeCandidateNodeImpl implements DataTreeCandidateNode {
    private final Map<PathArgument, DataTreeCandidateNode> children;
    private final NormalizedNode<?, ?> dataAfter;
    private final PathArgument identifier;
    private final ModificationType type;

    private DataTreeCandidateNodeImpl(final PathArgument id, final ModificationType type, final NormalizedNode<?, ?> dataAfter,
            final Map<PathArgument, DataTreeCandidateNode> children) {
        this.identifier = Preconditions.checkNotNull(id);
        this.type = Preconditions.checkNotNull(type);
        this.children = Preconditions.checkNotNull(children);
        this.dataAfter = dataAfter;
    }

    DataTreeCandidateNodeImpl(final PathArgument id, final ModificationType type) {
        this(id, type, null, Collections.<PathArgument, DataTreeCandidateNode>emptyMap());
    }

    DataTreeCandidateNodeImpl(final PathArgument id, final ModificationType type, final NormalizedNode<?, ?> dataAfter) {
        this(id, type, dataAfter, Collections.<PathArgument, DataTreeCandidateNode>emptyMap());
    }

    DataTreeCandidateNodeImpl(final PathArgument id, final ModificationType type, final Map<PathArgument, DataTreeCandidateNode> children) {
        this(id, type, null, children);
    }

    @Override
    public PathArgument getIdentifier() {
        return identifier;
    }

    @Override
    public Collection<DataTreeCandidateNode> getChildNodes() {
        return children.values();
    }

    @Override
    public DataTreeCandidateNode getModifiedChild(final PathArgument identifier) {
        return children.get(identifier);
    }

    @Override
    public ModificationType getModificationType() {
        return type;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataAfter() {
        return Optional.<NormalizedNode<?, ?>>fromNullable(dataAfter);
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataBefore() {
        throw new UnsupportedOperationException("Not available");
    }

}