/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionFailure;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class ClientTransactionCommitCohortTest {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("type-1");
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    private static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0L);
    private static final String PERSISTENCE_ID = "per-1";
    private static final TransactionIdentifier TRANSACTION_ID = new TransactionIdentifier(HISTORY_ID, 0L);
    private static final int TRANSACTIONS = 3;

    @Mock
    private AbstractClientHistory history;
    private ActorSystem system;
    private List<TranasactionTester> transactions;
    private ClientTransactionCommitCohort cohort;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.apply();
        final TestProbe clientContextProbe = new TestProbe(system, "clientContext");
        final ClientActorContext context =
                AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        transactions = new ArrayList<>();
        for (int i = 0; i < TRANSACTIONS; i++) {
            transactions.add(createTransactionTester(new TestProbe(system, "backend" + i), context, history));
        }
        final Collection<AbstractProxyTransaction> proxies = transactions.stream()
                .map(TranasactionTester::getTransaction)
                .collect(Collectors.toList());
        proxies.forEach(AbstractProxyTransaction::seal);
        cohort = new ClientTransactionCommitCohort(history, TRANSACTION_ID, proxies);
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void testCanCommit() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::canCommit, TranasactionTester::expectCanCommit,
                TranasactionTester::replyCanCommitSuccess, true);
    }

    @Test
    public void testCanCommitFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::canCommit, TranasactionTester::expectCanCommit,
                TranasactionTester::replyCanCommitSuccess);
    }

    @Test
    public void testPreCommit() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::preCommit, TranasactionTester::expectPreCommit,
                TranasactionTester::replyPreCommitSuccess, null);
    }

    @Test
    public void testPreCommitFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::preCommit, TranasactionTester::expectPreCommit,
                TranasactionTester::replyPreCommitSuccess);
    }

    @Test
    public void testCommit() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::commit, TranasactionTester::expectCommit,
                TranasactionTester::replyCommitSuccess, null);
    }

    @Test
    public void testCommitFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::commit, TranasactionTester::expectCommit,
                TranasactionTester::replyCommitSuccess);
    }

    @Test
    public void testAbort() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::abort, TranasactionTester::expectAbort,
                TranasactionTester::replyAbortSuccess, null);
    }

    @Test
    public void testAbortFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::abort, TranasactionTester::expectAbort,
                TranasactionTester::replyAbortSuccess);
    }

    private static TranasactionTester createTransactionTester(final TestProbe backendProbe,
                                                              final ClientActorContext context,
                                                              final AbstractClientHistory history) {
        final ShardBackendInfo backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.BORON,
                "default", UnsignedLong.ZERO, Optional.empty(), 3);
        final AbstractClientConnection<ShardBackendInfo> connection =
                AccessClientUtil.createConnectedConnection(context, 0L, backend);
        final ProxyHistory proxyHistory = ProxyHistory.createClient(history, connection, HISTORY_ID);
        final RemoteProxyTransaction transaction =
                new RemoteProxyTransaction(proxyHistory, TRANSACTION_ID, false, false);
        return new TranasactionTester(transaction, connection, backendProbe);
    }


    private void replySuccess(final Collection<TranasactionTester> transactions,
                              final Consumer<TranasactionTester> expect,
                              final Consumer<TranasactionTester> reply) {
        for (final TranasactionTester transaction : transactions) {
            expect.accept(transaction);
            reply.accept(transaction);
        }
    }

    /**
     * Test operation success. Invokes given operation, which initiates message to the backend.
     * Received message is checked by expectFunction. Then replyFunction is invoked. Expected result is compared
     * to the operation future result.
     *
     * @param operation      operation
     * @param expectFunction expected message check
     * @param replyFunction  response function
     * @param expectedResult expected operation result
     * @param <T>            type
     * @throws Exception unexpected exception
     */
    private <T> void testOpSuccess(final Function<ClientTransactionCommitCohort, ListenableFuture<T>> operation,
                                   final Consumer<TranasactionTester> expectFunction,
                                   final Consumer<TranasactionTester> replyFunction,
                                   final T expectedResult) throws Exception {
        final ListenableFuture<T> result = operation.apply(cohort);
        replySuccess(transactions, expectFunction, replyFunction);
        Assert.assertEquals(expectedResult, getWithTimeout(result));
    }

    /**
     * Test operation failure. Invokes given operation, which initiates message to the backend.
     * Received message is checked by expectFunction. Then replyFunction is invoked. One of the transactions in
     * cohort receives failure response.
     *
     * @param operation      operation
     * @param expectFunction expected message check
     * @param replyFunction  response function
     * @param <T>            type
     * @throws Exception unexpected exception
     */
    private <T> void testOpFail(final Function<ClientTransactionCommitCohort, ListenableFuture<T>> operation,
                                final Consumer<TranasactionTester> expectFunction,
                                final Consumer<TranasactionTester> replyFunction) throws Exception {
        final ListenableFuture<T> canCommit = operation.apply(cohort);
        //reply success to all except last transaction
        replySuccess(transactions.subList(0, transactions.size() - 1), expectFunction, replyFunction);
        //reply fail to last transaction
        final TranasactionTester last = transactions.get(transactions.size() - 1);
        expectFunction.accept(last);
        final RuntimeRequestException cause = new RuntimeRequestException("fail", new RuntimeException());
        last.replyFailure(cause);
        //check future fail
        final ExecutionException exception =
                assertOperationThrowsException(() -> getWithTimeout(canCommit), ExecutionException.class);
        Assert.assertEquals(cause, exception.getCause());
    }

    /**
     * Helper class. Allows checking messages received by backend and respond to them.
     */
    private static class TranasactionTester {

        private final RemoteProxyTransaction transaction;
        private final AbstractClientConnection<ShardBackendInfo> connection;
        private final TestProbe backendProbe;
        private RequestEnvelope envelope;

        private TranasactionTester(final RemoteProxyTransaction transaction,
                                   final AbstractClientConnection<ShardBackendInfo> connection,
                                   final TestProbe backendProbe) {
            this.transaction = transaction;
            this.connection = connection;
            this.backendProbe = backendProbe;
        }

        private RemoteProxyTransaction getTransaction() {
            return transaction;
        }

        private void expectCanCommit() {
            envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
            final Request<?, ?> message = envelope.getMessage();
            Assert.assertTrue(message instanceof ModifyTransactionRequest);
            final ModifyTransactionRequest request = (ModifyTransactionRequest) message;
            Assert.assertTrue(request.getPersistenceProtocol().isPresent());
            Assert.assertEquals(PersistenceProtocol.THREE_PHASE, request.getPersistenceProtocol().get());
        }

        private void replyCanCommitSuccess() {
            final RequestSuccess<?, ?> success = new TransactionCanCommitSuccess(TRANSACTION_ID,
                    envelope.getMessage().getSequence());
            sendSuccess(success);
        }

        private void expectPreCommit() {
            envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
            final Request<?, ?> message = envelope.getMessage();
            Assert.assertTrue(message instanceof TransactionPreCommitRequest);
        }

        private void replyPreCommitSuccess() {
            final RequestSuccess<?, ?> success = new TransactionPreCommitSuccess(TRANSACTION_ID,
                    envelope.getMessage().getSequence());
            sendSuccess(success);
        }

        private void expectCommit() {
            envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
            final Request<?, ?> message = envelope.getMessage();
            Assert.assertTrue(message instanceof TransactionDoCommitRequest);
        }

        private void replyCommitSuccess() {
            final RequestSuccess<?, ?> success = new TransactionCommitSuccess(TRANSACTION_ID,
                    envelope.getMessage().getSequence());
            sendSuccess(success);
        }

        private void expectAbort() {
            envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
            final Request<?, ?> message = envelope.getMessage();
            Assert.assertTrue(message instanceof TransactionAbortRequest);
        }

        private void replyAbortSuccess() {
            final RequestSuccess<?, ?> success = new TransactionAbortSuccess(TRANSACTION_ID,
                    envelope.getMessage().getSequence());
            sendSuccess(success);
        }

        private void sendSuccess(final RequestSuccess<?, ?> success) {
            final long sessionId = envelope.getSessionId();
            final long txSequence = envelope.getTxSequence();
            final long executionTime = 0L;
            final SuccessEnvelope responseEnvelope = new SuccessEnvelope(success, sessionId, txSequence, executionTime);
            AccessClientUtil.completeRequest(connection, responseEnvelope);
        }

        private void replyFailure(final RequestException cause) {
            final long sessionId = envelope.getSessionId();
            final long txSequence = envelope.getTxSequence();
            final long executionTime = 0L;
            final RequestFailure<?, ?> fail =
                    new MockFailure(TRANSACTION_ID, envelope.getMessage().getSequence(), cause);
            final FailureEnvelope responseEnvelope = new FailureEnvelope(fail, sessionId, txSequence, executionTime);
            AccessClientUtil.completeRequest(connection, responseEnvelope);
        }
    }

    private static class MockFailure extends RequestFailure<TransactionIdentifier, TransactionFailure> {
        private MockFailure(@Nonnull final TransactionIdentifier target, final long sequence,
                            @Nonnull final RequestException cause) {
            super(target, sequence, cause);
        }

        @Nonnull
        @Override
        protected TransactionFailure cloneAsVersion(@Nonnull final ABIVersion targetVersion) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        protected AbstractRequestFailureProxy<TransactionIdentifier, TransactionFailure>
        externalizableProxy(@Nonnull final ABIVersion version) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}