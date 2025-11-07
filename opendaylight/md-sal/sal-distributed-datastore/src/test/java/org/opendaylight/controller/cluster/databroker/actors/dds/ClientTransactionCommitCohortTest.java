/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.HISTORY_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

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
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.common.Empty;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ClientTransactionCommitCohortTest {
    private static final String PERSISTENCE_ID = "per-1";
    private static final int TRANSACTIONS = 3;

    private final List<TransactionTester<RemoteProxyTransaction>> transactions = new ArrayList<>();

    @Mock
    private AbstractClientHistory history;
    @Mock
    private DatastoreContext datastoreContext;
    @Mock
    private ActorUtils actorUtils;

    private ActorSystem system;
    private ClientTransactionCommitCohort cohort;

    @Before
    public void setUp() {
        system = ActorSystem.apply();
        final TestProbe clientContextProbe = new TestProbe(system, "clientContext");
        final ClientActorContext context =
                AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(actorUtils).when(history).actorUtils();

        for (int i = 0; i < TRANSACTIONS; i++) {
            transactions.add(createTransactionTester(new TestProbe(system, "backend" + i), context, history));
        }
        final Collection<AbstractProxyTransaction> proxies = transactions.stream()
                .map(TransactionTester::getTransaction)
                .collect(Collectors.toList());
        proxies.forEach(AbstractProxyTransaction::seal);
        cohort = new ClientTransactionCommitCohort(history, TRANSACTION_ID, proxies);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testCanCommit() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::canCommit, this::expectCanCommit,
                this::replyCanCommitSuccess, Boolean.TRUE);
    }

    @Test
    public void testCanCommitFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::canCommit, this::expectCanCommit, this::replyCanCommitSuccess);
    }

    @Test
    public void testPreCommit() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::preCommit, this::expectPreCommit, this::replyPreCommitSuccess,
            Empty.value());
    }

    @Test
    public void testPreCommitFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::preCommit, this::expectPreCommit, this::replyPreCommitSuccess);
    }

    @Test
    public void testCommit() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::commit, this::expectCommit, this::replyCommitSuccess,
            CommitInfo.empty());
    }

    @Test
    public void testCommitFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::commit, this::expectCommit, this::replyCommitSuccess);
    }

    @Test
    public void testAbort() throws Exception {
        testOpSuccess(ClientTransactionCommitCohort::abort, this::expectAbort, this::replyAbortSuccess, Empty.value());
    }

    @Test
    public void testAbortFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::abort, this::expectAbort, this::replyAbortSuccess);
    }

    private void expectCanCommit(final TransactionTester<RemoteProxyTransaction> tester) {
        final ModifyTransactionRequest request = tester.expectTransactionRequest(ModifyTransactionRequest.class);
        assertEquals(Optional.of(PersistenceProtocol.THREE_PHASE), request.getPersistenceProtocol());
    }

    void expectPreCommit(final TransactionTester<?> tester) {
        tester.expectTransactionRequest(TransactionPreCommitRequest.class);
    }

    void expectCommit(final TransactionTester<?> tester) {
        tester.expectTransactionRequest(TransactionDoCommitRequest.class);
    }

    void expectAbort(final TransactionTester<?> tester) {
        tester.expectTransactionRequest(TransactionAbortRequest.class);
    }

    void replyCanCommitSuccess(final TransactionTester<?> tester) {
        final RequestSuccess<?, ?> success = new TransactionCanCommitSuccess(tester.getTransaction().getIdentifier(),
                tester.getLastReceivedMessage().getSequence());
        tester.replySuccess(success);
    }

    void replyPreCommitSuccess(final TransactionTester<?> tester) {
        final RequestSuccess<?, ?> success = new TransactionPreCommitSuccess(tester.getTransaction().getIdentifier(),
                tester.getLastReceivedMessage().getSequence());
        tester.replySuccess(success);
    }

    void replyCommitSuccess(final TransactionTester<?> tester) {
        final RequestSuccess<?, ?> success = new TransactionCommitSuccess(tester.getTransaction().getIdentifier(),
                tester.getLastReceivedMessage().getSequence());
        tester.replySuccess(success);
    }

    void replyAbortSuccess(final TransactionTester<?> tester) {
        final RequestSuccess<?, ?> success = new TransactionAbortSuccess(tester.getTransaction().getIdentifier(),
                tester.getLastReceivedMessage().getSequence());
        tester.replySuccess(success);
    }

    private static TransactionTester<RemoteProxyTransaction> createTransactionTester(final TestProbe backendProbe,
                                                             final ClientActorContext context,
                                                             final AbstractClientHistory history) {
        final ShardBackendInfo backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(),
                "default", UnsignedLong.ZERO, Optional.empty(), 3);
        final AbstractClientConnection<ShardBackendInfo> connection =
                AccessClientUtil.createConnectedConnection(context, 0L, backend);
        final ProxyHistory proxyHistory = ProxyHistory.createClient(history, connection, HISTORY_ID);
        final RemoteProxyTransaction transaction =
                new RemoteProxyTransaction(proxyHistory, TRANSACTION_ID, false, false, false);
        return new TransactionTester<>(transaction, connection, backendProbe);
    }

    private static <T extends AbstractProxyTransaction> void replySuccess(
            final Collection<TransactionTester<T>> transactions, final Consumer<TransactionTester<T>> expect,
            final Consumer<TransactionTester<T>> reply) {
        for (final var transaction : transactions) {
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
                                   final Consumer<TransactionTester<RemoteProxyTransaction>> expectFunction,
                                   final Consumer<TransactionTester<RemoteProxyTransaction>> replyFunction,
                                   final T expectedResult) throws Exception {
        final ListenableFuture<T> result = operation.apply(cohort);
        replySuccess(transactions, expectFunction, replyFunction);
        assertEquals(expectedResult, getWithTimeout(result));
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
            final Consumer<TransactionTester<RemoteProxyTransaction>> expectFunction,
            final Consumer<TransactionTester<RemoteProxyTransaction>> replyFunction) throws Exception {
        final ListenableFuture<T> canCommit = operation.apply(cohort);
        //reply success to all except last transaction
        replySuccess(transactions.subList(0, transactions.size() - 1), expectFunction, replyFunction);
        //reply fail to last transaction
        final TransactionTester<RemoteProxyTransaction> last = transactions.get(transactions.size() - 1);
        expectFunction.accept(last);
        final RuntimeException e = new RuntimeException();
        final RuntimeRequestException cause = new RuntimeRequestException("fail", e);
        last.replyFailure(cause);
        //check future fail
        final ExecutionException exception =
                assertOperationThrowsException(() -> getWithTimeout(canCommit), ExecutionException.class);
        assertEquals(e, exception.getCause());
    }

}
