/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

/**
 * Covers negative test cases
 *
 * @author Basheeruddin Ahmed <syedbahm@cisco.com>
 */
public class ShardTransactionFailureTest extends AbstractActorTest {
    private static ListeningExecutorService storeExecutor =
        MoreExecutors.listeningDecorator(MoreExecutors.sameThreadExecutor());

    private static final InMemoryDOMDataStore store =
        new InMemoryDOMDataStore("OPER", storeExecutor,
            MoreExecutors.sameThreadExecutor());

    private static final SchemaContext testSchemaContext =
        TestModel.createTestContext();

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("operational").build();

    static {
        store.onGlobalContextUpdated(testSchemaContext);
    }


    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadOnlyTransactionClosed()
        throws Throwable {

        final ActorRef shard =
            getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP, null));
        final Props props =
            ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                TestModel.createTestContext());

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeReadWithReadOnlyTransactionClosed");

        ShardTransactionMessages.ReadData readData =
            ShardTransactionMessages.ReadData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()
                ).build();
        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readData, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());

        ((ShardReadTransaction) subject.underlyingActor())
            .forUnitTestOnlyExplicitTransactionClose();

        future = akka.pattern.Patterns.ask(subject, readData, 3000);
        Await.result(future, Duration.Zero());


    }


    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadWriteOnlyTransactionClosed()
        throws Throwable {

        final ActorRef shard =
            getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP,null));
        final Props props =
            ShardTransaction.props(store.newReadWriteTransaction(), shard,
                TestModel.createTestContext());

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeReadWithReadWriteOnlyTransactionClosed");

        ShardTransactionMessages.ReadData readData =
            ShardTransactionMessages.ReadData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()
                ).build();
        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readData, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());

        ((ShardReadWriteTransaction) subject.underlyingActor())
            .forUnitTestOnlyExplicitTransactionClose();

        future = akka.pattern.Patterns.ask(subject, readData, 3000);
        Await.result(future, Duration.Zero());


    }

    @Test(expected = ReadFailedException.class)
    public void testNegativeExistsWithReadWriteOnlyTransactionClosed()
        throws Throwable {

        final ActorRef shard =
            getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP,null));
        final Props props =
            ShardTransaction.props(store.newReadWriteTransaction(), shard,
                TestModel.createTestContext());

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeExistsWithReadWriteOnlyTransactionClosed");

        ShardTransactionMessages.DataExists dataExists =
            ShardTransactionMessages.DataExists.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()
                ).build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, dataExists, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());

        ((ShardReadWriteTransaction) subject.underlyingActor())
            .forUnitTestOnlyExplicitTransactionClose();

        future = akka.pattern.Patterns.ask(subject, dataExists, 3000);
        Await.result(future, Duration.Zero());


    }

    @Test(expected = IllegalStateException.class)
    public void testNegativeWriteWithTransactionReady() throws Exception {


        final ActorRef shard =
            getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP,null));
        final Props props =
            ShardTransaction.props(store.newWriteOnlyTransaction(), shard,
                TestModel.createTestContext());

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeWriteWithTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());

        ShardTransactionMessages.WriteData writeData =
            ShardTransactionMessages.WriteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).setNormalizedNode(
                NormalizedNodeMessages.Node.newBuilder().build()

            ).build();

        future = akka.pattern.Patterns.ask(subject, writeData, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());


    }


    @Test(expected = IllegalStateException.class)
    public void testNegativeReadWriteWithTransactionReady() throws Exception {


        final ActorRef shard =
            getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP,null));
        final Props props =
            ShardTransaction.props(store.newReadWriteTransaction(), shard,
                TestModel.createTestContext());

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeReadWriteWithTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());

        ShardTransactionMessages.WriteData writeData =
            ShardTransactionMessages.WriteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).setNormalizedNode(
                NormalizedNodeMessages.Node.newBuilder().build()

            ).build();

        future = akka.pattern.Patterns.ask(subject, writeData, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());


    }

    @Test(expected = IllegalStateException.class)
    public void testNegativeMergeTransactionReady() throws Exception {


        final ActorRef shard =
            getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP,null));
        final Props props =
            ShardTransaction.props(store.newReadWriteTransaction(), shard,
                TestModel.createTestContext());

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props, "testNegativeMergeTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());

        ShardTransactionMessages.MergeData mergeData =
            ShardTransactionMessages.MergeData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).setNormalizedNode(
                NormalizedNodeMessages.Node.newBuilder().build()

            ).build();

        future = akka.pattern.Patterns.ask(subject, mergeData, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());


    }


    @Test(expected = IllegalStateException.class)
    public void testNegativeDeleteDataWhenTransactionReady() throws Exception {


        final ActorRef shard =
            getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP,null));
        final Props props =
            ShardTransaction.props(store.newReadWriteTransaction(), shard,
                TestModel.createTestContext());

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeDeleteDataWhenTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());

        ShardTransactionMessages.DeleteData deleteData =
            ShardTransactionMessages.DeleteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).build();

        future = akka.pattern.Patterns.ask(subject, deleteData, 3000);
        assertTrue(future.isCompleted());
        Await.result(future, Duration.Zero());


    }
}
