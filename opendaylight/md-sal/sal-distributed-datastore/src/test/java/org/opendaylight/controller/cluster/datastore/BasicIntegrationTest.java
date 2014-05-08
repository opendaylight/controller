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
import akka.actor.Terminated;
import akka.testkit.JavaTestKit;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
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
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class BasicIntegrationTest extends AbstractActorTest {

    @Test
    public void integrationTest() throws Exception{
        // This test will
        // - create a Shard
        // - initiate a transaction
        // - write something
        // - read the transaction for commit
        // - commit the transaction


        new JavaTestKit(getSystem()) {{
            final Props props = Shard.props("config");
            final ActorRef shard = getSystem().actorOf(props);

            new Within(duration("5 seconds")) {
                protected void run() {

                    shard.tell(
                        new UpdateSchemaContext(TestModel.createTestContext()),
                        getRef());

                    shard.tell(new CreateTransactionChain(), getRef());

                    final ActorSelection transactionChain =
                        new ExpectMsg<ActorSelection>("CreateTransactionChainReply") {
                            protected ActorSelection match(Object in) {
                                if (in instanceof CreateTransactionChainReply) {
                                    ActorPath transactionChainPath =
                                        ((CreateTransactionChainReply) in)
                                            .getTransactionChainPath();
                                    return getSystem()
                                        .actorSelection(transactionChainPath);
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertNotNull(transactionChain);

                    transactionChain.tell(new CreateTransaction("txn-1"), getRef());

                    final ActorSelection transaction =
                        new ExpectMsg<ActorSelection>("CreateTransactionReply") {
                            protected ActorSelection match(Object in) {
                                if (in instanceof CreateTransactionReply) {
                                    ActorPath transactionPath =
                                        ((CreateTransactionReply) in)
                                            .getTransactionPath();
                                    return getSystem()
                                        .actorSelection(transactionPath);
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertNotNull(transaction);

                    // Add a watch on the transaction actor so that we are notified when it dies
                    final ActorRef transactionActorRef = watchActor(transaction);

                    transaction.tell(new WriteData(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME)),
                        getRef());

                    Boolean writeDone = new ExpectMsg<Boolean>("WriteDataReply") {
                        protected Boolean match(Object in) {
                            if (in instanceof WriteDataReply) {
                                return true;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    Assert.assertTrue(writeDone);

                    transaction.tell(new ReadyTransaction(), getRef());

                    final ActorSelection cohort =
                        new ExpectMsg<ActorSelection>("ReadyTransactionReply") {
                            protected ActorSelection match(Object in) {
                                if (in instanceof ReadyTransactionReply) {
                                    ActorPath cohortPath =
                                        ((ReadyTransactionReply) in)
                                            .getCohortPath();
                                    return getSystem()
                                        .actorSelection(cohortPath);
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertNotNull(cohort);

                    // Add a watch on the transaction actor so that we are notified when it dies
                    final ActorRef cohorActorRef = watchActor(cohort);

                    cohort.tell(new PreCommitTransaction(), getRef());

                    Boolean preCommitDone =
                        new ExpectMsg<Boolean>("PreCommitTransactionReply") {
                            protected Boolean match(Object in) {
                                if (in instanceof PreCommitTransactionReply) {
                                    return true;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertTrue(preCommitDone);

                    // FIXME : When we commit on the cohort it "kills" the Transaction.
                    // This in turn kills the child of Transaction as well.
                    // The order in which we receive the terminated event for both
                    // these actors is not fixed which may cause this test to fail
                    cohort.tell(new CommitTransaction(), getRef());

                    final Boolean terminatedCohort =
                        new ExpectMsg<Boolean>("Terminated Cohort") {
                            protected Boolean match(Object in) {
                                if (in instanceof Terminated) {
                                    return cohorActorRef.equals(((Terminated) in).actor());
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertTrue(terminatedCohort);


                    final Boolean terminatedTransaction =
                        new ExpectMsg<Boolean>("Terminated Transaction") {
                            protected Boolean match(Object in) {
                                if (in instanceof Terminated) {
                                    return transactionActorRef.equals(((Terminated) in).actor());
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertTrue(terminatedTransaction);

                    final Boolean commitDone =
                        new ExpectMsg<Boolean>("CommitTransactionReply") {
                            protected Boolean match(Object in) {
                                if (in instanceof CommitTransactionReply) {
                                    return true;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertTrue(commitDone);

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
