/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.HISTORY_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;
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
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;

public class ClientTransactionCommitCohortTest {

    private static final String PERSISTENCE_ID = "per-1";
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
        testOpSuccess(ClientTransactionCommitCohort::canCommit, this::expect3PhaseCanCommit,
                TranasactionTester::replyCanCommitSuccess, true);
    }

    @Test
    public void testCanCommitFail() throws Exception {
        testOpFail(ClientTransactionCommitCohort::canCommit, this::expect3PhaseCanCommit,
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

    private void expect3PhaseCanCommit(final TranasactionTester transaction) {
        transaction.expectCanCommit(PersistenceProtocol.THREE_PHASE);
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

}