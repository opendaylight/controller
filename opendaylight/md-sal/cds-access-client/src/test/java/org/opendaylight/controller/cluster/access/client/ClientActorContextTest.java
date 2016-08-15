/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import akka.actor.ActorRef;
import akka.actor.Scheduler;
import akka.dispatch.Dispatcher;
import com.google.common.base.Ticker;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.DeadHistoryException;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

public class ClientActorContextTest {
    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName(ClientActorContextTest.class.getSimpleName());
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    private static final String PERSISTENCE_ID = ClientActorContextTest.class.getSimpleName();

    @Mock
    private ActorRef mockSelf;
    @Mock
    private Scheduler mockScheduler;
    @Mock
    private Dispatcher mockDispatcher;
    @Mock
    private RequestException mockCause;
    @Mock
    private ClientActorBehavior mockActorBehavior;

    private Long mockCookie;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockCookie = ThreadLocalRandom.current().nextLong();
    }

    @Test
    public void testMockingControl() {
        final ClientActorContext ctx = new ClientActorContext(mockSelf,mockScheduler,mockDispatcher,PERSISTENCE_ID,CLIENT_ID);
        assertSame(CLIENT_ID, ctx.getIdentifier());
        assertSame(PERSISTENCE_ID, ctx.persistenceId());
        assertSame(mockSelf, ctx.self());
    }

    @Test
    public void testTicker() {
        final ClientActorContext ctx = new ClientActorContext(mockSelf,mockScheduler,mockDispatcher,PERSISTENCE_ID,CLIENT_ID);
        assertSame(Ticker.systemTicker(), ctx.ticker());
    }

    @Test
    public void testQueueFor() {
        final ClientActorContext ctx = new ClientActorContext(mockSelf,mockScheduler,mockDispatcher,PERSISTENCE_ID,CLIENT_ID);
        final SequencedQueue seqQueue = ctx.queueFor(mockCookie);
        final Long qCookie = seqQueue.getCookie();
        assertSame(qCookie, mockCookie);
        assertFalse(seqQueue.hasCompleted());
    }

    @Test
    public void testPoison() {
        final ClientActorContext ctx = spy(
                new ClientActorContext(mockSelf, mockScheduler, mockDispatcher, PERSISTENCE_ID, CLIENT_ID));
        final SequencedQueue seqQueue = ctx.queueFor(mockCookie);
        final Long qCookie = seqQueue.getCookie();
        assertSame(qCookie, mockCookie);
        assertFalse(seqQueue.hasCompleted());
        ctx.poison(mockCause);
        assertTrue(seqQueue.hasCompleted());
    }

    @Test
    public void testRemoveQueue() {
        final ClientActorContext ctx = new ClientActorContext(mockSelf,mockScheduler,mockDispatcher,PERSISTENCE_ID,CLIENT_ID);
        final SequencedQueue seqQueue = ctx.queueFor(mockCookie);
        final Long qCookie = seqQueue.getCookie();
        assertSame(qCookie, mockCookie);
        assertFalse(seqQueue.hasCompleted());
        ctx.removeQueue(seqQueue);
        ctx.poison(mockCause);
        assertFalse(seqQueue.hasCompleted());
    }

    @Test
    public void testCompleteRequest() {
        final TransactionIdentifier target = makeTransactionIdentifier();
        final long newGeneration = ThreadLocalRandom.current().nextLong();
        final RequestException exception = new DeadHistoryException(newGeneration);
        final MockFailure failureRequest = new MockFailure(target, exception);
        final long sessionId = ThreadLocalRandom.current().nextLong();
        final long txSequence = ThreadLocalRandom.current().nextLong();
        final FailureEnvelope fe = new FailureEnvelope(failureRequest, sessionId, txSequence);

        final ClientActorContext ctx = spy(new ClientActorContext(mockSelf,mockScheduler,mockDispatcher,PERSISTENCE_ID,CLIENT_ID));
        final ClientActorBehavior actorBehavior = ctx.completeRequest(mockActorBehavior, fe);
        assertNotNull(actorBehavior);
        assertSame(mockActorBehavior, actorBehavior);
    }

    private static TransactionIdentifier makeTransactionIdentifier() {
        final long mockHistoryId = ThreadLocalRandom.current().nextLong();
        final long transactionId = ThreadLocalRandom.current().nextLong();
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(CLIENT_ID, mockHistoryId);
        return new TransactionIdentifier(historyId, transactionId);
    }

    private class MockFailure extends RequestFailure<WritableIdentifier, MockFailure> {
        private static final long serialVersionUID = 1L;

        MockFailure(final WritableIdentifier target, final RequestException cause) {
            super(target, 0, cause);
        }

        @Override
        protected AbstractRequestFailureProxy<WritableIdentifier, MockFailure> externalizableProxy(
                final ABIVersion version) {
            return null;
        }

        @Override
        protected MockFailure cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }
}
