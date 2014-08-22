/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.testkit.JavaTestKit;

import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

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
            ShardContext shardContext = new ShardContext();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, shardContext);
            final ActorRef shard = getSystem().actorOf(props);

            new Within(duration("10 seconds")) {
                @Override
                protected void run() {
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

                    // 1. Create a TransactionChain
                    shard.tell(new CreateTransactionChain().toSerializable(), getRef());

                    final ActorSelection transactionChain =
                        new ExpectMsg<ActorSelection>(duration("3 seconds"), "CreateTransactionChainReply") {
                            @Override
                            protected ActorSelection match(Object in) {
                                if (in.getClass().equals(CreateTransactionChainReply.SERIALIZABLE_CLASS)) {
                                    ActorPath transactionChainPath =
                                        CreateTransactionChainReply.fromSerializable(getSystem(),in)
                                            .getTransactionChainPath();
                                    return getSystem()
                                        .actorSelection(transactionChainPath);
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertNotNull(transactionChain);

                    System.out.println("Successfully created transaction chain");

                    // 2. Create a Transaction on the TransactionChain
                    transactionChain.tell(new CreateTransaction("txn-1", TransactionProxy.TransactionType.WRITE_ONLY.ordinal() ).toSerializable(), getRef());

                    final ActorSelection transaction =
                        new ExpectMsg<ActorSelection>(duration("3 seconds"), "CreateTransactionReply") {
                            @Override
                            protected ActorSelection match(Object in) {
                                if (CreateTransactionReply.SERIALIZABLE_CLASS.equals(in.getClass())) {
                                    CreateTransactionReply reply = CreateTransactionReply.fromSerializable(in);
                                    return getSystem()
                                        .actorSelection(reply
                                            .getTransactionPath());
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertNotNull(transaction);

                    System.out.println("Successfully created transaction");

                    // 3. Write some data
                    transaction.tell(new WriteData(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME), schemaContext).toSerializable(),
                        getRef());

                    Boolean writeDone = new ExpectMsg<Boolean>(duration("3 seconds"), "WriteDataReply") {
                        @Override
                        protected Boolean match(Object in) {
                            if (in.getClass().equals(WriteDataReply.SERIALIZABLE_CLASS)) {
                                return true;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(writeDone);

                    System.out.println("Successfully wrote data");

                    // 4. Ready the transaction for commit

                    transaction.tell(new ReadyTransaction().toSerializable(), getRef());

                    final ActorSelection cohort =
                        new ExpectMsg<ActorSelection>(duration("3 seconds"), "ReadyTransactionReply") {
                            @Override
                            protected ActorSelection match(Object in) {
                                if (in.getClass().equals(ReadyTransactionReply.SERIALIZABLE_CLASS)) {
                                    ActorPath cohortPath =
                                        ReadyTransactionReply.fromSerializable(getSystem(),in)
                                            .getCohortPath();
                                    return getSystem()
                                        .actorSelection(cohortPath);
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertNotNull(cohort);

                    System.out.println("Successfully readied the transaction");

                    // 5. PreCommit the transaction

                    cohort.tell(new PreCommitTransaction().toSerializable(), getRef());

                    Boolean preCommitDone =
                        new ExpectMsg<Boolean>(duration("3 seconds"), "PreCommitTransactionReply") {
                            @Override
                            protected Boolean match(Object in) {
                                if (in.getClass().equals(PreCommitTransactionReply.SERIALIZABLE_CLASS)) {
                                    return true;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertTrue(preCommitDone);

                    System.out.println("Successfully pre-committed the transaction");

                    // 6. Commit the transaction
                    cohort.tell(new CommitTransaction().toSerializable(), getRef());

                    // FIXME : Add assertions that the commit worked and that the cohort and transaction actors were terminated

                    System.out.println("TODO : Check Successfully committed the transaction");
                }


            };
        }

            private ActorRef watchActor(ActorSelection actor) {
                Future<ActorRef> future = actor
                    .resolveOne(FiniteDuration.apply(100, "milliseconds"));

                try {
                    ActorRef actorRef = Await.result(future,
                        FiniteDuration.apply(100, "milliseconds"));

                    watch(actorRef);

                    return actorRef;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };


    }
}
