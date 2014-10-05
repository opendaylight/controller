/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.FiniteDuration;
import java.util.Collections;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertEquals;

public class BasicIntegrationTest extends AbstractActorTest {

    @Test
    public void integrationTest() throws Exception{
        // System.setProperty("shard.persistent", "true");
        // This test will
        // - create a Shard
        // - initiate a transaction
        // - write something
        // - read the transaction for commit
        // - commit the transaction


        new JavaTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            final SchemaContext schemaContext = TestModel.createTestContext();
            DatastoreContext datastoreContext = new DatastoreContext();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, datastoreContext,
                    TestModel.createTestContext());
            final ActorRef shard = getSystem().actorOf(props);

            shard.tell(new UpdateSchemaContext(schemaContext), getRef());

            // Wait for a specific log message to show up
            final boolean result =
                    new JavaTestKit.EventFilter<Boolean>(Logging.Info.class
                            ) {
                @Override
                protected Boolean run() {
                    return true;
                }
            }.from(shard.path().toString())
            .message("Switching from state Candidate to Leader")
            .occurrences(1).exec();

            assertEquals(true, result);

            FiniteDuration duration = duration("5 seconds");

            // Create a transaction on the shard
            String transactionId = "txn-1";
            shard.tell(new CreateTransaction(transactionId, TransactionProxy.TransactionType.WRITE_ONLY.ordinal() ).
                    toSerializable(), getRef());

            CreateTransactionReply createReply = CreateTransactionReply.fromSerializable(
                    expectMsgClass(duration, CreateTransactionReply.SERIALIZABLE_CLASS));

            ActorSelection transaction = getSystem().actorSelection(createReply.getTransactionPath());

            System.out.println("Successfully created transaction");

            // 3. Write some data
            transaction.tell(new WriteData(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), schemaContext).toSerializable(),
                    getRef());

            expectMsgClass(duration, WriteDataReply.SERIALIZABLE_CLASS);

            System.out.println("Successfully wrote data");

            // 4. Ready the transaction for commit

            transaction.tell(new ReadyTransaction().toSerializable(), getRef());

            ReadyTransactionReply readyReply = ReadyTransactionReply.fromSerializable(getSystem(),
                    expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS));
            ActorSelection cohort = getSystem().actorSelection(readyReply.getCohortPath());

            System.out.println("Successfully readied the transaction");

            // 5. CanCommit the transaction

            cohort.tell(new CanCommitTransaction(transactionId).toSerializable(), getRef());

            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            System.out.println("Successfully can-committed the transaction");

            // 6. Commit the transaction
            cohort.tell(new CommitTransaction(transactionId).toSerializable(), getRef());

            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            System.out.println("TODO : Check Successfully committed the transaction");
        }};
    }
}
