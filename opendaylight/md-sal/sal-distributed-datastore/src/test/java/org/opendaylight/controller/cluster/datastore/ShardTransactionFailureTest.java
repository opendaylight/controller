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
import akka.pattern.AskTimeoutException;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Covers negative test cases
 *
 * @author Basheeruddin Ahmed <syedbahm@cisco.com>
 */
public class ShardTransactionFailureTest extends AbstractActorTest {
    private static final InMemoryDOMDataStore store =
        new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());

    private static final SchemaContext testSchemaContext =
        TestModel.createTestContext();

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("operational").build();

    private final DatastoreContext datastoreContext = DatastoreContext.newBuilder().build();

    private final ShardStats shardStats = new ShardStats(SHARD_IDENTIFIER.toString(), "DataStore");

    @BeforeClass
    public static void staticSetup() {
        store.onGlobalContextUpdated(testSchemaContext);
    }

    private ActorRef createShard(){
        return getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.<ShardIdentifier, String>emptyMap(), datastoreContext,
                TestModel.createTestContext()));
    }

    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadOnlyTransactionClosed()
        throws Throwable {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);

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
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().close();

        future = akka.pattern.Patterns.ask(subject, readData, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }


    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadWriteTransactionClosed()
        throws Throwable {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeReadWithReadWriteTransactionClosed");

        ShardTransactionMessages.ReadData readData =
            ShardTransactionMessages.ReadData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()
                ).build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readData, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().close();

        future = akka.pattern.Patterns.ask(subject, readData, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }

    @Test(expected = ReadFailedException.class)
    public void testNegativeExistsWithReadWriteTransactionClosed()
        throws Throwable {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeExistsWithReadWriteTransactionClosed");

        ShardTransactionMessages.DataExists dataExists =
            ShardTransactionMessages.DataExists.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()
                ).build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, dataExists, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().close();

        future = akka.pattern.Patterns.ask(subject, dataExists, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }

    @Test(expected = AskTimeoutException.class)
    public void testNegativeWriteWithTransactionReady() throws Exception {


        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newWriteOnlyTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeWriteWithTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        ShardTransactionMessages.WriteData writeData =
            ShardTransactionMessages.WriteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).setNormalizedNode(
                buildNormalizedNode()

            ).build();

        future = akka.pattern.Patterns.ask(subject, writeData, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }

    @Test(expected = AskTimeoutException.class)
    public void testNegativeReadWriteWithTransactionReady() throws Exception {


        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeReadWriteWithTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        ShardTransactionMessages.WriteData writeData =
            ShardTransactionMessages.WriteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()
                )
                .setNormalizedNode(buildNormalizedNode())
                .build();

        future = akka.pattern.Patterns.ask(subject, writeData, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }

    private NormalizedNodeMessages.Node buildNormalizedNode() {
        return NormalizedNodeSerializer
            .serialize(Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).build());
    }

    @Test(expected = AskTimeoutException.class)
    public void testNegativeMergeTransactionReady() throws Exception {


        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props, "testNegativeMergeTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        ShardTransactionMessages.MergeData mergeData =
            ShardTransactionMessages.MergeData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).setNormalizedNode(
                buildNormalizedNode()

            ).build();

        future = akka.pattern.Patterns.ask(subject, mergeData, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }


    @Test(expected = AskTimeoutException.class)
    public void testNegativeDeleteDataWhenTransactionReady() throws Exception {


        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);

        final TestActorRef<ShardTransaction> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeDeleteDataWhenTransactionReady");

        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        Future<Object> future =
            akka.pattern.Patterns.ask(subject, readyTransaction, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        ShardTransactionMessages.DeleteData deleteData =
            ShardTransactionMessages.DeleteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).build();

        future = akka.pattern.Patterns.ask(subject, deleteData, 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }
}
