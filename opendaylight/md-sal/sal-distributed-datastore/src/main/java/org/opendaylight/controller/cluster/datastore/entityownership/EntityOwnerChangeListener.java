/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.createEntity;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import java.util.Collection;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for entity owner changes and notifies the EntityOwnershipListenerSupport appropriately.
 *
 * @author Thomas Pantelis
 */
class EntityOwnerChangeListener implements DOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerChangeListener.class);

    private final String localMemberName;
    private final EntityOwnershipListenerSupport listenerSupport;

    EntityOwnerChangeListener(String localMemberName, EntityOwnershipListenerSupport listenerSupport) {
        this.localMemberName = localMemberName;
        this.listenerSupport = listenerSupport;
    }

    void init(ShardDataTree shardDataTree) {
        shardDataTree.registerTreeChangeListener(YangInstanceIdentifier.builder(ENTITY_OWNERS_PATH).
                node(EntityType.QNAME).node(EntityType.QNAME).node(ENTITY_QNAME).node(ENTITY_QNAME).node(EntityOwnersModel.ENTITY_OWNER_QNAME).build(), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
        for(DataTreeCandidate change: changes) {
            DataTreeCandidateNode changeRoot = change.getRootNode();
            LeafNode<?> ownerLeaf = (LeafNode<?>) changeRoot.getDataAfter().get();

            LOG.debug("{}: Entity node changed: {}, {}", logId(), changeRoot.getModificationType(), change.getRootPath());

            String newOwner = extractOwner(ownerLeaf);

            String origOwner = null;
            Optional<NormalizedNode<?, ?>> dataBefore = changeRoot.getDataBefore();
            if(dataBefore.isPresent()) {
                origOwner = extractOwner((LeafNode<?>) changeRoot.getDataBefore().get());
            }

            LOG.debug("{}: New owner: {}, Original owner: {}", logId(), newOwner, origOwner);

            if(!Objects.equal(origOwner, newOwner)) {
                boolean isOwner = Objects.equal(localMemberName, newOwner);
                boolean wasOwner = Objects.equal(localMemberName, origOwner);
                boolean hasOwner = newOwner != null && !newOwner.toString().isEmpty();

                Entity entity = createEntity(change.getRootPath());

                LOG.debug("{}: Calling notifyEntityOwnershipListeners: entity: {}, wasOwner: {}, isOwner: {}, hasOwner: {}",
                        logId(), entity, wasOwner, isOwner, hasOwner);

                listenerSupport.notifyEntityOwnershipListeners(entity, wasOwner, isOwner, hasOwner);
            }
        }
    }

    private static String extractOwner(LeafNode<?> ownerLeaf) {
        Object value = ownerLeaf.getValue();
        return value != null ? value.toString() : null;
    }

    private String logId() {
        return listenerSupport.getLogId();
    }
}
