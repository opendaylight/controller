/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Ticker;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opendaylight.controller.cluster.access.ABIVersion;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;

@ExtendWith(MockitoExtension.class)
// FIXME:
@MockitoSettings(strictness = Strictness.LENIENT)
abstract class AbstractProxyTransactionTest<T extends AbstractProxyTransaction> {
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
    protected static final ContainerNode DATA_1 = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(PATH_1.getLastPathArgument().getNodeType()))
            .build();
    protected static final ContainerNode DATA_2 = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(PATH_2.getLastPathArgument().getNodeType()))
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

    ClientActorContext context;
    T transaction;

    @BeforeEach
    final void beforeEach() {
        system = ActorSystem.create();
        clientContextProbe = new TestProbe(system, "clientContext");
        backendProbe = new TestProbe(system, "backend");
        context = AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID,
                PERSISTENCE_ID);
        final var backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(), "default",
            UnsignedLong.ZERO, Optional.empty(), 3);
        final var connection = AccessClientUtil.createConnectedConnection(context, 0L, backend);
        final var parent = ProxyHistory.createClient(history, connection, HISTORY_ID);
        transaction = createTransaction(parent, TestUtils.TRANSACTION_ID, snapshot);
        tester = new TransactionTester<>(transaction, connection, backendProbe);
    }

    final void mockForRemote() {
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(actorUtils).when(history).actorUtils();
    }

    @SuppressWarnings("checkstyle:hiddenField")
    abstract T createTransaction(ProxyHistory parent, TransactionIdentifier id, DataTreeSnapshot snapshot);

    @AfterEach
    final void afterEach() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    abstract void testExists() throws Exception;

    @Test
    abstract void testRead() throws Exception;

    @Test
    abstract void testWrite();

    @Test
    abstract void testMerge();

    @Test
    abstract void testDelete();

    @Test
    abstract void testDirectCommit() throws Exception;

    @Test
    abstract void testCanCommit();

    @Test
    abstract void testPreCommit();

    @Test
    abstract void testDoCommit();

    @Test
    abstract void testForwardToRemoteAbort();

    @Test
    abstract void testForwardToRemoteCommit();

    @Test
    void testAbortVotingFuture() {
        testRequestResponse(f -> transaction.abort(f), TransactionAbortRequest.class, TransactionAbortSuccess::new);
    }

    @Test
    void testForwardToRemotePurge() {
        final var probe = new TestProbe(system);
        final var request = new TransactionPurgeRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionPurgeRequest.class);
    }

    @Test
    void testReplayMessages() {
        final var probe = new TestProbe(system);
        final var entries = new ArrayList<ConnectionEntry>();
        final Consumer<Response<?, ?>> callback = mock();
        final var request1 = new ReadTransactionRequest(TRANSACTION_ID, 2L, probe.ref(), PATH_2, true);
        final var request2 = new ExistsTransactionRequest(TRANSACTION_ID, 3L, probe.ref(), PATH_3, true);
        entries.add(AccessClientUtil.createConnectionEntry(request1, callback, 0L));
        entries.add(AccessClientUtil.createConnectionEntry(request2, callback, 0L));
        final var successor = createRemoteProxyTransactionTester();
        final var successful1 = new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref());
        transaction.recordSuccessfulRequest(successful1);
        final var successful2 = new ReadTransactionRequest(TRANSACTION_ID, 1L, probe.ref(), PATH_1, true);
        transaction.recordSuccessfulRequest(successful2);
        transaction.startReconnect();

        final var mockSuccessor = mock(ProxyHistory.class);
        when(mockSuccessor.createTransactionProxy(TRANSACTION_ID, transaction.isSnapshotOnly(), false))
            .thenReturn(successor.getTransaction());

        transaction.replayMessages(mockSuccessor, entries);

        final var transformed = successor.expectTransactionRequest(ModifyTransactionRequest.class);
        assertNotNull(transformed);
        assertEquals(successful1.getSequence(), transformed.getSequence());
        assertEquals(Optional.of(PersistenceProtocol.ABORT), transformed.getPersistenceProtocol());

        var tmpRead = successor.expectTransactionRequest(ReadTransactionRequest.class);
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

        final var tmpExist = successor.expectTransactionRequest(ExistsTransactionRequest.class);
        assertNotNull(tmpExist);
        assertEquals(request2.getTarget(), tmpExist.getTarget());
        assertEquals(request2.getSequence(), tmpExist.getSequence());
        assertEquals(request2.getPath(), tmpExist.getPath());
        assertEquals(successor.localActor(), tmpExist.getReplyTo());
    }

    static final void checkModifications(final ModifyTransactionRequest modifyRequest) {
        final var modifications = assertThat(modifyRequest.getModifications()).hasSize(3).actual();
        assertEquals(PATH_1, assertInstanceOf(TransactionWrite.class, modifications.get(0)).getPath());
        assertEquals(PATH_2, assertInstanceOf(TransactionMerge.class, modifications.get(1)).getPath());
        assertEquals(PATH_3, assertInstanceOf(TransactionDelete.class, modifications.get(2)).getPath());
    }

    @SuppressWarnings("checkstyle:hiddenField")
    final <R extends TransactionRequest<R>> void testRequestResponse(final Consumer<VotingFuture<Empty>> consumer,
            final Class<R> expectedRequest,
            final BiFunction<TransactionIdentifier, Long, TransactionSuccess<?>> replySupplier) {
        final var tester = getTester();
        final VotingFuture<Empty> future = mock();
        transaction.seal();
        consumer.accept(future);
        final var req = tester.expectTransactionRequest(expectedRequest);
        tester.replySuccess(replySupplier.apply(TRANSACTION_ID, req.getSequence()));
        verify(future).voteYes();
    }

    final <R extends TransactionRequest<R>> R testHandleForwardedRemoteRequest(final R request) {
        transaction.handleReplayedRemoteRequest(request, mock(), Ticker.systemTicker().read());
        final var envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
        final var received = (R) envelope.getMessage();
        assertSame(received.getClass(), request.getClass());
        assertEquals(TRANSACTION_ID, received.getTarget());
        assertEquals(clientContextProbe.ref(), received.getReplyTo());
        return received;
    }

    final <R extends TransactionRequest<R>> R testForwardToRemote(final TransactionRequest<?> toForward,
            final Class<R> expectedMessageClass) {
        final Consumer<Response<?, ?>> callback = mock();
        final var transactionTester = createRemoteProxyTransactionTester();
        final var successor = transactionTester.getTransaction();
        transaction.forwardToRemote(successor, toForward, callback);
        return transactionTester.expectTransactionRequest(expectedMessageClass);
    }

    final TransactionTester<T> getTester() {
        return tester;
    }

    final TestProbe createProbe() {
        return new TestProbe(system);
    }

    @SuppressWarnings("checkstyle:hiddenField")
    final TransactionTester<LocalReadWriteProxyTransaction> createLocalProxy() {
        final var backendProbe = new TestProbe(system, "backend2");
        final var clientContextProbe = new TestProbe(system, "clientContext2");
        final var context =
                AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        final var backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(),
                "default", UnsignedLong.ZERO, Optional.empty(), 3);
        final var connection = AccessClientUtil.createConnectedConnection(context, 0L, backend);
        final var history = mock(AbstractClientHistory.class);
        final var parent = ProxyHistory.createClient(history, connection, HISTORY_ID);
        final var snapshot = mock(DataTreeSnapshot.class);
        when(snapshot.newModification()).thenReturn(mock(CursorAwareDataTreeModification.class));
        final var tx = new LocalReadWriteProxyTransaction(parent, TestUtils.TRANSACTION_ID, snapshot);
        return new TransactionTester<>(tx, connection, backendProbe);
    }

    @SuppressWarnings("checkstyle:hiddenField")
    final TransactionTester<RemoteProxyTransaction> createRemoteProxyTransactionTester() {
        final var clientContextProbe = new TestProbe(system, "remoteClientContext");
        final var backendProbe = new TestProbe(system, "remoteBackend");
        final var history = mock(AbstractClientHistory.class);
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(actorUtils).when(history).actorUtils();

        final var context =
            AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        final var backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(), "default",
            UnsignedLong.ZERO, Optional.empty(), 5);
        final var connection = AccessClientUtil.createConnectedConnection(context, 0L, backend);
        final var proxyHistory = ProxyHistory.createClient(history, connection, HISTORY_ID);

        final var transaction = new RemoteProxyTransaction(proxyHistory, TRANSACTION_ID, false, false, false);
        return new TransactionTester<>(transaction, connection, backendProbe);
    }
}
