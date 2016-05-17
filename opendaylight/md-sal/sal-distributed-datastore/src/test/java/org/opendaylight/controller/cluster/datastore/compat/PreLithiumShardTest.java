/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.TestActorRef;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * Unit tests for backwards compatibility with pre-Lithium versions.
 *
 * @author Thomas Pantelis
 */
public class PreLithiumShardTest extends AbstractShardTest {
    @Test
    public void testApplyHelium2VersionSnapshot() throws Exception {
        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(),
                "testApplyHelium2VersionSnapshot");

        NormalizedNodeToNodeCodec codec = new NormalizedNodeToNodeCodec(SCHEMA_CONTEXT);

        DataTree store = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        store.setSchemaContext(SCHEMA_CONTEXT);

        writeToStore(store, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        YangInstanceIdentifier root = YangInstanceIdentifier.builder().build();
        NormalizedNode<?,?> expected = readStore(store, root);

        NormalizedNodeMessages.Container encode = codec.encode(expected);

        Snapshot snapshot = Snapshot.create(encode.getNormalizedNode().toByteString().toByteArray(),
                Collections.<ReplicatedLogEntry>emptyList(), 1, 2, 3, 4);

        shard.underlyingActor().getRaftActorSnapshotCohort().applySnapshot(snapshot.getState());

        NormalizedNode<?,?> actual = readStore(shard, root);

        assertEquals("Root node", expected, actual);

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }
}
