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
import org.hamcrest.Description;
import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipChangeState;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipChange;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;

/**
 * Abstract base class providing utility methods.
 *
 * @author Thomas Pantelis
 */
public class AbstractEntityOwnershipTest extends AbstractActorTest {
    protected void verifyEntityCandidate(final NormalizedNode<?, ?> node, final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName, final boolean expectPresent) {
        try {
            assertNotNull("Missing " + EntityOwners.QNAME.toString(), node);
            assertTrue(node instanceof ContainerNode);

            final ContainerNode entityOwnersNode = (ContainerNode) node;

            final MapEntryNode entityTypeEntry = getMapEntryNodeChild(entityOwnersNode, EntityType.QNAME,
                    ENTITY_TYPE_QNAME, entityType, true);

            final MapEntryNode entityEntry = getMapEntryNodeChild(entityTypeEntry, ENTITY_QNAME, ENTITY_ID_QNAME,
                    entityId, true);

            getMapEntryNodeChild(entityEntry, Candidate.QNAME, CANDIDATE_NAME_QNAME, candidateName, expectPresent);
        } catch(final AssertionError e) {
            throw new AssertionError("Verification of entity candidate failed - returned data was: " + node, e);
        }
    }

    protected void verifyEntityCandidate(final String entityType, final YangInstanceIdentifier entityId, final String candidateName,
            final Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader, final boolean expectPresent) {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            final NormalizedNode<?, ?> node = reader.apply(ENTITY_OWNERS_PATH);
            try {
                verifyEntityCandidate(node, entityType, entityId, candidateName, expectPresent);
                return;
            } catch (final AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    protected void verifyEntityCandidate(final String entityType, final YangInstanceIdentifier entityId, final String candidateName,
            final Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader) {
        verifyEntityCandidate(entityType, entityId, candidateName, reader, true);
    }

    protected MapEntryNode getMapEntryNodeChild(final DataContainerNode<? extends PathArgument> parent, final QName childMap,
            final QName child, final Object key, final boolean expectPresent) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> childNode =
                parent.getChild(new NodeIdentifier(childMap));
        assertEquals("Missing " + childMap.toString(), true, childNode.isPresent());

        final MapNode entityTypeMapNode = (MapNode) childNode.get();
        final Optional<MapEntryNode> entityTypeEntry = entityTypeMapNode.getChild(new NodeIdentifierWithPredicates(
                childMap, child, key));
        if(expectPresent && !entityTypeEntry.isPresent()) {
            fail("Missing " + childMap.toString() + " entry for " + key + ". Actual: " + entityTypeMapNode.getValue());
        } else if(!expectPresent && entityTypeEntry.isPresent()) {
            fail("Found unexpected " + childMap.toString() + " entry for " + key);
        }

        return entityTypeEntry.isPresent() ? entityTypeEntry.get() : null;
    }

    static void verifyOwner(final String expected, final String entityType, final YangInstanceIdentifier entityId,
            final Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader) {
        AssertionError lastError = null;
        final YangInstanceIdentifier entityPath = entityPath(entityType, entityId).node(ENTITY_OWNER_QNAME);
        final Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            try {
                final NormalizedNode<?, ?> node = reader.apply(entityPath);
                Assert.assertNotNull("Owner was not set for entityId: " + entityId, node);
                Assert.assertEquals("Entity owner", expected, node.getValue().toString());
                return;
            } catch(final AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    protected void verifyNodeRemoved(final YangInstanceIdentifier path,
            final Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader) {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            try {
                final NormalizedNode<?, ?> node = reader.apply(path);
                Assert.assertNull("Node was not removed at path: " + path, node);
                return;
            } catch(final AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    static void writeNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> node, final ShardDataTree shardDataTree)
            throws DataValidationFailedException {
        final DataTreeModification modification = shardDataTree.newModification();
        modification.merge(path, node);
        commit(shardDataTree, modification);
    }

    static void deleteNode(final YangInstanceIdentifier path, final ShardDataTree shardDataTree)
            throws DataValidationFailedException {
        final DataTreeModification modification = shardDataTree.newModification();
        modification.delete(path);
        commit(shardDataTree, modification);
    }

    static void commit(final ShardDataTree shardDataTree, final DataTreeModification modification)
            throws DataValidationFailedException {
        shardDataTree.notifyListeners(shardDataTree.commit(modification));
    }

    static DOMEntityOwnershipChange ownershipChange(final DOMEntity expEntity, final boolean expWasOwner, final boolean expIsOwner, final boolean expHasOwner) {
        return Matchers.argThat(new ArgumentMatcher<DOMEntityOwnershipChange>() {

            @Override
            public boolean matches(final Object argument) {
                final DOMEntityOwnershipChange change = (DOMEntityOwnershipChange) argument;
                return expEntity.equals(change.getEntity()) && expWasOwner == change.getState().wasOwner()
                        && expIsOwner == change.getState().isOwner() && expHasOwner == change.getState().hasOwner();
            }
        });
    }

    static DOMEntityOwnershipChange ownershipChange(final DOMEntity expEntity) {
        return Matchers.argThat(new ArgumentMatcher<DOMEntityOwnershipChange>() {
            @Override
            public boolean matches(final Object argument) {
                final DOMEntityOwnershipChange change = (DOMEntityOwnershipChange) argument;
                return expEntity.equals(change.getEntity());
            }

            @Override
            public void describeTo(final Description description) {
                description.appendValue(new DOMEntityOwnershipChange(expEntity,
                        EntityOwnershipChangeState.from(false, false, false)));
            }
        });
    }

    static EntityOwnershipChange ownershipChange(final Entity expEntity, final boolean expWasOwner,
            final boolean expIsOwner, final boolean expHasOwner) {
        return Matchers.argThat(new ArgumentMatcher<EntityOwnershipChange>() {
            @Override
            public boolean matches(final Object argument) {
                final EntityOwnershipChange change = (EntityOwnershipChange) argument;
                return expEntity.equals(change.getEntity()) && expWasOwner == change.wasOwner() &&
                        expIsOwner == change.isOwner() && expHasOwner == change.hasOwner();
            }

            @Override
            public void describeTo(final Description description) {
                description.appendValue(new EntityOwnershipChange(expEntity, expWasOwner, expIsOwner, expHasOwner));
            }
        });
    }

    static EntityOwnershipChange ownershipChange(final Entity expEntity) {
        return Matchers.argThat(new ArgumentMatcher<EntityOwnershipChange>() {
            @Override
            public boolean matches(final Object argument) {
                final EntityOwnershipChange change = (EntityOwnershipChange) argument;
                return expEntity.equals(change.getEntity());
            }

            @Override
            public void describeTo(final Description description) {
                description.appendValue(new EntityOwnershipChange(expEntity, false, false, false));
            }
        });
    }
}
