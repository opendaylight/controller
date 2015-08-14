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
import static org.junit.Assert.fail;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NAME_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_TYPE_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;

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
            throw new AssertionError("Verification of entity candidate failed - returned data was: " + node, e);
        }
    }

    protected void verifyEntityCandidate(String entityType, YangInstanceIdentifier entityId, String candidateName,
            Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader) {
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            NormalizedNode<?, ?> node = reader.apply(ENTITY_OWNERS_PATH);
            try {
                verifyEntityCandidate(node, entityType, entityId, candidateName);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    protected MapEntryNode getMapEntryNodeChild(DataContainerNode<? extends PathArgument> parent, QName childMap,
            QName child, Object key) {
        Optional<DataContainerChild<? extends PathArgument, ?>> childNode =
                parent.getChild(new NodeIdentifier(childMap));
        assertEquals("Missing " + childMap.toString(), true, childNode.isPresent());

        MapNode entityTypeMapNode = (MapNode) childNode.get();
        Optional<MapEntryNode> entityTypeEntry = entityTypeMapNode.getChild(new NodeIdentifierWithPredicates(
                childMap, child, key));
        if(!entityTypeEntry.isPresent()) {
            fail("Missing " + childMap.toString() + " entry for " + key + ". Actual: " + entityTypeMapNode.getValue());
        }
        return entityTypeEntry.get();
    }

    protected void verifyOwner(String expected, String entityType, YangInstanceIdentifier entityId,
            Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader) {
        AssertionError lastError = null;
        YangInstanceIdentifier entityPath = entityPath(entityType, entityId).node(ENTITY_OWNER_QNAME);
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            try {
                NormalizedNode<?, ?> node = reader.apply(entityPath);
                Assert.assertNotNull("Owner was not set for entityId: " + entityId, node);
                Assert.assertEquals("Entity owner", expected, node.getValue().toString());
                return;
            } catch(AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    protected void verifyNodeRemoved(YangInstanceIdentifier path,
            Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader) {
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            try {
                NormalizedNode<?, ?> node = reader.apply(path);
                Assert.assertNull("Node was not removed at path: " + path, node);
                return;
            } catch(AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    static void writeNode(YangInstanceIdentifier path, NormalizedNode<?, ?> node, ShardDataTree shardDataTree)
            throws DataValidationFailedException {
        DataTreeModification modification = shardDataTree.getDataTree().takeSnapshot().newModification();
        modification.merge(path, node);
        commit(shardDataTree, modification);
    }

    static void deleteNode(YangInstanceIdentifier path, ShardDataTree shardDataTree)
            throws DataValidationFailedException {
        DataTreeModification modification = shardDataTree.getDataTree().takeSnapshot().newModification();
        modification.delete(path);
        commit(shardDataTree, modification);
    }

    static void commit(ShardDataTree shardDataTree, DataTreeModification modification)
            throws DataValidationFailedException {
        modification.ready();

        shardDataTree.getDataTree().validate(modification);
        DataTreeCandidateTip candidate = shardDataTree.getDataTree().prepare(modification);
        shardDataTree.getDataTree().commit(candidate);
        shardDataTree.notifyListeners(candidate);
    }
}
