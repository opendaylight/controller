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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
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

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

    static {
        store.onGlobalContextUpdated(testSchemaContext);
    }


    @Test(expected = Exception.class)
    public void testNegativeAbortResultsInException() throws Exception {

        final ActorRef shard =
            getSystem()
                .actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP));
        final DOMStoreThreePhaseCommitCohort mockCohort = Mockito
            .mock(DOMStoreThreePhaseCommitCohort.class);
        final CompositeModification mockComposite =
            Mockito.mock(CompositeModification.class);
        final Props props =
            ThreePhaseCommitCohort.props(mockCohort, shard, mockComposite);

        final TestActorRef<ThreePhaseCommitCohort> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativeAbortResultsInException");

        when(mockCohort.abort()).thenReturn(
            Futures.<Void>immediateFailedFuture(new Exception()));

        Future<Object> future =
            akka.pattern.Patterns.ask(subject,
                ThreePhaseCommitCohortMessages.AbortTransaction.newBuilder()
                    .build(), 3000);
        assertTrue(future.isCompleted());

        Await.result(future, Duration.Zero());


        fail(
            "if we reach here -- the exception should have occurred before this");


    }


    @Test(expected = Exception.class)
    public void testNegativeCanCommitResultsInException() throws Exception {

        final ActorRef shard =
            getSystem()
                .actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP));
        final DOMStoreThreePhaseCommitCohort mockCohort = Mockito
            .mock(DOMStoreThreePhaseCommitCohort.class);
        final CompositeModification mockComposite =
            Mockito.mock(CompositeModification.class);
        final Props props =
            ThreePhaseCommitCohort.props(mockCohort, shard, mockComposite);

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
        assertTrue(future.isCompleted());

        Await.result(future, Duration.Zero());

        fail(
            "if we reach here -- the exception should have occurred before this");
    }


    @Test(expected = Exception.class)
    public void testNegativePreCommitResultsInException() throws Exception {

        final ActorRef shard =
            getSystem()
                .actorOf(Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP));
        final DOMStoreThreePhaseCommitCohort mockCohort = Mockito
            .mock(DOMStoreThreePhaseCommitCohort.class);
        final CompositeModification mockComposite =
            Mockito.mock(CompositeModification.class);
        final Props props =
            ThreePhaseCommitCohort.props(mockCohort, shard, mockComposite);

        final TestActorRef<ThreePhaseCommitCohort> subject = TestActorRef
            .create(getSystem(), props,
                "testNegativePreCommitResultsInException");

        when(mockCohort.preCommit()).thenReturn(
            Futures
                .<Void>immediateFailedFuture(
                    new IllegalStateException("some exception")));

        Future<Object> future =
            akka.pattern.Patterns.ask(subject,
                ThreePhaseCommitCohortMessages.PreCommitTransaction.newBuilder()
                    .build(), 3000);
        assertTrue(future.isCompleted());

        Await.result(future, Duration.Zero());

        fail(
            "if we reach here -- the exception should have occurred before this");
    }

    @Test(expected = Exception.class)
    public void testNegativeCommitResultsInException() throws Exception {



        final TestActorRef<Shard> subject = TestActorRef
            .create(getSystem(),
                Shard.props(SHARD_IDENTIFIER, Collections.EMPTY_MAP),
                "testNegativeCommitResultsInException");



        final ActorRef shardTransaction =
            getSystem().actorOf(
                ShardTransaction.props(store.newReadWriteTransaction(), subject,
                    TestModel.createTestContext()));

        ShardTransactionMessages.WriteData writeData =
            ShardTransactionMessages.WriteData.newBuilder()
                .setInstanceIdentifierPathArguments(
                    NormalizedNodeMessages.InstanceIdentifier.newBuilder()
                        .build()).setNormalizedNode(
                NormalizedNodeMessages.Node.newBuilder().build()

            ).build();

        Future future =
            akka.pattern.Patterns.ask(shardTransaction, writeData, 3000);


        ShardTransactionMessages.ReadyTransaction readyTransaction =
            ShardTransactionMessages.ReadyTransaction.newBuilder().build();

        future =
            akka.pattern.Patterns.ask(shardTransaction, readyTransaction, 3000);


        ForwardedCommitTransaction mockForwardCommitTransaction =
            Mockito.mock(ForwardedCommitTransaction.class);
        DOMStoreThreePhaseCommitCohort mockThreePhaseCommitTransaction =
            Mockito.mock(DOMStoreThreePhaseCommitCohort.class);
        when(mockForwardCommitTransaction.getCohort())
            .thenReturn(mockThreePhaseCommitTransaction);
        when(mockThreePhaseCommitTransaction.commit()).thenReturn(Futures
            .<Void>immediateFailedFuture(
                new IllegalStateException("some exception")));
        Modification mockModification = Mockito.mock(
            Modification.class);
        when(mockForwardCommitTransaction.getModification())
            .thenReturn(mockModification);

        when(mockModification.toSerializable()).thenReturn(
            PersistentMessages.CompositeModification.newBuilder().build());



        future =
            akka.pattern.Patterns.ask(subject,
                mockForwardCommitTransaction
                , 3000);
        Await.result(future, Duration.Zero());

        fail(
            "if we reach here -- the exception should have occurred before this");
    }



}
