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
import akka.testkit.JavaTestKit;
import junit.framework.Assert;
import org.junit.Test;
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
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

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
            final Props props = Shard.props("config");
            final ActorRef shard = getSystem().actorOf(props);

            new Within(duration("5 seconds")) {
                protected void run() {

                    shard.tell(
                        new UpdateSchemaContext(TestModel.createTestContext()),
                        getRef());

                    shard.tell(new CreateTransactionChain().toSerializable(), getRef());

                    final ActorSelection transactionChain =
                        new ExpectMsg<ActorSelection>("CreateTransactionChainReply") {
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

                    Assert.assertNotNull(transactionChain);

                    transactionChain.tell(new CreateTransaction("txn-1").toSerializable(), getRef());

                    final ActorSelection transaction =
                        new ExpectMsg<ActorSelection>("CreateTransactionReply") {
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

                    Assert.assertNotNull(transaction);

                    // Add a watch on the transaction actor so that we are notified when it dies
                    final ActorRef transactionActorRef = watchActor(transaction);

                    transaction.tell(new WriteData(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME), TestModel.createTestContext()).toSerializable(),
                        getRef());

                    Boolean writeDone = new ExpectMsg<Boolean>("WriteDataReply") {
                        protected Boolean match(Object in) {
                            if (in.getClass().equals(WriteDataReply.SERIALIZABLE_CLASS)) {
                                return true;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    Assert.assertTrue(writeDone);

                    transaction.tell(new ReadyTransaction().toSerializable(), getRef());

                    final ActorSelection cohort =
                        new ExpectMsg<ActorSelection>("ReadyTransactionReply") {
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

                    Assert.assertNotNull(cohort);

                    // Add a watch on the transaction actor so that we are notified when it dies
                    final ActorRef cohorActorRef = watchActor(cohort);

                    cohort.tell(new PreCommitTransaction().toSerializable(), getRef());

                    Boolean preCommitDone =
                        new ExpectMsg<Boolean>("PreCommitTransactionReply") {
                            protected Boolean match(Object in) {
                                if (in.getClass().equals(PreCommitTransactionReply.SERIALIZABLE_CLASS)) {
                                    return true;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertTrue(preCommitDone);

                    cohort.tell(new CommitTransaction().toSerializable(), getRef());

                    // FIXME : Add assertions that the commit worked and that the cohort and transaction actors were terminated

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
