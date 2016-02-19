/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
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
    private static final SchemaContext testSchemaContext =
            TestModel.createTestContext();
    private static final TransactionType RO = TransactionType.READ_ONLY;
    private static final TransactionType RW = TransactionType.READ_WRITE;
    private static final TransactionType WO = TransactionType.WRITE_ONLY;

    private static final ShardDataTree store = new ShardDataTree(testSchemaContext, TreeType.OPERATIONAL);

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("operational").build();

    private final DatastoreContext datastoreContext = DatastoreContext.newBuilder().build();

    private final ShardStats shardStats = new ShardStats(SHARD_IDENTIFIER.toString(), "DataStore");

    private ActorRef createShard(){
        ActorRef shard = getSystem().actorOf(Shard.builder().id(SHARD_IDENTIFIER).datastoreContext(datastoreContext).
                schemaContext(TestModel.createTestContext()).props());
        ShardTestKit.waitUntilLeader(shard);
        return shard;
    }

    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadOnlyTransactionClosed() throws Throwable {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(RO, store.newReadOnlyTransaction("test-txn", null), shard,
                datastoreContext, shardStats, "txn");

        final TestActorRef<ShardTransaction> subject = TestActorRef.create(getSystem(), props,
                "testNegativeReadWithReadOnlyTransactionClosed");

        Future<Object> future = akka.pattern.Patterns.ask(subject,
                new ReadData(YangInstanceIdentifier.EMPTY, DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().abort();

        future = akka.pattern.Patterns.ask(subject, new ReadData(YangInstanceIdentifier.EMPTY,
                DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }


    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadWriteTransactionClosed() throws Throwable {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(RW, store.newReadWriteTransaction("test-txn", null), shard,
                datastoreContext, shardStats, "txn");

        final TestActorRef<ShardTransaction> subject = TestActorRef.create(getSystem(), props,
                "testNegativeReadWithReadWriteTransactionClosed");

        Future<Object> future = akka.pattern.Patterns.ask(subject,
                new ReadData(YangInstanceIdentifier.EMPTY, DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().abort();

        future = akka.pattern.Patterns.ask(subject, new ReadData(YangInstanceIdentifier.EMPTY,
                DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }

    @Test(expected = ReadFailedException.class)
    public void testNegativeExistsWithReadWriteTransactionClosed() throws Throwable {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(RW, store.newReadWriteTransaction("test-txn", null), shard,
                datastoreContext, shardStats, "txn");

        final TestActorRef<ShardTransaction> subject = TestActorRef.create(getSystem(), props,
                "testNegativeExistsWithReadWriteTransactionClosed");

        Future<Object> future = akka.pattern.Patterns.ask(subject,
                new DataExists(YangInstanceIdentifier.EMPTY, DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().abort();

        future = akka.pattern.Patterns.ask(subject,
                new DataExists(YangInstanceIdentifier.EMPTY, DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, Duration.create(3, TimeUnit.SECONDS));
    }
}
