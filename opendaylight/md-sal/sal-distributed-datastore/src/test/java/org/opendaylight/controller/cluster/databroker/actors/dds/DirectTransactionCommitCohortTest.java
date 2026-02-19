/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.HISTORY_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
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
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DirectTransactionCommitCohortTest {
    private static final String PERSISTENCE_ID = "per-1";

    @Mock
    private AbstractClientHistory history;
    @Mock
    private DatastoreContext datastoreContext;
    @Mock
    private ActorUtils actorUtils;

    private ActorSystem system;
    private TransactionTester<?> transaction;
    private DirectTransactionCommitCohort cohort;

    @Before
    public void setUp() {
        system = ActorSystem.apply();
        final TestProbe clientContextProbe = new TestProbe(system, "clientContext");
        final ClientActorContext context =
                AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(actorUtils).when(history).actorUtils();

        transaction = createTransactionTester(new TestProbe(system, "backend"), context, history);
        final AbstractProxyTransaction proxy = transaction.getTransaction();
        proxy.seal();
        cohort = new DirectTransactionCommitCohort(history, TRANSACTION_ID, proxy);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testCanCommit() throws Exception {
        final ListenableFuture<Boolean> canCommit = cohort.canCommit();
        final ModifyTransactionRequest request = transaction.expectTransactionRequest(ModifyTransactionRequest.class);
        assertEquals(Optional.of(PersistenceProtocol.SIMPLE), request.getPersistenceProtocol());
        final RequestSuccess<?, ?> success = new TransactionCommitSuccess(transaction.getTransaction().getIdentifier(),
                transaction.getLastReceivedMessage().getSequence());
        transaction.replySuccess(success);
        assertEquals(Boolean.TRUE, getWithTimeout(canCommit));
    }

    @Test
    public void testPreCommit() throws Exception {
        final ListenableFuture<?> preCommit = cohort.preCommit();
        assertNotNull(getWithTimeout(preCommit));
    }

    @Test
    public void testAbort() throws Exception {
        final ListenableFuture<?> abort = cohort.abort();
        verify(history).onTransactionComplete(transaction.getTransaction().getIdentifier());
        assertNotNull(getWithTimeout(abort));
    }

    @Test
    public void testCommit() throws Exception {
        final ListenableFuture<?> commit = cohort.commit();
        verify(history).onTransactionComplete(transaction.getTransaction().getIdentifier());
        assertNotNull(getWithTimeout(commit));
    }

    private static TransactionTester<?> createTransactionTester(final TestProbe backendProbe,
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

}
