/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.google.common.io.ByteSource;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.io.InputOutputStreamFactory;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;

public class ShardSnapshotActorTest extends AbstractActorTest {
    private static final InputOutputStreamFactory STREAM_FACTORY = InputOutputStreamFactory.simple();

    @Test
    public void testSerializeSnapshot() throws Exception {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef snapshotActor = getSystem().actorOf(ShardSnapshotActor.props(STREAM_FACTORY),
            "testSerializeBoronSnapshotWithInstallSnapshot");
        kit.watch(snapshotActor);

        final var snapshot = new ShardSnapshotState(new MetadataShardDataTreeSnapshot(TestModel.EMPTY_TEST));

        final var installSnapshotStream = new ByteArrayOutputStream();
        ShardSnapshotActor.requestSnapshot(snapshotActor, snapshot, installSnapshotStream, kit.getRef());

        final var reply = kit.expectMsgClass(Duration.ofSeconds(3), CaptureSnapshotReply.class);
        assertSame(snapshot, reply.snapshotState());

        final ShardDataTreeSnapshot deserialized;
        try (var in = new ObjectInputStream(STREAM_FACTORY.createInputStream(
            ByteSource.wrap(installSnapshotStream.toByteArray())))) {
            deserialized = ShardDataTreeSnapshot.deserialize(in).getSnapshot();
        }

        assertEquals("Root node", Optional.of(TestModel.EMPTY_TEST), deserialized.getRootNode());
    }
}
