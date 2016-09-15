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
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidatePath;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;

import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.hamcrest.Description;
import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract base class providing utility methods.
 *
 * @author Thomas Pantelis
 */
public class AbstractEntityOwnershipTest extends AbstractActorTest {
    protected final Logger testLog = LoggerFactory.getLogger(getClass());

    private static final AtomicInteger NEXT_SHARD_NUM = new AtomicInteger();

    protected void verifyEntityCandidate(NormalizedNode<?, ?> node, String entityType,
            YangInstanceIdentifier entityId, String candidateName, boolean expectPresent) {
        try {
            assertNotNull("Missing " + EntityOwners.QNAME.toString(), node);
            assertTrue(node instanceof ContainerNode);

            ContainerNode entityOwnersNode = (ContainerNode) node;

            MapEntryNode entityTypeEntry = getMapEntryNodeChild(entityOwnersNode, EntityType.QNAME,
                    ENTITY_TYPE_QNAME, entityType, true);

            MapEntryNode entityEntry = getMapEntryNodeChild(entityTypeEntry, ENTITY_QNAME, ENTITY_ID_QNAME,
                    entityId, true);

            getMapEntryNodeChild(entityEntry, Candidate.QNAME, CANDIDATE_NAME_QNAME, candidateName, expectPresent);
        } catch(AssertionError e) {
            throw new AssertionError("Verification of entity candidate failed - returned data was: " + node, e);
        }
    }

