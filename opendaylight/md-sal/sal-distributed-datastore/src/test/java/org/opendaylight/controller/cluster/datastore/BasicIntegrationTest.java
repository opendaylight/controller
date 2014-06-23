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

public class BasicIntegrationTest extends AbstractActorTest {

    @Test
    public void integrationTest() {
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
                        new ExpectMsg<ActorSelection>("match hint") {
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

                    transactionChain.tell(new CreateTransaction(), getRef());

                    final ActorSelection transaction =
                        new ExpectMsg<ActorSelection>("match hint") {
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

                    transaction.tell(new WriteData(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME)),
                        getRef());

                    Boolean writeDone = new ExpectMsg<Boolean>("match hint") {
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
                        new ExpectMsg<ActorSelection>("match hint") {
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

                    cohort.tell(new PreCommitTransaction(), getRef());

                    Boolean preCommitDone =
                        new ExpectMsg<Boolean>("match hint") {
                            protected Boolean match(Object in) {
                                if (in instanceof PreCommitTransactionReply) {
                                    return true;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    Assert.assertTrue(preCommitDone);

                    cohort.tell(new CommitTransaction(), getRef());

                    final Boolean commitDone =
                        new ExpectMsg<Boolean>("match hint") {
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
        }};


    }
}
