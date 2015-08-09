/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.mockito.Mockito.mock;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.Map;
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
import org.opendaylight.controller.cluster.datastore.messages.SuccessReply;
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
    private static final QName ENTITY_QNAME1 = QName.create("test", "2015-08-14", "entity1");
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

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(ENTITY_QNAME1);
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        shard.tell(new RegisterCandidateLocal(candidate, entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        verifyEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    @Test
    public void testOnRegisterCandidateLocalWithNoInitialLeader() throws Exception {
        ShardTestKit kit = new ShardTestKit(getSystem());

        dataStoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

        String peerId = actorFactory.generateActorId("peer");
        TestActorRef<TestPeer> peer = actorFactory.createTestActor(Props.create(TestPeer.class, peerId).
                withDispatcher(Dispatchers.DefaultDispatcherId()), peerId);

        TestActorRef<EntityOwnershipShard> shard = actorFactory.createTestActor(newShardProps(
                ImmutableMap.<String, String>builder().put(peerId, peer.path().toString()).build()).
                withDispatcher(Dispatchers.DefaultDispatcherId()));

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(ENTITY_QNAME1);
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        shard.tell(new RegisterCandidateLocal(candidate, entity), kit.getRef());
        kit.expectMsgClass(SuccessReply.class);

        peer.underlyingActor().grantVote = true;

        verifyEntityCandidate(shard, ENTITY_TYPE, entityId, LOCAL_MEMBER_NAME);
    }

    private void verifyEntityCandidate(TestActorRef<EntityOwnershipShard> shard, String entityType,
            YangInstanceIdentifier entityId, String candidateName) throws Exception {
        verifyEntityCandidate(readEntityOwners(shard), entityType, entityId, candidateName);
    }

    private NormalizedNode<?, ?> readEntityOwners(TestActorRef<EntityOwnershipShard> shard) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            NormalizedNode<?, ?> node = AbstractShardTest.readStore(shard, EntityOwnershipShard.ENTITY_OWNERS_PATH);
            if(node != null) {
                return node;
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        return null;
    }

    private Props newShardProps() {
        return newShardProps(Collections.<String,String>emptyMap());
    }

    private Props newShardProps(Map<String,String> peers) {
        return EntityOwnershipShard.props(shardID, peers, dataStoreContextBuilder.build(), SCHEMA_CONTEXT,
                LOCAL_MEMBER_NAME);
    }

    public static class TestPeer extends UntypedActor {
        volatile boolean grantVote;
        private final String myId;

        public TestPeer(String myId) {
            this.myId = myId;
        }

        @Override
        public void onReceive(Object message) {
            if(message instanceof RequestVote) {
                if(grantVote) {
                    getSender().tell(new RequestVoteReply(((RequestVote)message).getTerm(), true), getSelf());
                }
            } else if(message instanceof AppendEntries) {
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
