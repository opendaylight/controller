/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Status.Failure;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.api.TermInfo;

/**
 * Unit tests for ShardManagerGetSnapshotReplyActor.
 *
 * @author Thomas Pantelis
 */
public class ShardManagerGetSnapshotReplyActorTest extends AbstractActorTest {
    private static final MemberName MEMBER_1 = MemberName.forName("member-1");

    @Test
    public void testSuccess() {
        TestKit kit = new TestKit(getSystem());

        List<String> shardList = Arrays.asList("shard1", "shard2", "shard3");
        ShardManagerSnapshot shardManagerSnapshot = new ShardManagerSnapshot(shardList);
        ActorRef replyActor = getSystem().actorOf(ShardManagerGetSnapshotReplyActor.props(
                shardList, "config", shardManagerSnapshot, kit.getRef(), "shard-manager", Duration.ofSeconds(100)),
                "testSuccess");

        kit.watch(replyActor);

        ByteState shard1SnapshotState = ByteState.of(new byte[]{1,2,3});
        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard1", MEMBER_1, "config").toString(),
            Snapshot.create(shard1SnapshotState, List.of(), 2, 1, 2, 1, new TermInfo(1, "member-1"), null)),
            ActorRef.noSender());

        ByteState shard2SnapshotState = ByteState.of(new byte[]{4,5,6});
        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard2", MEMBER_1, "config").toString(),
            Snapshot.create(shard2SnapshotState, List.of(), 2, 1, 2, 1, new TermInfo(1, "member-1"), null)),
            ActorRef.noSender());

        kit.expectNoMessage(Duration.ofMillis(500));

        ByteState shard3SnapshotState = ByteState.of(new byte[]{7,8,9});
        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard3", MEMBER_1, "config").toString(),
            Snapshot.create(shard3SnapshotState, List.of(), 2, 1, 2, 1, new TermInfo(1, "member-1"), null)),
            ActorRef.noSender());

        DatastoreSnapshot datastoreSnapshot = kit.expectMsgClass(DatastoreSnapshot.class);

        assertEquals("getType", "config", datastoreSnapshot.getType());
        assertEquals("getShardManagerSnapshot", shardManagerSnapshot.getShardList(),
                datastoreSnapshot.getShardManagerSnapshot().getShardList());
        final var shardSnapshots = datastoreSnapshot.getShardSnapshots();
        assertEquals("ShardSnapshot size", 3, shardSnapshots.size());
        assertEquals("ShardSnapshot 1 getName", "shard1", shardSnapshots.get(0).getName());
        assertEquals("ShardSnapshot 1 getSnapshot", shard1SnapshotState,
                shardSnapshots.get(0).getSnapshot().state());
        assertEquals("ShardSnapshot 2 getName", "shard2", shardSnapshots.get(1).getName());
        assertEquals("ShardSnapshot 2 getSnapshot", shard2SnapshotState,
                shardSnapshots.get(1).getSnapshot().state());
        assertEquals("ShardSnapshot 3 getName", "shard3", shardSnapshots.get(2).getName());
        assertEquals("ShardSnapshot 3 getSnapshot", shard3SnapshotState,
                shardSnapshots.get(2).getSnapshot().state());

        kit.expectMsgClass(Terminated.class);
    }

    @Test
    public void testGetSnapshotFailureReply() {
        TestKit kit = new TestKit(getSystem());

        ActorRef replyActor = getSystem().actorOf(ShardManagerGetSnapshotReplyActor.props(
            List.of("shard1", "shard2"), "config", null, kit.getRef(), "shard-manager", Duration.ofSeconds(100)),
            "testGetSnapshotFailureReply");

        kit.watch(replyActor);

        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard1", MEMBER_1, "config").toString(),
            Snapshot.create(ByteState.of(new byte[]{1,2,3}), List.of(), 2, 1, 2, 1, new TermInfo(1, "member-1"), null)),
            ActorRef.noSender());

        replyActor.tell(new Failure(new RuntimeException()), ActorRef.noSender());

        kit.expectMsgClass(Failure.class);
        kit.expectTerminated(replyActor);
    }

    @Test
    public void testGetSnapshotTimeout() {
        TestKit kit = new TestKit(getSystem());

        ActorRef replyActor = getSystem().actorOf(ShardManagerGetSnapshotReplyActor.props(
            List.of("shard1"), "config", null, kit.getRef(), "shard-manager", Duration.ofMillis(100)),
            "testGetSnapshotTimeout");

        kit.watch(replyActor);

        Failure failure = kit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", TimeoutException.class, failure.cause().getClass());
        kit.expectTerminated(replyActor);
    }
}
