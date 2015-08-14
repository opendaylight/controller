/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_TYPE_QNAME;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
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
                node(EntityType.QNAME).node(EntityType.QNAME).node(ENTITY_QNAME).node(ENTITY_QNAME).build(), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
        for(DataTreeCandidate change: changes) {
            DataTreeCandidateNode changeRoot = change.getRootNode();
            MapEntryNode entityNode = (MapEntryNode) changeRoot.getDataAfter().get();

            LOG.debug("Entity node changed: {}, {}", changeRoot.getModificationType(), change.getRootPath());

            String newOwner = extractOwner(entityNode);

            String origOwner = null;
            Optional<NormalizedNode<?, ?>> dataBefore = changeRoot.getDataBefore();
            if(dataBefore.isPresent()) {
                MapEntryNode origEntityNode = (MapEntryNode) changeRoot.getDataBefore().get();
                origOwner = extractOwner(origEntityNode);
            }

            LOG.debug("New owner: {}, Original owner: {}", newOwner, origOwner);

            if(!Objects.equal(origOwner, newOwner)) {
                boolean isOwner = Objects.equal(localMemberName, newOwner);
                boolean wasOwner = Objects.equal(localMemberName, origOwner);
                if(isOwner || wasOwner) {
                    Entity entity = createEntity(change.getRootPath());

                    LOG.debug("Calling notifyEntityOwnershipListeners: entity: {}, wasOwner: {}, isOwner: {}",
                            entity, wasOwner, isOwner);

                    listenerSupport.notifyEntityOwnershipListeners(entity, wasOwner, isOwner);
                }
            }
        }
    }

    private Entity createEntity(YangInstanceIdentifier entityPath) {
        String entityType = null;
        YangInstanceIdentifier entityId = null;
        for(PathArgument pathArg: entityPath.getPathArguments()) {
            if(pathArg instanceof NodeIdentifierWithPredicates) {
                NodeIdentifierWithPredicates nodeKey = (NodeIdentifierWithPredicates) pathArg;
                Entry<QName, Object> key = nodeKey.getKeyValues().entrySet().iterator().next();
                if(ENTITY_TYPE_QNAME.equals(key.getKey())) {
                    entityType = key.getValue().toString();
                } else if(ENTITY_ID_QNAME.equals(key.getKey())) {
                    entityId = (YangInstanceIdentifier) key.getValue();
                }
            }
        }

        return new Entity(entityType, entityId);
    }

    private String extractOwner(MapEntryNode entityNode) {
        Optional<DataContainerChild<? extends PathArgument, ?>> ownerNode = entityNode.getChild(ENTITY_OWNER_NODE_ID);
        return ownerNode.isPresent() ? (String) ownerNode.get().getValue() : null;
    }
}