    protected void verifyEntityCandidate(String entityType, YangInstanceIdentifier entityId, String candidateName,
            Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader, boolean expectPresent) {
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            NormalizedNode<?, ?> node = reader.apply(ENTITY_OWNERS_PATH);
            try {
                verifyEntityCandidate(node, entityType, entityId, candidateName, expectPresent);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    protected void verifyEntityCandidate(String entityType, YangInstanceIdentifier entityId, String candidateName,
            Function<YangInstanceIdentifier,NormalizedNode<?,?>> reader) {
        verifyEntityCandidate(entityType, entityId, candidateName, reader, true);
    }

    protected MapEntryNode getMapEntryNodeChild(DataContainerNode<? extends PathArgument> parent, QName childMap,
            QName child, Object key, boolean expectPresent) {
        Optional<DataContainerChild<? extends PathArgument, ?>> childNode =
                parent.getChild(new NodeIdentifier(childMap));
        assertEquals("Missing " + childMap.toString(), true, childNode.isPresent());

        MapNode entityTypeMapNode = (MapNode) childNode.get();
        Optional<MapEntryNode> entityTypeEntry = entityTypeMapNode.getChild(new NodeIdentifierWithPredicates(
                childMap, child, key));
        if(expectPresent && !entityTypeEntry.isPresent()) {
            fail("Missing " + childMap.toString() + " entry for " + key + ". Actual: " + entityTypeMapNode.getValue());
        } else if(!expectPresent && entityTypeEntry.isPresent()) {
            fail("Found unexpected " + childMap.toString() + " entry for " + key);
        }

        return entityTypeEntry.isPresent() ? entityTypeEntry.get() : null;
    }

    static void verifyOwner(String expected, String entityType, YangInstanceIdentifier entityId,
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
        DataTreeModification modification = shardDataTree.newModification();
        modification.merge(path, node);
        commit(shardDataTree, modification);
    }

    static void deleteNode(YangInstanceIdentifier path, ShardDataTree shardDataTree)
            throws DataValidationFailedException {
        DataTreeModification modification = shardDataTree.newModification();
        modification.delete(path);
        commit(shardDataTree, modification);
    }

    static void commit(ShardDataTree shardDataTree, DataTreeModification modification)
            throws DataValidationFailedException {
        shardDataTree.notifyListeners(shardDataTree.commit(modification));
    }

    static DOMEntityOwnershipChange ownershipChange(final DOMEntity expEntity, final boolean expWasOwner,
            final boolean expIsOwner, final boolean expHasOwner) {
        return ownershipChange(expEntity, expWasOwner, expIsOwner, expHasOwner, false);
    }

    static DOMEntityOwnershipChange ownershipChange(final DOMEntity expEntity, final boolean expWasOwner,
            final boolean expIsOwner, final boolean expHasOwner, final boolean expInJeopardy) {
        return Matchers.argThat(new ArgumentMatcher<DOMEntityOwnershipChange>() {
            @Override
            public boolean matches(Object argument) {
                DOMEntityOwnershipChange change = (DOMEntityOwnershipChange) argument;
                return expEntity.equals(change.getEntity()) && expWasOwner == change.getState().wasOwner() &&
                        expIsOwner == change.getState().isOwner() && expHasOwner == change.getState().hasOwner() &&
                        expInJeopardy == change.inJeopardy();
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(new DOMEntityOwnershipChange(expEntity, EntityOwnershipChangeState.from(
                        expWasOwner, expIsOwner, expHasOwner), expInJeopardy));
            }
        });
    }

    static DOMEntityOwnershipChange ownershipChange(final DOMEntity expEntity) {
        return Matchers.argThat(new ArgumentMatcher<DOMEntityOwnershipChange>() {
            @Override
            public boolean matches(Object argument) {
                DOMEntityOwnershipChange change = (DOMEntityOwnershipChange) argument;
                return expEntity.equals(change.getEntity());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(new DOMEntityOwnershipChange(expEntity, EntityOwnershipChangeState.from(
                        false, false, false)));
            }
        });
    }

    static void verifyOwner(final TestActorRef<? extends EntityOwnershipShard> shard, String entityType,
            YangInstanceIdentifier entityId, String localMemberName) {
        verifyOwner(localMemberName, entityType, entityId, path -> {
            try {
                return AbstractShardTest.readStore(shard, path);
            } catch(Exception e) {
                return null;
            }
        });
    }

    static void verifyNoOwnerSet(TestActorRef<? extends EntityOwnershipShard> shard, String entityType,
            YangInstanceIdentifier entityId) {
        YangInstanceIdentifier entityPath = entityPath(entityType, entityId).node(ENTITY_OWNER_QNAME);
        try {
            NormalizedNode<?, ?> node = AbstractShardTest.readStore(shard, entityPath);
            if(node != null) {
                Assert.fail("Owner " + node.getValue() + " was set for " + entityPath);
            }

        } catch (Exception e) {
            throw new AssertionError("read failed", e);
        }
    }

    static void verifyRaftState(final TestActorRef<? extends EntityOwnershipShard> shard, Consumer<OnDemandRaftState> verifier)
            throws Exception {
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            FiniteDuration operationDuration = Duration.create(5, TimeUnit.SECONDS);
            Future<Object> future = Patterns.ask(shard, GetOnDemandRaftState.INSTANCE, new Timeout(operationDuration));
            OnDemandRaftState raftState = (OnDemandRaftState)Await.result(future, operationDuration);
            try {
                verifier.accept(raftState);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    static ShardIdentifier newShardId(String memberName) {
        return ShardIdentifier.create("entity-ownership", MemberName.forName(memberName),
            "operational" + NEXT_SHARD_NUM.getAndIncrement());
    }

    void verifyEntityCandidateRemoved(final TestActorRef<EntityOwnershipShard> shard, String entityType,
            YangInstanceIdentifier entityId, String candidateName) {
        verifyNodeRemoved(candidatePath(entityType, entityId, candidateName),
                path -> {
                    try {
                        return AbstractShardTest.readStore(shard, path);
                    } catch(Exception e) {
                        throw new AssertionError("Failed to read " + path, e);
                    }
            });
    }

    void verifyCommittedEntityCandidate(final TestActorRef<? extends EntityOwnershipShard> shard, String entityType,
            YangInstanceIdentifier entityId, String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName, path -> {
            try {
                return AbstractShardTest.readStore(shard, path);
            } catch(Exception e) {
                throw new AssertionError("Failed to read " + path, e);
            }
        });
    }

    void verifyNoEntityCandidate(final TestActorRef<? extends EntityOwnershipShard> shard, String entityType,
            YangInstanceIdentifier entityId, String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName, path -> {
            try {
                return AbstractShardTest.readStore(shard, path);
            } catch(Exception e) {
                throw new AssertionError("Failed to read " + path, e);
            }
        }, false);
    }
}
