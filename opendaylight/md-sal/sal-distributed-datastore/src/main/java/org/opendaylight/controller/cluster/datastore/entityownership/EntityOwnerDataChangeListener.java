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
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DataChangeListener that listeners for entity owner changes and notifies the EntityOwnershipListenerSupport
 * appropriately.
 *
 * @author Thomas Pantelis
 */
class EntityOwnerDataChangeListener implements AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerDataChangeListener.class);

    private final String localMemberName;
    private final EntityOwnershipListenerSupport listenerSupport;

    EntityOwnerDataChangeListener(String localMemberName, EntityOwnershipListenerSupport listenerSupport) {
        this.localMemberName = localMemberName;
        this.listenerSupport = listenerSupport;
    }

    void init(ShardDataTree shardDataTree) {
        // We register at the entity-type level so we can easily obtain the entity type needed by the
        // EntityOwnershipListenerSupport for its notifications.
        shardDataTree.registerChangeListener(YangInstanceIdentifier.builder(ENTITY_OWNERS_PATH).
                node(EntityType.QNAME).node(EntityType.QNAME).build(), this, DataChangeScope.SUBTREE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        for(Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> updatedEntry: change.getUpdatedData().entrySet()) {
            NormalizedNode<?, ?> updatedNode = updatedEntry.getValue();
            YangInstanceIdentifier updatedPath = updatedEntry.getKey();

            // We're only interested in the entity children. We also need to check for the MapEntryNode
            // node class b/c we'll also get notified for the parent Map nodes.
            if(ENTITY_QNAME.equals(updatedNode.getNodeType()) && updatedNode instanceof MapEntry) {
                LOG.debug("Entity node updated: {}", updatedPath);

                MapEntryNode entityNode = (MapEntryNode) updatedNode;
                String newOwner = extractOwner(entityNode);

                String origOwner = null;
                MapEntryNode origEntityNode = (MapEntryNode) change.getOriginalData().get(updatedPath);
                if(origEntityNode != null) {
                    origOwner = extractOwner(origEntityNode);
                }

                LOG.debug("New owner: {}, Original owner: {}", newOwner, origOwner);

                boolean isOwner = Objects.equal(localMemberName, newOwner);
                boolean wasOwner = Objects.equal(localMemberName, origOwner);
                if(isOwner || wasOwner) {
                    Entity entity = createEntity(updatedPath);

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
