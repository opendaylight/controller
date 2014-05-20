/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import java.util.Collections;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeCandidateNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

final class NoopDataTreeCandidate extends AbstractDataTreeCandidate {
    private static final DataTreeCandidateNode ROOT = new DataTreeCandidateNode() {
        @Override
        public ModificationType getModificationType() {
            return ModificationType.UNMODIFIED;
        }

        @Override
        public Iterable<DataTreeCandidateNode> getChildNodes() {
            return Collections.emptyList();
        }

        @Override
        public PathArgument getIdentifier() {
            throw new IllegalStateException("Attempted to read identifier of the no-operation change");
        }

        @Override
        public Optional<NormalizedNode<?, ?>> getDataAfter() {
            return Optional.absent();
        }

        @Override
        public Optional<NormalizedNode<?, ?>> getDataBefore() {
            return Optional.absent();
        }
    };

    protected NoopDataTreeCandidate(final InstanceIdentifier rootPath, final NodeModification modificationRoot) {
        super(rootPath);
        Preconditions.checkArgument(modificationRoot.getModificationType() == ModificationType.UNMODIFIED);
    }

    @Override
    public DataTreeCandidateNode getRootNode() {
        return ROOT;
    }
}
