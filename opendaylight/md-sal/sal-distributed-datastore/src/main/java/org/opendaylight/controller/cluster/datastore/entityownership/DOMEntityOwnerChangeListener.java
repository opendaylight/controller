/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.createDOMEntity;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import java.util.Collection;
import java.util.Objects;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for entity owner changes and notifies the DOMEntityOwnershipListenerSupport appropriately.
 */
public class DOMEntityOwnerChangeListener extends AbstractEntityOwnerChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(DOMEntityOwnerChangeListener.class);

    private final String localMemberName;
    private final DOMEntityOwnershipListenerSupport listenerSupport;

    DOMEntityOwnerChangeListener(final MemberName localMemberName,
            final DOMEntityOwnershipListenerSupport listenerSupport) {
        this.localMemberName = Verify.verifyNotNull(localMemberName.getName());
        this.listenerSupport = Preconditions.checkNotNull(listenerSupport);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        for (final DataTreeCandidate change : changes) {
            final DataTreeCandidateNode changeRoot = change.getRootNode();
            final LeafNode<?> ownerLeaf = (LeafNode<?>) changeRoot.getDataAfter().get();

            LOG.debug("{}: Entity node changed: {}, {}", logId(), changeRoot.getModificationType(),
                    change.getRootPath());

            final String newOwner = extractOwner(ownerLeaf);

            String origOwner = null;
            final Optional<NormalizedNode<?, ?>> dataBefore = changeRoot.getDataBefore();
            if (dataBefore.isPresent()) {
                origOwner = extractOwner((LeafNode<?>) changeRoot.getDataBefore().get());
            }

            LOG.debug("{}: New owner: {}, Original owner: {}", logId(), newOwner, origOwner);

            if (!Objects.equals(origOwner, newOwner)) {
                final boolean isOwner = localMemberName.equals(newOwner);
                final boolean wasOwner = localMemberName.equals(origOwner);
                final boolean hasOwner = !Strings.isNullOrEmpty(newOwner);

                final DOMEntity entity = createDOMEntity(change.getRootPath());

                LOG.debug(
                        "{}: Calling notifyEntityOwnershipListeners: entity: {}, wasOwner: {}, isOwner: {}, hasOwner: {}",
                        logId(), entity, wasOwner, isOwner, hasOwner);

                listenerSupport.notifyEntityOwnershipListeners(entity, wasOwner, isOwner, hasOwner);
            }
        }
    }

    private String logId() {
        return listenerSupport.getLogId();
    }
}
