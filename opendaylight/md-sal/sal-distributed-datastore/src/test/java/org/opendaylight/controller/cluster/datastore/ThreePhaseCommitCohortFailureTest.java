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
import akka.util.Timeout;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedCommitTransaction;
import org.opendaylight.controller.cluster.datastore.modification.CompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


public class ThreePhaseCommitCohortFailureTest extends AbstractActorTest {

    private static ListeningExecutorService storeExecutor =
        MoreExecutors.listeningDecorator(MoreExecutors.sameThreadExecutor());

    private static final InMemoryDOMDataStore store =
        new InMemoryDOMDataStore("OPER", storeExecutor,
            MoreExecutors.sameThreadExecutor());

    private static final SchemaContext testSchemaContext =
        TestModel.createTestContext();

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("config").build();

    private final DatastoreContext datastoreContext = new DatastoreContext();

    private final ShardStats shardStats = new ShardStats(SHARD_IDENTIFIER.toString(), "DataStore");

    @BeforeClass
    public static void staticSetup() {
        store.onGlobalContextUpdated(testSchemaContext);
    }

    private final FiniteDuration ASK_RESULT_DURATION = Duration.create(5000, TimeUnit.MILLISECONDS);

    private ActorRef createShard(){
        return getSystem().actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP, datastoreContext, TestModel.createTestContext()));
    }

    @Test(expected = TestException.class)
    public void testNegativeAbortResultsInException() throws Exception {

        final ActorRef shard = createShard();
        final DOMStoreThreePhaseCommitCohort mockCohort = Mockito
            .mock(DOMStoreThreePhaseCommitCohort.class);
        final CompositeModification mockComposite =
            Mockito.mock(CompositeModification.class);
        final Props props =
            ThreePhaseCommitCohort.props(mockCohort, shard, mockComposite, shardStats);

        final TestActorRef<ThreePhaseCommitCohort> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeAbortResultsInException");

        when(mockCohort.abort()).thenReturn(
            Futures.<Void>immediateFailedFuture(new TestException()));

        Future<Object> future =
            akka.pattern.Patterns.ask(subject,
                ThreePhaseCommitCohortMessages.AbortTransaction.newBuilder()
                    .build(), 3000);
        assertTrue(future.isCompleted());

        Await.result(future, ASK_RESULT_DURATION);
    }


    @Test(expected = OptimisticLockFailedException.class)
    public void testNegativeCanCommitResultsInException() throws Exception {

        final ActorRef shard = createShard();
        final DOMStoreThreePhaseCommitCohort mockCohort = Mockito
            .mock(DOMStoreThreePhaseCommitCohort.class);
        final CompositeModification mockComposite =
            Mockito.mock(CompositeModification.class);
        final Props props =
            ThreePhaseCommitCohort.props(mockCohort, shard, mockComposite, shardStats);

        final TestActorRef<ThreePhaseCommitCohort> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeCanCommitResultsInException");

        when(mockCohort.canCommit()).thenReturn(
            Futures
                .<Boolean>immediateFailedFuture(
                    new OptimisticLockFailedException("some exception")));

        Future<Object> future =
            akka.pattern.Patterns.ask(subject,
                ThreePhaseCommitCohortMessages.CanCommitTransaction.newBuilder()
                    .build(), 3000);


        Await.result(future, ASK_RESULT_DURATION);

    }


    @Test(expected = TestException.class)
    public void testNegativePreCommitResultsInException() throws Exception {

        final ActorRef shard = createShard();
        final DOMStoreThreePhaseCommitCohort mockCohort = Mockito
            .mock(DOMStoreThreePhaseCommitCohort.class);
        final CompositeModification mockComposite =
            Mockito.mock(CompositeModification.class);
        final Props props =
            ThreePhaseCommitCohort.props(mockCohort, shard, mockComposite, shardStats);

        final TestActorRef<ThreePhaseCommitCohort> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativePreCommitResultsInException");

        when(mockCohort.preCommit()).thenReturn(
            Futures
                .<Void>immediateFailedFuture(
                    new TestException()));

        Future<Object> future =
            akka.pattern.Patterns.ask(subject,
                ThreePhaseCommitCohortMessages.PreCommitTransaction.newBuilder()
                    .build(), 3000);

        Await.result(future, ASK_RESULT_DURATION);

    }

    @Test(expected = TestException.class)
    public void testNegativeCommitResultsInException() throws Exception {

        final TestActorRef<Shard> subject = TestActorRef.create(getSystem(),
                Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP, datastoreContext, TestModel.createTestContext()),
                "testNegativeCommitResultsInException");

        final ActorRef shardTransaction =
            getSystem().actorOf(ShardTransaction.props(store.newReadWriteTransaction(), subject,
                    testSchemaContext, datastoreContext, shardStats));

        ShardTransactionMessages.WriteData writeData =
            ShardTransactionMessages.WriteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).setNormalizedNode(
                NormalizedNodeMessages.Node.newBuilder().build()

            ).build();

        Timeout askTimeout = new Timeout(ASK_RESULT_DURATION);

        //This is done so that Modification list is updated which is used during commit
        Future<Object> future = akka.pattern.Patterns.ask(shardTransaction, writeData, askTimeout);

        //ready transaction creates the cohort so that we get into the
        //block where in commmit is done
        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        future = akka.pattern.Patterns.ask(shardTransaction, readyTransaction, askTimeout);

        //but when the message is sent it will have the MockCommit object
        //so that we can simulate throwing of exception
        ForwardedCommitTransaction mockForwardCommitTransaction =
            Mockito.mock(ForwardedCommitTransaction.class);
        DOMStoreThreePhaseCommitCohort mockThreePhaseCommitTransaction =
            Mockito.mock(DOMStoreThreePhaseCommitCohort.class);
        when(mockForwardCommitTransaction.getCohort())
            .thenReturn(mockThreePhaseCommitTransaction);
        when(mockThreePhaseCommitTransaction.commit()).thenReturn(Futures
            .<Void>immediateFailedFuture(
                new TestException()));
        Modification mockModification = Mockito.mock(
            Modification.class);
        when(mockForwardCommitTransaction.getModification())
            .thenReturn(mockModification);

        when(mockModification.toSerializable()).thenReturn(
            PersistentMessages.CompositeModification.newBuilder().build());

        future = akka.pattern.Patterns.ask(subject, mockForwardCommitTransaction, askTimeout);
        Await.result(future, ASK_RESULT_DURATION);
    }

    private class TestException extends Exception {
    }
}
