/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.EntityOwners;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Abstract base class providing utility methods.
 *
 * @author Thomas Pantelis
 */
public class AbstractEntityOwnershipTest extends AbstractActorTest {
    protected void verifyEntityCandidate(NormalizedNode<?, ?> node, String entityType,
            YangInstanceIdentifier entityId, String candidateName) {
        try {
            assertNotNull("Missing " + EntityOwners.QNAME.toString(), node);
            assertTrue(node instanceof ContainerNode);

            ContainerNode entityOwnersNode = (ContainerNode) node;

            MapEntryNode entityTypeEntry = getMapEntryNodeChild(entityOwnersNode, EntityType.QNAME,
                    EntityOwnershipShard.ENTITY_TYPE, entityType);

            MapEntryNode entityEntry = getMapEntryNodeChild(entityTypeEntry, EntityOwnershipShard.ENTITY_QNAME,
                    EntityOwnershipShard.ENTITY_ID, entityId);

            getMapEntryNodeChild(entityEntry, Candidate.QNAME, EntityOwnershipShard.CANDIDATE_NAME, candidateName);
        } catch(AssertionError e) {
            throw new AssertionError("Verification of entity candidate failed - returned data was: " + node, e);
        }
    }

    protected MapEntryNode getMapEntryNodeChild(DataContainerNode<? extends PathArgument> parent, QName childMap,
            QName child, Object key) {
        Optional<DataContainerChild<? extends PathArgument, ?>> childNode =
                parent.getChild(new NodeIdentifier(childMap));
        assertEquals("Missing " + childMap.toString(), true, childNode.isPresent());

        MapNode entityTypeMapNode = (MapNode) childNode.get();
        Optional<MapEntryNode> entityTypeEntry = entityTypeMapNode.getChild(new NodeIdentifierWithPredicates(
                childMap, child, key));
        assertEquals("Missing " + childMap.toString() + " entry for " + key, true, entityTypeEntry.isPresent());
        return entityTypeEntry.get();
    }

    protected String getEntityOwner(NormalizedNode<?,?> node,
                                    String entityType,
                                    YangInstanceIdentifier entityId){

        ContainerNode entityOwnersNode = (ContainerNode) node;

        MapEntryNode entityTypeEntry = getMapEntryNodeChild(entityOwnersNode, EntityType.QNAME,
                EntityOwnershipShard.ENTITY_TYPE, entityType);

        MapEntryNode entityEntry = getMapEntryNodeChild(entityTypeEntry, EntityOwnershipShard.ENTITY_QNAME,
                EntityOwnershipShard.ENTITY_ID, entityId);


        Collection<DataContainerChild<? extends PathArgument, ?>> children = entityEntry.getValue();

        for(DataContainerChild<? extends PathArgument, ?> child : children){
            if(child.getIdentifier().getNodeType().equals(EntityOwnershipShard.ENTITY_OWNER)){
                return child.getValue().toString();
            }
        }

        return null;
    }

    protected void verifyOwner(String expected, NormalizedNode<?,?> node,
                               String entityType,
                               YangInstanceIdentifier entityId){

        for(int i = 0;i<100;i++) {
            try {
                Assert.assertEquals(expected, getEntityOwner(node, entityType, entityId));
                return;
            } catch(AssertionError error){
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }
        throw new AssertionError("Verification of entity owner failed");
    }

}
