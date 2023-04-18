/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Ticker;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
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
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public abstract class AbstractProxyTransactionTest<T extends AbstractProxyTransaction> {
    protected static final TransactionIdentifier TRANSACTION_ID = TestUtils.TRANSACTION_ID;
    private static final ClientIdentifier CLIENT_ID = TestUtils.CLIENT_ID;
    private static final LocalHistoryIdentifier HISTORY_ID = TestUtils.HISTORY_ID;

    protected static final YangInstanceIdentifier PATH_1 = YangInstanceIdentifier.builder()
            .node(QName.create("ns-1", "node-1"))
            .build();
    protected static final YangInstanceIdentifier PATH_2 = YangInstanceIdentifier.builder()
            .node(QName.create("ns-1", "node-2"))
            .build();
    protected static final YangInstanceIdentifier PATH_3 = YangInstanceIdentifier.builder()
            .node(QName.create("ns-1", "node-3"))
            .build();
    protected static final ContainerNode DATA_1 = Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(PATH_1.getLastPathArgument().getNodeType()))
            .build();
    protected static final ContainerNode DATA_2 = Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(PATH_2.getLastPathArgument().getNodeType()))
            .build();
    protected static final String PERSISTENCE_ID = "per-1";

    @Mock
    private DataTreeSnapshot snapshot;
    @Mock
    private AbstractClientHistory history;
    @Mock
    private DatastoreContext datastoreContext;
    @Mock
    private ActorUtils actorUtils;

    private ActorSystem system;
    private TestProbe backendProbe;
    private TestProbe clientContextProbe;
    private TransactionTester<T> tester;
    protected ClientActorContext context;
    protected T transaction;

    @Before
    public void setUp() {
        system = ActorSystem.apply();
        clientContextProbe = new TestProbe(system, "clientContext");
        backendProbe = new TestProbe(system, "backend");
        context = AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID,
                PERSISTENCE_ID);
        final ShardBackendInfo backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(),
                "default", UnsignedLong.ZERO, Optional.empty(), 3);
        final AbstractClientConnection<ShardBackendInfo> connection =
                AccessClientUtil.createConnectedConnection(context, 0L, backend);

        final ProxyHistory parent = ProxyHistory.createClient(history, connection, HISTORY_ID);
        transaction = createTransaction(parent, TestUtils.TRANSACTION_ID, snapshot);
        tester = new TransactionTester<>(transaction, connection, backendProbe);
    }

    protected final void mockForRemote() {
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(actorUtils).when(history).actorUtils();
    }

    @SuppressWarnings("checkstyle:hiddenField")
    protected abstract T createTransaction(ProxyHistory parent, TransactionIdentifier id, DataTreeSnapshot snapshot);

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public abstract void testExists() throws Exception;

    @Test
    public abstract void testRead() throws Exception;

    @Test
    public abstract void testWrite();

    @Test
    public abstract void testMerge();

    @Test
    public abstract void testDelete();

    @Test
    public abstract void testDirectCommit() throws Exception;

    @Test
    public abstract void testCanCommit();

    @Test
    public abstract void testPreCommit();

    @Test
    public abstract void testDoCommit();

    @Test
    public abstract void testForwardToRemoteAbort();

    @Test
    public abstract void testForwardToRemoteCommit();

    @Test
    public void testAbortVotingFuture() {
        testRequestResponse(f -> transaction.abort(f), TransactionAbortRequest.class, TransactionAbortSuccess::new);
    }

    @Test
    public void testForwardToRemotePurge() {
        final TestProbe probe = new TestProbe(system);
        final TransactionPurgeRequest request = new TransactionPurgeRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionPurgeRequest.class);
    }

    @Test
    public void testReplayMessages() {
        final TestProbe probe = new TestProbe(system);
        final List<ConnectionEntry> entries = new ArrayList<>();
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        final ReadTransactionRequest request1 =
                new ReadTransactionRequest(TRANSACTION_ID, 2L, probe.ref(), PATH_2, true);
        final ExistsTransactionRequest request2 =
                new ExistsTransactionRequest(TRANSACTION_ID, 3L, probe.ref(), PATH_3, true);
        entries.add(AccessClientUtil.createConnectionEntry(request1, callback, 0L));
        entries.add(AccessClientUtil.createConnectionEntry(request2, callback, 0L));
        final TransactionTester<RemoteProxyTransaction> successor = createRemoteProxyTransactionTester();
        final AbortLocalTransactionRequest successful1 = new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref());
        transaction.recordSuccessfulRequest(successful1);
        final ReadTransactionRequest successful2 =
                new ReadTransactionRequest(TRANSACTION_ID, 1L, probe.ref(), PATH_1, true);
        transaction.recordSuccessfulRequest(successful2);
        transaction.startReconnect();

        final ProxyHistory mockSuccessor = mock(ProxyHistory.class);
        when(mockSuccessor.createTransactionProxy(TRANSACTION_ID, transaction.isSnapshotOnly(), false))
            .thenReturn(successor.getTransaction());

        transaction.replayMessages(mockSuccessor, entries);

        final ModifyTransactionRequest transformed = successor.expectTransactionRequest(ModifyTransactionRequest.class);
        assertNotNull(transformed);
        assertEquals(successful1.getSequence(), transformed.getSequence());
        assertEquals(Optional.of(PersistenceProtocol.ABORT), transformed.getPersistenceProtocol());

        ReadTransactionRequest tmpRead = successor.expectTransactionRequest(ReadTransactionRequest.class);
        assertNotNull(tmpRead);
        assertEquals(successful2.getTarget(), tmpRead.getTarget());
        assertEquals(successful2.getSequence(), tmpRead.getSequence());
        assertEquals(successful2.getPath(), tmpRead.getPath());
        assertEquals(successor.localActor(), tmpRead.getReplyTo());

        tmpRead = successor.expectTransactionRequest(ReadTransactionRequest.class);
        assertNotNull(tmpRead);
        assertEquals(request1.getTarget(), tmpRead.getTarget());
        assertEquals(request1.getSequence(), tmpRead.getSequence());
        assertEquals(request1.getPath(), tmpRead.getPath());
        assertEquals(successor.localActor(), tmpRead.getReplyTo());

        final ExistsTransactionRequest tmpExist = successor.expectTransactionRequest(ExistsTransactionRequest.class);
        assertNotNull(tmpExist);
        assertEquals(request2.getTarget(), tmpExist.getTarget());
        assertEquals(request2.getSequence(), tmpExist.getSequence());
        assertEquals(request2.getPath(), tmpExist.getPath());
        assertEquals(successor.localActor(), tmpExist.getReplyTo());
    }

    protected void checkModifications(final ModifyTransactionRequest modifyRequest) {
        final List<TransactionModification> modifications = modifyRequest.getModifications();
        assertEquals(3, modifications.size());
        assertThat(modifications, hasItem(allOf(isA(TransactionWrite.class), hasPath(PATH_1))));
        assertThat(modifications, hasItem(allOf(isA(TransactionMerge.class), hasPath(PATH_2))));
        assertThat(modifications, hasItem(allOf(isA(TransactionDelete.class), hasPath(PATH_3))));
    }

    @SuppressWarnings("checkstyle:hiddenField")
    protected <R extends TransactionRequest<R>> void testRequestResponse(final Consumer<VotingFuture<Empty>> consumer,
            final Class<R> expectedRequest,
            final BiFunction<TransactionIdentifier, Long, TransactionSuccess<?>> replySupplier) {
        final TransactionTester<T> tester = getTester();
        final VotingFuture<Empty> future = mock(VotingFuture.class);
        transaction.seal();
        consumer.accept(future);
        final TransactionRequest<?> req = tester.expectTransactionRequest(expectedRequest);
        tester.replySuccess(replySupplier.apply(TRANSACTION_ID, req.getSequence()));
        verify(future).voteYes();
    }

    protected <R extends TransactionRequest<R>> R testHandleForwardedRemoteRequest(final R request) {
        transaction.handleReplayedRemoteRequest(request, createCallbackMock(), Ticker.systemTicker().read());
        final RequestEnvelope envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
        final R received = (R) envelope.getMessage();
        assertTrue(received.getClass().equals(request.getClass()));
        assertEquals(TRANSACTION_ID, received.getTarget());
        assertEquals(clientContextProbe.ref(), received.getReplyTo());
        return received;
    }

    protected <R extends TransactionRequest<R>> R testForwardToRemote(final TransactionRequest<?> toForward,
            final Class<R> expectedMessageClass) {
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        final TransactionTester<RemoteProxyTransaction> transactionTester = createRemoteProxyTransactionTester();
        final RemoteProxyTransaction successor = transactionTester.getTransaction();
        transaction.forwardToRemote(successor, toForward, callback);
        return transactionTester.expectTransactionRequest(expectedMessageClass);
    }

    protected TransactionTester<T> getTester() {
        return tester;
    }

    @SuppressWarnings("unchecked")
    protected static <T> Consumer<T> createCallbackMock() {
        return mock(Consumer.class);
    }

    protected static BaseMatcher<TransactionModification> hasPath(final YangInstanceIdentifier path) {
        return new BaseMatcher<>() {

            @Override
            public boolean matches(final Object item) {
                return path.equals(((TransactionModification) item).getPath());
            }

            @Override
            public void describeTo(final Description description) {
                description.appendValue(path);
            }

            @Override
            public void describeMismatch(final Object item, final Description description) {
                final TransactionModification modification = (TransactionModification) item;
                description.appendText("was ").appendValue(modification.getPath());
            }
        };
    }

    protected TestProbe createProbe() {
        return new TestProbe(system);
    }

    @SuppressWarnings("checkstyle:hiddenField")
    protected TransactionTester<LocalReadWriteProxyTransaction> createLocalProxy() {
        final TestProbe backendProbe = new TestProbe(system, "backend2");
        final TestProbe clientContextProbe = new TestProbe(system, "clientContext2");
        final ClientActorContext context =
                AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        final ShardBackendInfo backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(),
                "default", UnsignedLong.ZERO, Optional.empty(), 3);
        final AbstractClientConnection<ShardBackendInfo> connection =
                AccessClientUtil.createConnectedConnection(context, 0L, backend);
        final AbstractClientHistory history = mock(AbstractClientHistory.class);
        final ProxyHistory parent = ProxyHistory.createClient(history, connection, HISTORY_ID);
        final DataTreeSnapshot snapshot = mock(DataTreeSnapshot.class);
        when(snapshot.newModification()).thenReturn(mock(CursorAwareDataTreeModification.class));
        final LocalReadWriteProxyTransaction tx =
                new LocalReadWriteProxyTransaction(parent, TestUtils.TRANSACTION_ID, snapshot);
        return new TransactionTester<>(tx, connection, backendProbe);
    }

    @SuppressWarnings("checkstyle:hiddenField")
    protected TransactionTester<RemoteProxyTransaction> createRemoteProxyTransactionTester() {
        final TestProbe clientContextProbe = new TestProbe(system, "remoteClientContext");
        final TestProbe backendProbe = new TestProbe(system, "remoteBackend");
        final AbstractClientHistory history = mock(AbstractClientHistory.class);
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(actorUtils).when(history).actorUtils();

        final ClientActorContext context =
                AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        final ShardBackendInfo backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(),
                "default", UnsignedLong.ZERO, Optional.empty(), 5);
        final AbstractClientConnection<ShardBackendInfo> connection =
                AccessClientUtil.createConnectedConnection(context, 0L, backend);
        final ProxyHistory proxyHistory = ProxyHistory.createClient(history, connection, HISTORY_ID);

        final RemoteProxyTransaction transaction =
                new RemoteProxyTransaction(proxyHistory, TRANSACTION_ID, false, false, false);
        return new TransactionTester<>(transaction, connection, backendProbe);
    }
}
