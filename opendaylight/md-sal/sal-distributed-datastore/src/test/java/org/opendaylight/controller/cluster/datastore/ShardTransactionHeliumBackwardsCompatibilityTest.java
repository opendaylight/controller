/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.FiniteDuration;

/**
 * Tests backwards compatibility support from Helium-1 to Helium.
 *
 * In Helium-1, the 3-phase commit support was moved from the ThreePhaseCommitCohort actor to the
 * Shard. As a consequence, a new transactionId field was added to the CanCommitTransaction,
 * CommitTransaction and AbortTransaction messages. With a base Helium version node, these messages
 * would be sans transactionId so this test verifies the Shard handles that properly.
 *
 * @author Thomas Pantelis
 */
public class ShardTransactionHeliumBackwardsCompatibilityTest extends AbstractActorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testTransactionCommit() throws Exception {
        new ShardTestKit(getSystem()) {{
            SchemaContext schemaContext = TestModel.createTestContext();
            Props shardProps = Shard.props(ShardIdentifier.builder().memberName("member-1").
                    shardName("inventory").type("config").build(),
                    Collections.<ShardIdentifier,String>emptyMap(),
                    DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).build(),
                    schemaContext).withDispatcher(Dispatchers.DefaultDispatcherId());

            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(), shardProps,
                    "testTransactionCommit");

            waitUntilLeader(shard);

            // Send CreateTransaction message with no messages version

            String transactionID = "txn-1";
            shard.tell(ShardTransactionMessages.CreateTransaction.newBuilder()
                    .setTransactionId(transactionID)
                    .setTransactionType(TransactionProxy.TransactionType.WRITE_ONLY.ordinal())
                    .setTransactionChainId("").build(), getRef());

            final FiniteDuration duration = duration("5 seconds");

            CreateTransactionReply reply = expectMsgClass(duration, CreateTransactionReply.class);

            ActorSelection txActor = getSystem().actorSelection(reply.getTransactionActorPath());

            // Write data to the Tx

            txActor.tell(new WriteData(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME)).toSerializable(
                            DataStoreVersions.BASE_HELIUM_VERSION), getRef());

            expectMsgClass(duration, ShardTransactionMessages.WriteDataReply.class);

            // Ready the Tx

            txActor.tell(new ReadyTransaction().toSerializable(), getRef());

            ReadyTransactionReply readyReply = ReadyTransactionReply.fromSerializable(expectMsgClass(
                    duration, ReadyTransactionReply.SERIALIZABLE_CLASS));

            ActorSelection cohortActor = getSystem().actorSelection(readyReply.getCohortPath());

            // Send the CanCommitTransaction message with no transactionId.

            cohortActor.tell(ThreePhaseCommitCohortMessages.CanCommitTransaction.newBuilder().build(),
                    getRef());

            expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS);

            // Send the PreCommitTransaction message with no transactionId.

            cohortActor.tell(ThreePhaseCommitCohortMessages.PreCommitTransaction.newBuilder().build(),
                    getRef());

            expectMsgClass(duration, PreCommitTransactionReply.SERIALIZABLE_CLASS);

            // Send the CommitTransaction message with no transactionId.

            cohortActor.tell(ThreePhaseCommitCohortMessages.CommitTransaction.newBuilder().build(),
                    getRef());

            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            NormalizedNode<?, ?> node = ShardTest.readStore(shard, TestModel.TEST_PATH);
            Assert.assertNotNull("Data not found in store", node);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testTransactionAbort() throws Exception {
        new ShardTestKit(getSystem()) {{
            SchemaContext schemaContext = TestModel.createTestContext();
            Props shardProps = Shard.props(ShardIdentifier.builder().memberName("member-1").
                    shardName("inventory").type("config").build(),
                    Collections.<ShardIdentifier,String>emptyMap(),
                    DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).build(),
                    schemaContext).withDispatcher(Dispatchers.DefaultDispatcherId());

            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(), shardProps,
                    "testTransactionAbort");

            waitUntilLeader(shard);

            // Send CreateTransaction message with no messages version

            String transactionID = "txn-1";
            shard.tell(ShardTransactionMessages.CreateTransaction.newBuilder()
                    .setTransactionId(transactionID)
                    .setTransactionType(TransactionProxy.TransactionType.WRITE_ONLY.ordinal())
                    .setTransactionChainId("").build(), getRef());

            final FiniteDuration duration = duration("5 seconds");

            CreateTransactionReply reply = expectMsgClass(duration, CreateTransactionReply.class);

            ActorSelection txActor = getSystem().actorSelection(reply.getTransactionActorPath());

            // Write data to the Tx

            txActor.tell(new WriteData(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME)), getRef());

            expectMsgClass(duration, WriteDataReply.class);

            // Ready the Tx

            txActor.tell(new ReadyTransaction().toSerializable(), getRef());

            ReadyTransactionReply readyReply = ReadyTransactionReply.fromSerializable(expectMsgClass(
                    duration, ReadyTransactionReply.SERIALIZABLE_CLASS));

            ActorSelection cohortActor = getSystem().actorSelection(readyReply.getCohortPath());

            // Send the CanCommitTransaction message with no transactionId.

            cohortActor.tell(ThreePhaseCommitCohortMessages.CanCommitTransaction.newBuilder().build(),
                    getRef());

            expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS);

            // Send the AbortTransaction message with no transactionId.

            cohortActor.tell(ThreePhaseCommitCohortMessages.AbortTransaction.newBuilder().build(),
                    getRef());

            expectMsgClass(duration, AbortTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }
}
