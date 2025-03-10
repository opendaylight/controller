/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ShardSnapshotActorTest extends AbstractActorTest {
    private static final InputOutputStreamFactory STREAM_FACTORY = InputOutputStreamFactory.simple();

    private static void testSerializeSnapshot(final String testName, final ShardDataTreeSnapshot snapshot,
            final boolean withInstallSnapshot) throws Exception {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef snapshotActor = getSystem().actorOf(ShardSnapshotActor.props(STREAM_FACTORY), testName);
        kit.watch(snapshotActor);

        final NormalizedNode expectedRoot = snapshot.getRootNode().orElseThrow();

        ByteArrayOutputStream installSnapshotStream = withInstallSnapshot ? new ByteArrayOutputStream() : null;
        ShardSnapshotActor.requestSnapshot(snapshotActor, snapshot, installSnapshotStream, kit.getRef());

        final CaptureSnapshotReply reply = kit.expectMsgClass(Duration.ofSeconds(3), CaptureSnapshotReply.class);
        assertNotNull("getSnapshotState is null", reply.snapshotState());
        assertEquals("SnapshotState type", ShardSnapshotState.class, reply.snapshotState().getClass());
        assertEquals("Snapshot", snapshot, ((ShardSnapshotState)reply.snapshotState()).getSnapshot());

        if (installSnapshotStream != null) {
            final ShardDataTreeSnapshot deserialized;
            try (ObjectInputStream in = new ObjectInputStream(STREAM_FACTORY.createInputStream(
                    ByteSource.wrap(installSnapshotStream.toByteArray())))) {
                deserialized = ShardDataTreeSnapshot.deserialize(in).getSnapshot();
            }

            assertEquals("Deserialized snapshot type", snapshot.getClass(), deserialized.getClass());
            assertEquals("Root node", Optional.of(expectedRoot), deserialized.getRootNode());
        }
    }

    @Test
    public void testSerializeBoronSnapshot() throws Exception {
        testSerializeSnapshot("testSerializeBoronSnapshotWithInstallSnapshot",
                new MetadataShardDataTreeSnapshot(TestModel.EMPTY_TEST), true);
        testSerializeSnapshot("testSerializeBoronSnapshotWithoutInstallSnapshot",
                new MetadataShardDataTreeSnapshot(TestModel.EMPTY_TEST), false);
    }
}
