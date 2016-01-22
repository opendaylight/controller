/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardTestKit;
import org.opendaylight.controller.cluster.datastore.TransactionType;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Shard unit tests for backwards compatibility with pre-Boron versions.
 *
 * @author Thomas Pantelis
 */
public class PreBoronShardTest extends AbstractShardTest {

    @Test
    public void testCreateTransaction(){
        new ShardTestKit(getSystem()) {{
            final ActorRef shard = actorFactory.createActor(newShardProps(), "testCreateTransaction");

            waitUntilLeader(shard);

            shard.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shard.tell(new CreateTransaction("txn-1", TransactionType.READ_ONLY.ordinal(), null,
                    DataStoreVersions.LITHIUM_VERSION).toSerializable(), getRef());

            ShardTransactionMessages.CreateTransactionReply reply =
                    expectMsgClass(ShardTransactionMessages.CreateTransactionReply.class);

            final String path = CreateTransactionReply.fromSerializable(reply).getTransactionPath().toString();
            assertTrue("Unexpected transaction path " + path,
                    path.contains("akka://test/user/testCreateTransaction/shard-txn-1"));
        }};
    }

    @Test
    public void testBatchedModificationsWithCommitOnReady() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = actorFactory.createTestActor(
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testBatchedModificationsWithCommitOnReady");

            waitUntilLeader(shard);

            final String transactionID = "tx";

            BatchedModifications batched = new BatchedModifications(transactionID,
                    DataStoreVersions.LITHIUM_VERSION, "");
            batched.addModification(new WriteModification(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME)));
            batched.setReady(true);
            batched.setDoCommitOnReady(true);
            batched.setTotalMessagesSent(1);

            shard.tell(batched, getRef());
            expectMsgClass(ThreePhaseCommitCohortMessages.CommitTransactionReply.class);
        }};
    }

    @Test
    public void testImmediateCommitWithForwardedReadyTransaction() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = actorFactory.createTestActor(
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testImmediateCommitWithForwardedReadyTransaction");

            waitUntilLeader(shard);

            final String transactionID = "tx";

            shard.tell(prepareForwardedReadyTransaction(mockShardDataTreeCohort(), transactionID,
                    DataStoreVersions.LITHIUM_VERSION, true), getRef());

            expectMsgClass(ThreePhaseCommitCohortMessages.CommitTransactionReply.class);
        }};
    }

    @Test
    public void testThreePhaseCommitOnForwardedReadyTransaction() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = actorFactory.createTestActor(
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testThreePhaseCommitOnForwardedReadyTransaction");

            waitUntilLeader(shard);

            final String transactionID = "tx";

            shard.tell(prepareForwardedReadyTransaction(mockShardDataTreeCohort(), transactionID,
                    DataStoreVersions.LITHIUM_VERSION, false), getRef());
            expectMsgClass(ReadyTransactionReply.class);

            shard.tell(new CanCommitTransaction(transactionID, DataStoreVersions.LITHIUM_VERSION).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(ThreePhaseCommitCohortMessages.CanCommitTransactionReply.class));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            shard.tell(new CommitTransaction(transactionID, DataStoreVersions.LITHIUM_VERSION).toSerializable(), getRef());
            expectMsgClass(ThreePhaseCommitCohortMessages.CommitTransactionReply.class);
        }};
    }
}
