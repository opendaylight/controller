/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.ShardTestKit;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.SuccessReply;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Unit tests for EntityOwnershipShard.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipShardTest extends AbstractEntityOwnershipTest {
    private static final String ENTITY_TYPE = "test type";
    private static final YangInstanceIdentifier ENTITY_ID1 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity1"));
    private static final YangInstanceIdentifier ENTITY_ID2 =
            YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "entity2"));
    private static final SchemaContext SCHEMA_CONTEXT = SchemaContextHelper.entityOwners();
    private static final AtomicInteger NEXT_SHARD_NUM = new AtomicInteger();
    private static final String LOCAL_MEMBER_NAME = "member-1";

    private final ShardIdentifier shardID = ShardIdentifier.builder().memberName(LOCAL_MEMBER_NAME)
            .shardName("entity-ownership").type("operational" + NEXT_SHARD_NUM.getAndIncrement()).build();

    private final Builder dataStoreContextBuilder = DatastoreContext.newBuilder();
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testOnRegisterCandidateLocal() throws Exception {
        ShardTestKit kit = new ShardTestKit(getSystem());

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps());

        kit.waitUntilLeader(shard);

        YangInstanceIdentifier entityId = ENTITY_ID1;
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        shard.tell(new RegisterCandidateLocal(candidate, entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithNoInitialLeader() throws Exception {
        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

        String peerId = actorFactory.generateActorId("follower");
        TestActorRef<MockFollower> peer = actorFactory.createTestActor(Props.create(MockFollower.class, peerId).
                withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps(
                ImmutableMap.<String, String>builder().put(peerId, peer.path().toString()).build()).
                withDispatcher(Dispatchers.DefaultDispatcherId()));

        YangInstanceIdentifier entityId = ENTITY_ID1;
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        shard.tell(new RegisterCandidateLocal(candidate, entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Now grant the vote so the shard becomes the leader. This should retry the commit.
        peer.underlyingActor().grantVote = true;

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithNoInitialConsensus() throws Exception {
        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2).
                shardTransactionCommitTimeoutInSeconds(1);

        String peerId = actorFactory.generateActorId("follower");
        TestActorRef<MockFollower> peer = actorFactory.createTestActor(Props.create(MockFollower.class, peerId).
                withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        MockFollower follower = peer.underlyingActor();
        follower.grantVote = true;

        // Drop AppendEntries so consensus isn't reached.
        follower.dropAppendEntries = true;

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps(
                ImmutableMap.<String, String>builder().put(peerId, peer.path().toString()).build()).
                withDispatcher(Dispatchers.DefaultDispatcherId()));

        kit.waitUntilLeader(shard);

        YangInstanceIdentifier entityId = ENTITY_ID1;
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        shard.tell(new RegisterCandidateLocal(candidate, entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Wait enough time for the commit to timeout.
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

        // Resume AppendEntries - the follower should ack the commit which should then result in the candidate
        // write being applied to the state.
        follower.dropAppendEntries = false;

        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithIsolatedLeader() throws Exception {
        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2).
                shardIsolatedLeaderCheckIntervalInMillis(50);

        String peerId = actorFactory.generateActorId("follower");
        TestActorRef<MockFollower> peer = actorFactory.createTestActor(Props.create(MockFollower.class, peerId).
                withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        MockFollower follower = peer.underlyingActor();
        follower.grantVote = true;

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps(
                ImmutableMap.<String, String>builder().put(peerId, peer.path().toString()).build()).
                withDispatcher(Dispatchers.DefaultDispatcherId()));

        kit.waitUntilLeader(shard);

        // Drop AppendEntries and wait enough time for the shard to switch to IsolatedLeader.
        follower.dropAppendEntries = true;
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        YangInstanceIdentifier entityId = ENTITY_ID1;
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        shard.tell(new RegisterCandidateLocal(candidate, entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        // Resume AppendEntries - the candidate write should now be committed.
        follower.dropAppendEntries = false;
        verifyCommittedEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);

        verifyOwner(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithRemoteLeader() throws Exception {
        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(100).
                shardBatchedModificationCount(5);

        String peerId = actorFactory.generateActorId("leader");
        TestActorRef<MockLeader> peer = actorFactory.createTestActor(Props.create(MockLeader.class).
                withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps(
                ImmutableMap.<String, String>builder().put(peerId, peer.path().toString()).build()).
                withDispatcher(Dispatchers.DefaultDispatcherId()));

        shard.tell(new AppendEntries(1L, peerId, -1L, -1L, Collections.<ReplicatedLogEntry>emptyList(), -1L, -1L,
                DataStoreVersions.CURRENT_VERSION), peer);

        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        shard.tell(new RegisterCandidateLocal(candidate, new Entity(ENTITY_TYPE, ENTITY_ID1)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        MockLeader leader = peer.underlyingActor();
        assertEquals("Leader received BatchedModifications", true, Uninterruptibles.awaitUninterruptibly(
                leader.modificationsReceived, 5, TimeUnit.SECONDS));
        verifyBatchedEntityCandidate(leader.getAndClearReceivedModifications(), ENTITY_TYPE, ENTITY_ID1,
                LOCAL_MEMBER_NAME);

        shard.tell(dataStoreContextBuilder.shardElectionTimeoutFactor(2).build(), ActorRef.noSender());

        // Test with initial commit timeout and subsequent retry.

        leader.modificationsReceived = new CountDownLatch(1);
        leader.sendReply = false;

        shard.tell(dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1).build(), ActorRef.noSender());

        shard.tell(new RegisterCandidateLocal(candidate, new Entity(ENTITY_TYPE, ENTITY_ID2)), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        assertEquals("Leader received BatchedModifications", true, Uninterruptibles.awaitUninterruptibly(
                leader.modificationsReceived, 5, TimeUnit.SECONDS));
        verifyBatchedEntityCandidate(leader.getAndClearReceivedModifications(), ENTITY_TYPE, ENTITY_ID2,
                LOCAL_MEMBER_NAME);

        // Send a bunch of registration messages quickly and verify.

        int max = 100;
        leader.delay = 4;
        leader.modificationsReceived = new CountDownLatch(max);
        List<YangInstanceIdentifier> entityIds = new ArrayList<>();
        for(int i = 1; i <= max; i++) {
            YangInstanceIdentifier id = YangInstanceIdentifier.of(QName.create("test", "2015-08-14", "test" + i));
            entityIds.add(id);
            shard.tell(new RegisterCandidateLocal(candidate, new Entity(ENTITY_TYPE, id)), kit.getRef());
        }

        assertEquals("Leader received BatchedModifications", true, Uninterruptibles.awaitUninterruptibly(
                leader.modificationsReceived, 10, TimeUnit.SECONDS));

        // Sleep a little to ensure no additional BatchedModifications are received.

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        List<Modification> receivedMods = leader.getAndClearReceivedModifications();
        for(int i = 0; i < max; i++) {
            verifyBatchedEntityCandidate(receivedMods.get(i), ENTITY_TYPE, entityIds.get(i), LOCAL_MEMBER_NAME);
        }

        assertEquals("# modifications received", max, receivedMods.size());
    }

    private void verifyCommittedEntityCandidate(TestActorRef<EntityOwnershipShard> shard, String entityType,
            YangInstanceIdentifier entityId, String candidateName) throws Exception {
        verifyEntityCandidate(readEntityOwners(shard), entityType, entityId, candidateName);
    }

    private void verifyBatchedEntityCandidate(List<Modification> mods, String entityType,
            YangInstanceIdentifier entityId, String candidateName) throws Exception {
        assertEquals("BatchedModifications size", 1, mods.size());
        verifyBatchedEntityCandidate(mods.get(0), entityType, entityId, candidateName);
    }

    private void verifyBatchedEntityCandidate(Modification mod, String entityType,
            YangInstanceIdentifier entityId, String candidateName) throws Exception {
        assertEquals("Modification type", MergeModification.class, mod.getClass());
        verifyEntityCandidate(((MergeModification)mod).getData(), entityType,
                entityId, candidateName);
    }

    private NormalizedNode<?, ?> readEntityOwners(TestActorRef<EntityOwnershipShard> shard) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            NormalizedNode<?, ?> node = AbstractShardTest.readStore(shard, ENTITY_OWNERS_PATH);
            if(node != null) {
                return node;
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        return null;
    }

    private void verifyOwner(final TestActorRef<EntityOwnershipShard> shard, String entityType, YangInstanceIdentifier entityId,
            String localMemberName) {
        verifyOwner(localMemberName, entityType, entityId, new Function<YangInstanceIdentifier, NormalizedNode<?,?>>() {
            @Override
            public NormalizedNode<?, ?> apply(YangInstanceIdentifier path) {
                try {
                    return AbstractShardTest.readStore(shard, path);
                } catch(Exception e) {
                    return null;
                }
            }
        });
    }

    private Props newShardProps() {
        return newShardProps(Collections.<String,String>emptyMap());
    }

    private Props newShardProps(Map<String,String> peers) {
        return EntityOwnershipShard.props(shardID, peers, dataStoreContextBuilder.build(), SCHEMA_CONTEXT,
                LOCAL_MEMBER_NAME);
    }

    public static class MockFollower extends UntypedActor {
        volatile boolean grantVote;
        volatile boolean dropAppendEntries;
        private final String myId;

        public MockFollower(String myId) {
            this.myId = myId;
        }

        @Override
        public void onReceive(Object message) {
            if(message instanceof RequestVote) {
                if(grantVote) {
                    getSender().tell(new RequestVoteReply(((RequestVote)message).getTerm(), true), getSelf());
                }
            } else if(message instanceof AppendEntries) {
                if(!dropAppendEntries) {
                    AppendEntries req = (AppendEntries) message;
                    long lastIndex = req.getLeaderCommit();
                    if (req.getEntries().size() > 0) {
                        for(ReplicatedLogEntry entry : req.getEntries()) {
                            lastIndex = entry.getIndex();
                        }
                    }

                    getSender().tell(new AppendEntriesReply(myId, req.getTerm(), true, lastIndex, req.getTerm(),
                            DataStoreVersions.CURRENT_VERSION), getSelf());
                }
            }
        }
    }

    public static class MockLeader extends UntypedActor {
        volatile CountDownLatch modificationsReceived = new CountDownLatch(1);
        List<Modification> receivedModifications = new ArrayList<>();
        volatile boolean sendReply = true;
        volatile long delay;

        @Override
        public void onReceive(Object message) {
            if(message instanceof BatchedModifications) {
                if(delay > 0) {
                    Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
                }

                if(sendReply) {
                    BatchedModifications mods = (BatchedModifications) message;
                    synchronized (receivedModifications) {
                        for(int i = 0; i < mods.getModifications().size(); i++) {
                            receivedModifications.add(mods.getModifications().get(i));
                            modificationsReceived.countDown();
                        }
                    }

                    getSender().tell(CommitTransactionReply.INSTANCE.toSerializable(), getSelf());
                } else {
                    sendReply = true;
                }
            }
        }

        List<Modification> getAndClearReceivedModifications() {
            synchronized (receivedModifications) {
                List<Modification> ret = new ArrayList<>(receivedModifications);
                receivedModifications.clear();
                return ret;
            }
        }
    }
}
