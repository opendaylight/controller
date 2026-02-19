/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.primitives.UnsignedLong;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import scala.concurrent.Future;

public abstract class AbstractClientHistoryTest<T extends AbstractClientHistory> {
    protected static final String SHARD_NAME = "default";
    protected static final String PERSISTENCE_ID = "per-1";
    protected static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(TestUtils.CLIENT_ID, 1L);

    @Mock
    private DataTree tree;
    @Mock
    private DatastoreContext datastoreContext;

    protected abstract T object();

    protected abstract ClientActorContext clientActorContext();

    @Test
    public abstract void testDoCreateSnapshot();

    @Test
    public abstract void testDoCreateTransaction();

    @Test
    public abstract void testCreateHistoryProxy();

    @Test
    public abstract void testOnTransactionComplete();

    @Test
    public abstract void testOnTransactionAbort();

    @Test
    public abstract void testOnTransactionReady();

    @Test
    public abstract void testOnTransactionReadyDuplicate();

    @Test
    public void testCreateSnapshotProxy() {
        final var snapshotProxy = object().createSnapshotProxy(TestUtils.TRANSACTION_ID, 0L);
        assertNotNull(snapshotProxy);
        assertNotEquals(TestUtils.TRANSACTION_ID, snapshotProxy.getIdentifier());
    }

    @Test
    public void testCreateTransactionProxy() {
        final var transactionProxy = object().createTransactionProxy(TestUtils.TRANSACTION_ID, 0L);
        assertNotNull(transactionProxy);
        assertNotEquals(TestUtils.TRANSACTION_ID, transactionProxy.getIdentifier());
    }

    @Test
    public void testState() {
        assertEquals(AbstractClientHistory.State.IDLE, object().state());
    }

    @Test
    public void testUpdateState() {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.CLOSED);
        assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Test
    public void testDoClose() {
        object().createTransactionProxy(TestUtils.TRANSACTION_ID, 0L);
        object().doClose();
        assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Test
    public void testGetIdentifier() {
        assertEquals(HISTORY_ID, object().getIdentifier());
    }

    @Test
    public void testNextTx() {
        assertEquals(object().nextTx() + 1, object().nextTx());
    }

    @Test
    public void testResolveShardForPath() {
        final Long shardForPath = object().resolveShardForPath(YangInstanceIdentifier.of());
        assertNotNull(shardForPath);
        assertEquals(0L, (long) shardForPath);
    }

    @Test
    public void testLocalAbort() {
        object().localAbort(new Throwable());
        assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Test
    public void testOnProxyDestroyed() {
        final ProxyHistory proxyHistory = mock(ProxyHistory.class);
        doReturn(HISTORY_ID).when(proxyHistory).getIdentifier();

        object().onProxyDestroyed(proxyHistory);
        verify(proxyHistory).getIdentifier();
    }

    @Test
    public void testCreateTransaction() {
        final ClientTransaction transaction = object().createTransaction();
        assertNotNull(transaction);
    }

    @Test
    public void testTakeSnapshot() {
        final ClientSnapshot clientSnapshot = object().takeSnapshot();
        assertEquals(object().getIdentifier(), clientSnapshot.getIdentifier().getHistoryId());
    }

    @Test
    public void testStartReconnect() {
        // cookie and shard are the same
        final Long cookie = 0L;
        final Long shard = cookie;

        final var info = new ShardBackendInfo(clientActorContext().self(), 0L, ABIVersion.current(),
                SHARD_NAME, UnsignedLong.ZERO, Optional.of(tree), 10);
        final var newConn = AccessClientUtil.createConnectedConnection(clientActorContext(), cookie, info);
        object().createSnapshotProxy(TestUtils.TRANSACTION_ID, shard);

        final var reconnectCohort = object().startReconnect(newConn);
        assertNotNull(reconnectCohort);
    }

    @Test
    public void testStartReconnectMissingOldProxy() {
        // cookie and shard are different
        final Long cookie = 1L;
        final Long shard = 0L;

        final var info = new ShardBackendInfo(clientActorContext().self(), 0L, ABIVersion.current(),
                SHARD_NAME, UnsignedLong.ZERO, Optional.of(tree), 10);
        final var newConn = AccessClientUtil.createConnectedConnection(clientActorContext(), cookie, info);
        object().createSnapshotProxy(TestUtils.TRANSACTION_ID, shard);

        final HistoryReconnectCohort reconnectCohort = object().startReconnect(newConn);
        assertNull(reconnectCohort);
    }

    protected final ActorUtils createActorUtilsMock(final ActorSystem system, final ActorRef actor) {
        final var actorUtils = mock(ActorUtils.class);
        doReturn(Future.successful(new PrimaryShardInfo(system.actorSelection(actor.path()), (short) 0)))
            .when(actorUtils).findPrimaryShardAsync(any());
        doReturn(1000).when(datastoreContext).getShardBatchedModificationCount();
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(ExecutionContexts.global()).when(actorUtils).getClientDispatcher();
        return actorUtils;
    }
}
