/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.EntityOwners;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableOrderedMapNodeBuilder;

/**
 * Utility methods for entity-owners yang model.
 *
 * @author Thomas Pantelis
 */
public final class EntityOwnersModel {
    static final  QName ENTITY_QNAME = org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.
            md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.Entity.QNAME;
    static final QName CANDIDATE_NAME_QNAME = QName.create(Candidate.QNAME, "name");
    static final QName ENTITY_ID_QNAME = QName.create(ENTITY_QNAME, "id");
    static final QName ENTITY_OWNER_QNAME = QName.create(ENTITY_QNAME, "owner");
    static final QName ENTITY_TYPE_QNAME = QName.create(EntityType.QNAME, "type");

    static final NodeIdentifier ENTITY_OWNERS_NODE_ID = new NodeIdentifier(EntityOwners.QNAME);
    static final NodeIdentifier ENTITY_OWNER_NODE_ID = new NodeIdentifier(ENTITY_OWNER_QNAME);
    static final NodeIdentifier ENTITY_NODE_ID = new NodeIdentifier(ENTITY_QNAME);
    static final NodeIdentifier ENTITY_ID_NODE_ID = new NodeIdentifier(ENTITY_ID_QNAME);
    static final NodeIdentifier ENTITY_TYPE_NODE_ID = new NodeIdentifier(ENTITY_TYPE_QNAME);
    static final NodeIdentifier CANDIDATE_NODE_ID = new NodeIdentifier(Candidate.QNAME);
    static final NodeIdentifier CANDIDATE_NAME_NODE_ID = new NodeIdentifier(CANDIDATE_NAME_QNAME);
    static final YangInstanceIdentifier ENTITY_OWNERS_PATH = YangInstanceIdentifier.of(EntityOwners.QNAME);
    static final YangInstanceIdentifier ENTITY_TYPES_PATH =
            YangInstanceIdentifier.of(EntityOwners.QNAME).node(EntityType.QNAME);

    static YangInstanceIdentifier entityPath(String entityType, YangInstanceIdentifier entityId) {
        return YangInstanceIdentifier.builder(ENTITY_OWNERS_PATH).node(EntityType.QNAME).
                nodeWithKey(EntityType.QNAME, ENTITY_TYPE_QNAME, entityType).node(ENTITY_QNAME).
                        nodeWithKey(ENTITY_QNAME, ENTITY_ID_QNAME, entityId).build();

    }

    static YangInstanceIdentifier candidatePath(String entityType, YangInstanceIdentifier entityId,
            String candidateName) {
        return YangInstanceIdentifier.builder(ENTITY_OWNERS_PATH).node(EntityType.QNAME).
                nodeWithKey(EntityType.QNAME, ENTITY_TYPE_QNAME, entityType).node(ENTITY_QNAME).
                        nodeWithKey(ENTITY_QNAME, ENTITY_ID_QNAME, entityId).node(Candidate.QNAME).
                                nodeWithKey(Candidate.QNAME, CANDIDATE_NAME_QNAME, candidateName).build();

    }

    static YangInstanceIdentifier candidatePath(YangInstanceIdentifier entityPath, String candidateName) {
        return YangInstanceIdentifier.builder(entityPath).node(Candidate.QNAME).nodeWithKey(
                Candidate.QNAME, CANDIDATE_NAME_QNAME, candidateName).build();
    }

    static NodeIdentifierWithPredicates candidateNodeKey(String candidateName) {
        return new NodeIdentifierWithPredicates(Candidate.QNAME, CANDIDATE_NAME_QNAME, candidateName);
    }

    static NormalizedNode<?, ?> entityOwnersWithCandidate(String entityType, YangInstanceIdentifier entityId,
            String candidateName) {
        return entityOwnersWithEntityTypeEntry(entityTypeEntryWithEntityEntry(entityType,
                entityEntryWithCandidateEntry(entityId, candidateName)));
    }

    static ContainerNode entityOwnersWithEntityTypeEntry(MapEntryNode entityTypeNode) {
        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                ENTITY_OWNERS_NODE_ID).addChild(ImmutableNodes.mapNodeBuilder(EntityType.QNAME).
                        addChild(entityTypeNode).build()).build();
    }

    static MapEntryNode entityTypeEntryWithEntityEntry(String entityType, MapEntryNode entityNode) {
        return ImmutableNodes.mapEntryBuilder(EntityType.QNAME,
                ENTITY_TYPE_QNAME, entityType).addChild(ImmutableNodes.mapNodeBuilder(
                        ENTITY_QNAME).addChild(entityNode).build()).build();
    }

    static MapEntryNode entityEntryWithCandidateEntry(YangInstanceIdentifier entityId, String candidateName) {
        return ImmutableNodes.mapEntryBuilder(ENTITY_QNAME, ENTITY_ID_QNAME, entityId).addChild(
                candidateEntry(candidateName)).build();
    }

    static MapNode candidateEntry(String candidateName) {
        return ImmutableOrderedMapNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(Candidate.QNAME)).
                addChild(candidateMapEntry(candidateName)).build();
    }

    static MapEntryNode candidateMapEntry(String candidateName) {
        return ImmutableNodes.mapEntry(Candidate.QNAME, CANDIDATE_NAME_QNAME, candidateName);
    }

    static MapEntryNode entityEntryWithOwner(YangInstanceIdentifier entityId, String owner) {
        return ImmutableNodes.mapEntryBuilder(ENTITY_QNAME, ENTITY_ID_QNAME, entityId).addChild(
                ImmutableNodes.leafNode(ENTITY_OWNER_QNAME, owner)).build();
    }

    public static String entityTypeFromEntityPath(YangInstanceIdentifier entityPath){
        YangInstanceIdentifier parent = entityPath;
        while(!parent.isEmpty()) {
            if (EntityType.QNAME.equals(parent.getLastPathArgument().getNodeType())) {
                YangInstanceIdentifier.NodeIdentifierWithPredicates entityTypeLastPathArgument = (YangInstanceIdentifier.NodeIdentifierWithPredicates) parent.getLastPathArgument();
                return (String) entityTypeLastPathArgument.getKeyValues().get(ENTITY_TYPE_QNAME);
            }
            parent = parent.getParent();
        }
        return null;
    }

    static Entity createEntity(YangInstanceIdentifier entityPath) {
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
}
