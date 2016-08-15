/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import com.google.common.base.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.DeadHistoryException;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.Message;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.common.actor.TestTicker;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public final class ClientActorBehaviorTest {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");

    @Mock
    private ClientActorContext mockClientActorCtx;
    @Mock
    private ClientActorBehavior mockClientActorBehavior;
    @Mock
    private ActorRef mockActorRef;
    @Mock
    private RequestCallback mockCallback;

    private TestClientActorBehaviorTest clientActorBehavior;
    protected TestBackendInfoResolver backendInfoResolver;
    private ClientIdentifier clientId;
    private SequencedQueue mockQueue;

    @Before
    public void initialization() {
        MockitoAnnotations.initMocks(this);

        final FrontendType frontendType = FrontendType.forName(getClass().getSimpleName());
        final FrontendIdentifier frontendId = FrontendIdentifier.create(MEMBER_NAME, frontendType);
        clientId = ClientIdentifier.create(frontendId, 0);

        doReturn(clientId).when(mockClientActorCtx).getIdentifier();
        doReturn(mockClientActorBehavior).when(mockClientActorCtx).completeRequest(any(ClientActorBehavior.class),
                any(ResponseEnvelope.class));
        doNothing().when(mockClientActorCtx).poison(any(RequestException.class));

        clientActorBehavior = spy(new TestClientActorBehaviorTest(mockClientActorCtx));
    }

    @Test
    public void testGetIdentifier() {
        final ClientIdentifier clientIdent = clientActorBehavior.getIdentifier();
        assertTrue(clientIdent != null);
        assertSame(clientIdent, clientId);
    }

    @Test
    public void testOnReceiveCommandSuccessEnvelope() {
        final long sequence = ThreadLocalRandom.current().nextLong();
        final TransactionIdentifier identifier = makeTransactionIdentifier();
        final Optional<NormalizedNode<?, ?>> data = Optional.absent();
        final RequestSuccess<?, ?> message = new ReadTransactionSuccess(identifier, sequence, data);
        final long sessionId = ThreadLocalRandom.current().nextLong();
        final long txSequence = ThreadLocalRandom.current().nextLong();
        final SuccessEnvelope se = new SuccessEnvelope(message, sessionId, txSequence);

        final ClientActorBehavior resultActorBehavior = clientActorBehavior.onReceiveCommand(se);
        verify(mockClientActorCtx).completeRequest(clientActorBehavior, se);
        assertNotNull(resultActorBehavior);
        assertSame(resultActorBehavior, mockClientActorBehavior);
    }

    @Test
    public void testOnReceiveCommandFailureEnvelopeRetiredGenerationException() {
        final TransactionIdentifier target = makeTransactionIdentifier();
        final long sequence = ThreadLocalRandom.current().nextLong();
        final long newGeneration = ThreadLocalRandom.current().nextLong();
        final RequestException exception = new RetiredGenerationException(newGeneration);
        final RequestFailure<TransactionIdentifier, ?> message = new RequestFailure(target, sequence, exception) {

            @Override
            protected AbstractRequestFailureProxy externalizableProxy(final ABIVersion version) {
                return null;
            }

            @Override
            protected Message cloneAsVersion(final ABIVersion version) {
                return null;
            }
        };
        final long sessionId = ThreadLocalRandom.current().nextLong();
        final long txSequence = ThreadLocalRandom.current().nextLong();
        final FailureEnvelope fe = new FailureEnvelope(message, sessionId, txSequence);

        final ClientActorBehavior resultActorBehavior = clientActorBehavior.onReceiveCommand(fe);
        verify(clientActorBehavior).haltClient(any(Throwable.class));
        verify(mockClientActorCtx).poison(exception);
        assertNull(resultActorBehavior);
    }

    @Test
    public void testOnReceiveCommandFailureEnvelope() {
        final TransactionIdentifier target = makeTransactionIdentifier();
        final long newGeneration = ThreadLocalRandom.current().nextLong();
        final RequestException exception = new DeadHistoryException(newGeneration);
        final MockFailure failureRequest = new MockFailure(target, exception);
        final long sessionId = ThreadLocalRandom.current().nextLong();
        final long txSequence = ThreadLocalRandom.current().nextLong();
        final FailureEnvelope fe = new FailureEnvelope(failureRequest, sessionId, txSequence);

        final ClientActorBehavior resultActorBehavior = clientActorBehavior.onReceiveCommand(fe);
        verify(mockClientActorCtx).completeRequest(clientActorBehavior, fe);
        assertNotNull(resultActorBehavior);
        assertSame(resultActorBehavior, mockClientActorBehavior);
    }

    @Test
    public void testOnReceiveCommandInternalCommand() {
        final InternalCommand command = new InternalCommand() {

            @Override
            public ClientActorBehavior execute(final ClientActorBehavior currentBehavior) {
                return mockClientActorBehavior;
            }
        };
        final ClientActorBehavior resultActorBehavior = clientActorBehavior.onReceiveCommand(command);
        assertNotNull(resultActorBehavior);
        assertSame(resultActorBehavior, mockClientActorBehavior);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOnReceiveCommand() {
        clientActorBehavior.onReceiveCommand(new Object());
    }

    @Test
    public void testSendRequest() {
        doAnswer(new Answer<ClientActorBehavior>() {

            @Override
            public ClientActorBehavior answer(final InvocationOnMock invocation) throws Throwable {
                final InternalCommand ic = (InternalCommand) (invocation.getArguments())[0];
                return ic.execute(clientActorBehavior);
            }
        }).when(mockClientActorCtx).executeInActor(any(InternalCommand.class));

        final TestTicker ticker = new TestTicker();
        ticker.increment(ThreadLocalRandom.current().nextLong());
        final TransactionIdentifier target = makeTransactionIdentifier();
        mockQueue = new SequencedQueue(target.getHistoryId().getCookie(), ticker);
        doReturn(mockQueue).when(mockClientActorCtx).queueFor(target.getHistoryId().getCookie());
        clientActorBehavior = spy(new TestClientActorBehaviorTest(mockClientActorCtx));

        final long sequence = ThreadLocalRandom.current().nextLong();
        final TransactionRequest<?> request = new TransactionAbortRequest(target, sequence, mockActorRef);
        clientActorBehavior.sendRequest(request, mockCallback);
        verify(clientActorBehavior).resolver();
        verify(backendInfoResolver).resolveBackendInfo(target.getHistoryId().getCookie());
    }

    private TransactionIdentifier makeTransactionIdentifier() {
        final long mockHistoryId = ThreadLocalRandom.current().nextLong();
        final long transactionId = ThreadLocalRandom.current().nextLong();
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(clientId, mockHistoryId);
        return new TransactionIdentifier(historyId, transactionId);
    }

    private class TestClientActorBehaviorTest extends ClientActorBehavior {

        protected TestClientActorBehaviorTest(final ClientActorContext context) {
            super(context);
        }

        @Override
        protected void haltClient(final Throwable cause) {
            // NOOP - do nothing it is only TestObject
        }

        @Override
        protected ClientActorBehavior onCommand(final Object command) {
            throw new UnsupportedOperationException("Test Object implementation doesn't support this operation!");
        }

        @Override
        protected BackendInfoResolver<?> resolver() {
            backendInfoResolver = spy(new TestBackendInfoResolver());
            return backendInfoResolver;
        }

    }

    private class TestBackendInfoResolver extends BackendInfoResolver<BackendInfo> {

        TestBackendInfoResolver() {
            // TODO Auto-generated constructor stub
        }

        @Override
        protected CompletableFuture<BackendInfo> resolveBackendInfo(final Long cookie) {
            return new CompletableFuture<>();
        }

        @Override
        protected void invalidateBackendInfo(final CompletionStage<? extends BackendInfo> info) {
            throw new UnsupportedOperationException("Test Object implementation doesn't support this operation!");
        }

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
