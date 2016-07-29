/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.BoronShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.LegacyShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class ShardSnapshotActorTest extends AbstractActorTest {
    private static final NormalizedNode<?, ?> DATA = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

    private static void testSerializeSnapshot(final String testName, final AbstractShardDataTreeSnapshot snapshot)
            throws Exception {
        new JavaTestKit(getSystem()) {{

            final ActorRef snapshotActor = getSystem().actorOf(ShardSnapshotActor.props(), testName);
            watch(snapshotActor);

            final NormalizedNode<?, ?> expectedRoot = snapshot.getRootNode().get();

            ShardSnapshotActor.requestSnapshot(snapshotActor, snapshot, getRef());

            final CaptureSnapshotReply reply = expectMsgClass(duration("3 seconds"), CaptureSnapshotReply.class);
            assertNotNull("getSnapshot is null", reply.getSnapshot());

            final AbstractShardDataTreeSnapshot actual = AbstractShardDataTreeSnapshot.deserialize(reply.getSnapshot());
            assertNotNull(actual);
            assertEquals(snapshot.getClass(), actual.getClass());

            final Optional<NormalizedNode<?, ?>> maybeNode = actual.getRootNode();
            assertTrue(maybeNode.isPresent());

            assertEquals("Root node", expectedRoot, maybeNode.get());
        }};
    }

    @Test
    public void testSerializeBoronSnapshot() throws Exception {
        testSerializeSnapshot("testSerializeBoronSnapshot", new BoronShardDataTreeSnapshot(DATA));
    }

    @Deprecated
    @Test
    public void testSerializeLegacySnapshot() throws Exception {
        testSerializeSnapshot("testSerializeLegacySnapshot", new LegacyShardDataTreeSnapshot(DATA));
    }
}
