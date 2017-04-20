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

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class ShardSnapshotActorTest extends AbstractActorTest {
    private static final NormalizedNode<?, ?> DATA = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

    private static void testSerializeSnapshot(final String testName, final ShardDataTreeSnapshot snapshot,
            final boolean withInstallSnapshot) throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef snapshotActor = getSystem().actorOf(ShardSnapshotActor.props(), testName);
                watch(snapshotActor);

                final NormalizedNode<?, ?> expectedRoot = snapshot.getRootNode().get();

                ByteArrayOutputStream installSnapshotStream = withInstallSnapshot ? new ByteArrayOutputStream() : null;
                ShardSnapshotActor.requestSnapshot(snapshotActor, snapshot,
                        Optional.ofNullable(installSnapshotStream), getRef());

                final CaptureSnapshotReply reply = expectMsgClass(duration("3 seconds"), CaptureSnapshotReply.class);
                assertNotNull("getSnapshotState is null", reply.getSnapshotState());
                assertEquals("SnapshotState type", ShardSnapshotState.class, reply.getSnapshotState().getClass());
                assertEquals("Snapshot", snapshot, ((ShardSnapshotState)reply.getSnapshotState()).getSnapshot());

                if (installSnapshotStream != null) {
                    final ShardDataTreeSnapshot deserialized;
                    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
                            installSnapshotStream.toByteArray()))) {
                        deserialized = ShardDataTreeSnapshot.deserialize(in);
                    }

                    assertEquals("Deserialized snapshot type", snapshot.getClass(), deserialized.getClass());

                    final Optional<NormalizedNode<?, ?>> maybeNode = deserialized.getRootNode();
                    assertEquals("isPresent", true, maybeNode.isPresent());
                    assertEquals("Root node", expectedRoot, maybeNode.get());
                }
            }
        };
    }

    @Test
    public void testSerializeBoronSnapshot() throws Exception {
        testSerializeSnapshot("testSerializeBoronSnapshotWithInstallSnapshot",
                new MetadataShardDataTreeSnapshot(DATA), true);
        testSerializeSnapshot("testSerializeBoronSnapshotWithoutInstallSnapshot",
                new MetadataShardDataTreeSnapshot(DATA), false);
    }
}
