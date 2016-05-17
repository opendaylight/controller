/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.actor.Terminated;
import akka.testkit.JavaTestKit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Unit tests for ShardManagerGetSnapshotReplyActor.
 *
 * @author Thomas Pantelis
 */
public class ShardManagerGetSnapshotReplyActorTest extends AbstractActorTest {
    private static final MemberName MEMBER_1 = MemberName.forName("member-1");

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testSuccess() {
        JavaTestKit kit = new JavaTestKit(getSystem());

        byte[] shardManagerSnapshot = new byte[]{0,5,9};
        ActorRef replyActor = actorFactory.createActor(ShardManagerGetSnapshotReplyActor.props(
                Arrays.asList("shard1", "shard2", "shard3"), "config",
                shardManagerSnapshot, kit.getRef(), "shard-manager", Duration.create(100, TimeUnit.SECONDS)),
                    actorFactory.generateActorId("actor"));

        kit.watch(replyActor);

        byte[] shard1Snapshot = new byte[]{1,2,3};
        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard1", MEMBER_1, "config").toString(),
            shard1Snapshot), ActorRef.noSender());

        byte[] shard2Snapshot = new byte[]{4,5,6};
        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard2", MEMBER_1, "config").toString(),
            shard2Snapshot), ActorRef.noSender());

        kit.expectNoMsg(FiniteDuration.create(500, TimeUnit.MILLISECONDS));

        byte[] shard3Snapshot = new byte[]{7,8,9};
        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard3", MEMBER_1, "config").toString(),
            shard3Snapshot), ActorRef.noSender());

        DatastoreSnapshot datastoreSnapshot = kit.expectMsgClass(DatastoreSnapshot.class);

        assertEquals("getType", "config", datastoreSnapshot.getType());
        assertArrayEquals("getShardManagerSnapshot", shardManagerSnapshot, datastoreSnapshot.getShardManagerSnapshot());
        List<ShardSnapshot> shardSnapshots = datastoreSnapshot.getShardSnapshots();
        assertEquals("ShardSnapshot size", 3, shardSnapshots.size());
        assertEquals("ShardSnapshot 1 getName", "shard1", shardSnapshots.get(0).getName());
        assertArrayEquals("ShardSnapshot 1 getSnapshot", shard1Snapshot, shardSnapshots.get(0).getSnapshot());
        assertEquals("ShardSnapshot 2 getName", "shard2", shardSnapshots.get(1).getName());
        assertArrayEquals("ShardSnapshot 2 getSnapshot", shard2Snapshot, shardSnapshots.get(1).getSnapshot());
        assertEquals("ShardSnapshot 3 getName", "shard3", shardSnapshots.get(2).getName());
        assertArrayEquals("ShardSnapshot 3 getSnapshot", shard3Snapshot, shardSnapshots.get(2).getSnapshot());

        kit.expectMsgClass(Terminated.class);
    }

    @Test
    public void testGetSnapshotFailureReply() {
        JavaTestKit kit = new JavaTestKit(getSystem());

        byte[] shardManagerSnapshot = new byte[]{0,5,9};
        ActorRef replyActor = actorFactory.createActor(ShardManagerGetSnapshotReplyActor.props(
                Arrays.asList("shard1", "shard2"), "config",
                shardManagerSnapshot, kit.getRef(), "shard-manager", Duration.create(100, TimeUnit.SECONDS)),
                    actorFactory.generateActorId("actor"));

        kit.watch(replyActor);

        replyActor.tell(new GetSnapshotReply(ShardIdentifier.create("shard1", MEMBER_1, "config").toString(),
            new byte[]{1,2,3}), ActorRef.noSender());

        replyActor.tell(new Failure(new RuntimeException()), ActorRef.noSender());

        kit.expectMsgClass(Failure.class);
        kit.expectTerminated(replyActor);
    }

    @Test
    public void testGetSnapshotTimeout() {
        JavaTestKit kit = new JavaTestKit(getSystem());

        byte[] shardManagerSnapshot = new byte[]{0,5,9};
        ActorRef replyActor = actorFactory.createActor(ShardManagerGetSnapshotReplyActor.props(
                Arrays.asList("shard1"), "config",
                shardManagerSnapshot, kit.getRef(), "shard-manager", Duration.create(100, TimeUnit.MILLISECONDS)),
                    actorFactory.generateActorId("actor"));

        kit.watch(replyActor);

        Failure failure = kit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", TimeoutException.class, failure.cause().getClass());
        kit.expectTerminated(replyActor);
    }
}
