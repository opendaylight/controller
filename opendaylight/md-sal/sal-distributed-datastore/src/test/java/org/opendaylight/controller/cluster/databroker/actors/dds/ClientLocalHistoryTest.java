/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.primitives.UnsignedLong;
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
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ClientLocalHistoryTest extends AbstractClientHistoryTest<ClientLocalHistory> {
    private ActorSystem system;
    private AbstractDataStoreClientBehavior behavior;
    private ClientActorContext clientActorContext;
    private ClientLocalHistory object;

    @Mock
    private AbstractTransactionCommitCohort cohort;
    @Mock
    private ClientTransaction transaction;
    @Mock
    private ReadOnlyDataTree dataTree;
    @Mock
    private DataTreeSnapshot firstSnapshot;
    @Mock
    private DataTreeSnapshot secondSnapshot;
    @Mock
    private CursorAwareDataTreeModification modification;

    @Before
    public void setUp() {
        system = ActorSystem.apply();

        final TestProbe clientContextProbe = new TestProbe(system, "client");
        final TestProbe actorContextProbe = new TestProbe(system, "actor-context");
        clientActorContext = AccessClientUtil.createClientActorContext(
                system, clientContextProbe.ref(), TestUtils.CLIENT_ID, PERSISTENCE_ID);
        final ActorUtils actorUtilsMock = createActorUtilsMock(system, actorContextProbe.ref());
        behavior = new SimpleDataStoreClientBehavior(clientActorContext, actorUtilsMock, SHARD_NAME);

        object = new ClientLocalHistory(behavior, HISTORY_ID);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Override
    protected ClientLocalHistory object() {
        return object;
    }

    @Override
    protected ClientActorContext clientActorContext() {
        return clientActorContext;
    }

    @Test
    public void testClose() {
        object().close();
        assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Override
    @Test
    public void testDoCreateTransaction() {
        final ClientTransaction clientTransaction = object().doCreateTransaction();
        assertEquals(object().getIdentifier(), clientTransaction.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionAbort() {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        assertTrue(clientSnapshot.abort());
    }

    @Override
    @Test
    public void testCreateHistoryProxy() {
        final AbstractClientConnection<ShardBackendInfo> clientConnection = behavior.getConnection(0L);
        final ProxyHistory historyProxy = object().createHistoryProxy(HISTORY_ID, clientConnection);
        assertEquals(object().getIdentifier(), historyProxy.getIdentifier());
    }

    @Override
    @Test
    public void testDoCreateSnapshot() {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        assertEquals(new TransactionIdentifier(object().getIdentifier(), object().nextTx()).getHistoryId(),
                clientSnapshot.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionComplete() {
        final ClientTransaction tx = object().createTransaction();

        // make transaction ready
        object().onTransactionReady(tx, cohort);
        // state should be set to IDLE
        assertEquals(AbstractClientHistory.State.IDLE, object.state());

        // complete transaction
        object().onTransactionComplete(tx.getIdentifier());
        // state is still IDLE
        assertEquals(AbstractClientHistory.State.IDLE, object.state());
    }

    @Override
    @Test
    public void testOnTransactionReady() {
        final AbstractTransactionCommitCohort result = object().onTransactionReady(object().createTransaction(),
            cohort);
        assertEquals(result, cohort);
    }

    @Override
    @Test
    public void testOnTransactionReadyDuplicate() {
        final ClientTransaction tx = object().createTransaction();
        object().onTransactionReady(tx, cohort);
        final IllegalStateException ise = assertThrows(IllegalStateException.class,
            () -> object().onTransactionReady(tx, cohort));
        assertThat(ise.getMessage(), containsString(" is idle when readying transaction "));
    }

    @Test
    public void testOnTransactionReadyAndComplete() {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.TX_OPEN);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                object().onTransactionReady(transaction, cohort);
        assertEquals(cohort, transactionCommitCohort);
    }

    @Test
    public void testOnTransactionReadyAndCompleteStateClosed() {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.CLOSED);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                object().onTransactionReady(transaction, cohort);
        assertEquals(cohort, transactionCommitCohort);
    }

    @Test
    public void testOnTransactionReadyAndCompleteIdleFail() {
        final IllegalStateException ise = assertThrows(IllegalStateException.class,
            () -> object().onTransactionReady(transaction, cohort));
        assertThat(ise.getMessage(), endsWith(" is idle when readying transaction null"));
    }

    /**
     * Once a transaction committed through directCommit() completes, the local history must release it so the next
     * transaction on the chain is built on a fresh data-tree snapshot instead of reusing the committed
     * transaction's.
     */
    @Test
    public void nextTransactionRebasesOnFreshSnapshotAfterCommit() {
        // takeSnapshot() hands the first snapshot to the first transaction, the second to the next one.
        when(dataTree.takeSnapshot()).thenReturn(firstSnapshot, secondSnapshot);
        when(firstSnapshot.newModification()).thenReturn(modification);

        // A backend reporting a local data tree makes createClient() build a Local (co-located) history.
        final var backendProbe = new TestProbe(system, "backend");
        final var backend = new ShardBackendInfo(backendProbe.ref(), 0L, ABIVersion.current(), SHARD_NAME,
            UnsignedLong.ZERO, Optional.of(dataTree), 3);
        final var connection = AccessClientUtil.<ShardBackendInfo>createConnectedConnection(clientActorContext, 0L,
            backend);
        final var localHistory = ProxyHistory.createClient(object(), connection, HISTORY_ID);

        // Commit a read-write transaction on the chain through directCommit().
        final var tx1 = assertInstanceOf(LocalReadWriteProxyTransaction.class,
            localHistory.createTransactionProxy(new TransactionIdentifier(HISTORY_ID, 0L), false));
        tx1.seal();
        tx1.directCommit();

        final var tester = new TransactionTester<>(tx1, connection, backendProbe);
        final var request = tester.expectTransactionRequest(CommitLocalTransactionRequest.class);
        tester.replySuccess(new TransactionCommitSuccess(request.getTarget(), request.getSequence()));

        // Once the committed transaction is released, the next transaction must be built on a fresh data-tree
        // snapshot rather than reusing the committed transaction's snapshot.
        final var tx2 = assertInstanceOf(LocalReadOnlyProxyTransaction.class,
            localHistory.createTransactionProxy(new TransactionIdentifier(HISTORY_ID, 1L), true));
        assertSame(secondSnapshot, tx2.readOnlyView());
        verify(dataTree, times(2)).takeSnapshot();
    }
}
