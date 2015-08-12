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
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NAME_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_TYPE_QNAME;
import com.google.common.base.Optional;
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
                    ENTITY_TYPE_QNAME, entityType);

            MapEntryNode entityEntry = getMapEntryNodeChild(entityTypeEntry, ENTITY_QNAME, ENTITY_ID_QNAME, entityId);

            getMapEntryNodeChild(entityEntry, Candidate.QNAME, CANDIDATE_NAME_QNAME, candidateName);
        } catch(AssertionError e) {
            throw new AssertionError("Verification of enitity candidate failed - returned data was: " + node, e);
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
}
