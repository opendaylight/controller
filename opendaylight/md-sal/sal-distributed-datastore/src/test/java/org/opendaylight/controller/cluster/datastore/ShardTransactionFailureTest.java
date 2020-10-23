/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Covers negative test cases.
 *
 * @author Basheeruddin Ahmed
 */
public class ShardTransactionFailureTest extends AbstractActorTest {
    private static final EffectiveModelContext TEST_SCHEMA_CONTEXT = TestModel.createTestContext();
    private static final TransactionType RO = TransactionType.READ_ONLY;
    private static final TransactionType RW = TransactionType.READ_WRITE;

    private static final Shard MOCK_SHARD = mock(Shard.class);

    private static final ShardDataTree STORE = new ShardDataTree(MOCK_SHARD, TEST_SCHEMA_CONTEXT, TreeType.OPERATIONAL);

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.create("inventory", MemberName.forName("member-1"), "operational");

    private final DatastoreContext datastoreContext = DatastoreContext.newBuilder().build();

    private final ShardStats shardStats = new ShardStats(SHARD_IDENTIFIER.toString(), "DataStore", null);

    private ActorRef createShard() {
        ActorRef shard = getSystem().actorOf(Shard.builder().id(SHARD_IDENTIFIER).datastoreContext(datastoreContext)
                .schemaContextProvider(() -> TEST_SCHEMA_CONTEXT).props());
        ShardTestKit.waitUntilLeader(shard);
        return shard;
    }

    @Before
    public void setup() {
        ShardStats stats = mock(ShardStats.class);
        when(MOCK_SHARD.getShardMBean()).thenReturn(stats);
    }

    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadOnlyTransactionClosed() throws Exception {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(RO, STORE.newReadOnlyTransaction(nextTransactionId()), shard,
                datastoreContext, shardStats);

        final TestActorRef<ShardTransaction> subject = TestActorRef.create(getSystem(), props,
                "testNegativeReadWithReadOnlyTransactionClosed");

        Future<Object> future = akka.pattern.Patterns.ask(subject,
                new ReadData(YangInstanceIdentifier.empty(), DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, FiniteDuration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().abortFromTransactionActor();

        future = akka.pattern.Patterns.ask(subject, new ReadData(YangInstanceIdentifier.empty(),
                DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, FiniteDuration.create(3, TimeUnit.SECONDS));
    }


    @Test(expected = ReadFailedException.class)
    public void testNegativeReadWithReadWriteTransactionClosed() throws Exception {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(RW, STORE.newReadWriteTransaction(nextTransactionId()), shard,
                datastoreContext, shardStats);

        final TestActorRef<ShardTransaction> subject = TestActorRef.create(getSystem(), props,
                "testNegativeReadWithReadWriteTransactionClosed");

        Future<Object> future = akka.pattern.Patterns.ask(subject,
                new ReadData(YangInstanceIdentifier.empty(), DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, FiniteDuration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().abortFromTransactionActor();

        future = akka.pattern.Patterns.ask(subject, new ReadData(YangInstanceIdentifier.empty(),
                DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, FiniteDuration.create(3, TimeUnit.SECONDS));
    }

    @Test(expected = ReadFailedException.class)
    public void testNegativeExistsWithReadWriteTransactionClosed() throws Exception {

        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(RW, STORE.newReadWriteTransaction(nextTransactionId()), shard,
                datastoreContext, shardStats);

        final TestActorRef<ShardTransaction> subject = TestActorRef.create(getSystem(), props,
                "testNegativeExistsWithReadWriteTransactionClosed");

        Future<Object> future = akka.pattern.Patterns.ask(subject,
                new DataExists(YangInstanceIdentifier.empty(), DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, FiniteDuration.create(3, TimeUnit.SECONDS));

        subject.underlyingActor().getDOMStoreTransaction().abortFromTransactionActor();

        future = akka.pattern.Patterns.ask(subject,
                new DataExists(YangInstanceIdentifier.empty(), DataStoreVersions.CURRENT_VERSION), 3000);
        Await.result(future, FiniteDuration.create(3, TimeUnit.SECONDS));
    }
}
