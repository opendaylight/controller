/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.OnComplete;
import akka.testkit.javadsl.TestKit;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Test whether RmoteTransactionContext operates correctly.
 */
public class RemoteTransactionContextTest extends AbstractActorTest {
    private static final TransactionIdentifier TX_ID = new TransactionIdentifier(new LocalHistoryIdentifier(
        ClientIdentifier.create(FrontendIdentifier.create(MemberName.forName("test"), FrontendType.forName("test")), 0),
        0), 0);

    private OperationLimiter limiter;
    private RemoteTransactionContext txContext;
    private ActorUtils actorUtils;
    private TestKit kit;

    @Before
    public void before() {
        kit = new TestKit(getSystem());
        actorUtils = Mockito.spy(new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class)));
        limiter = new OperationLimiter(TX_ID, 4, 0);
        txContext = new RemoteTransactionContext(TX_ID, actorUtils.actorSelection(kit.getRef().path()), actorUtils,
            DataStoreVersions.CURRENT_VERSION, limiter);
        txContext.operationHandOffComplete();
    }

    /**
     * OperationLimiter should be correctly released when a failure, like AskTimeoutException occurs. Future reads
     * need to complete immediately with the failure and modifications should not be throttled and thrown away
     * immediately.
     */
    @Test
    public void testLimiterOnFailure() throws TimeoutException, InterruptedException {
        txContext.executeDelete(null, null);
        txContext.executeDelete(null, null);
        assertEquals(2, limiter.availablePermits());

        final Future<Object> sendFuture = txContext.sendBatchedModifications();
        assertEquals(2, limiter.availablePermits());

        BatchedModifications msg = kit.expectMsgClass(BatchedModifications.class);
        assertEquals(2, msg.getModifications().size());
        assertEquals(1, msg.getTotalMessagesSent());
        sendReply(new Failure(new NullPointerException()));
        assertFuture(sendFuture, new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                assertTrue(failure instanceof NullPointerException);
                assertEquals(4, limiter.availablePermits());

                // The transaction has failed, no throttling should occur
                txContext.executeDelete(null, null);
                assertEquals(4, limiter.availablePermits());

                // Executing a read should result in immediate failure
                final SettableFuture<Boolean> readFuture = SettableFuture.create();
                txContext.executeRead(new DataExists(), readFuture, null);
                assertTrue(readFuture.isDone());
                try {
                    readFuture.get();
                    fail("Read future did not fail");
                } catch (ExecutionException | InterruptedException e) {
                    assertTrue(e.getCause() instanceof NullPointerException);
                }
            }
        });

        final Future<Object> commitFuture = txContext.directCommit(null);

        msg = kit.expectMsgClass(BatchedModifications.class);
        // Modification should have been thrown away by the dropped transmit induced by executeRead()
        assertEquals(0, msg.getModifications().size());
        assertTrue(msg.isDoCommitOnReady());
        assertTrue(msg.isReady());
        assertEquals(2, msg.getTotalMessagesSent());
        sendReply(new Failure(new IllegalStateException()));
        assertFuture(commitFuture, new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                assertTrue(failure instanceof IllegalStateException);
            }
        });

        kit.expectNoMessage();
    }

    /**
     * OperationLimiter gives up throttling at some point -- {@link RemoteTransactionContext} needs to deal with that
     * case, too.
     */
    @Test
    public void testLimiterOnOverflowFailure() throws TimeoutException, InterruptedException {
        txContext.executeDelete(null, null);
        txContext.executeDelete(null, null);
        txContext.executeDelete(null, null);
        txContext.executeDelete(null, null);
        assertEquals(0, limiter.availablePermits());
        txContext.executeDelete(null, null);
        // Last acquire should have failed ...
        assertEquals(0, limiter.availablePermits());

        final Future<Object> future = txContext.sendBatchedModifications();
        assertEquals(0, limiter.availablePermits());

        BatchedModifications msg = kit.expectMsgClass(BatchedModifications.class);
        // ... so we are sending 5 modifications ...
        assertEquals(5, msg.getModifications().size());
        assertEquals(1, msg.getTotalMessagesSent());
        sendReply(new Failure(new NullPointerException()));

        assertFuture(future, new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                assertTrue(failure instanceof NullPointerException);
                // ... but they account for only 4 permits.
                assertEquals(4, limiter.availablePermits());
            }
        });

        kit.expectNoMessage();
    }

    private void sendReply(final Object message) {
        final ActorRef askActor = kit.getLastSender();
        kit.watch(askActor);
        kit.reply(new Failure(new IllegalStateException()));
        kit.expectTerminated(askActor);
    }

    private static void assertFuture(final Future<Object> future, final OnComplete<Object> complete)
            throws TimeoutException, InterruptedException {
        Await.ready(future, FiniteDuration.apply(3, TimeUnit.SECONDS));
        future.onComplete(complete, ExecutionContexts.fromExecutor(MoreExecutors.directExecutor()));
    }
}
