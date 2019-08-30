/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.google.common.primitives.UnsignedLong;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Promise;
import scala.concurrent.impl.Promise.DefaultPromise;

public abstract class AbstractClientHistoryTest<T extends AbstractClientHistory> {
    protected static final String SHARD_NAME = "default";
    protected static final String PERSISTENCE_ID = "per-1";
    protected static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 1L);

    @Mock
    private DataTree tree;

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
        final AbstractProxyTransaction snapshotProxy = object().createSnapshotProxy(TRANSACTION_ID, 0L);
        Assert.assertNotNull(snapshotProxy);
        Assert.assertNotEquals(TRANSACTION_ID, snapshotProxy.getIdentifier());
    }

    @Test
    public void testCreateTransactionProxy() {
        AbstractProxyTransaction transactionProxy = object().createTransactionProxy(TRANSACTION_ID, 0L);
        Assert.assertNotNull(transactionProxy);
        Assert.assertNotEquals(TRANSACTION_ID, transactionProxy.getIdentifier());
    }

    @Test
    public void testState() {
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object().state());
    }

    @Test
    public void testUpdateState() {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.CLOSED);
        Assert.assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Test
    public void testDoClose() {
        object().createTransactionProxy(TRANSACTION_ID, 0L);
        object().doClose();
        Assert.assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Test
    public void testGetIdentifier() {
        Assert.assertEquals(HISTORY_ID, object().getIdentifier());
    }

    @Test
    public void testNextTx() {
        Assert.assertTrue(object().nextTx() + 1 == object().nextTx());
    }

    @Test
    public void testResolveShardForPath() {
        final Long shardForPath = object().resolveShardForPath(YangInstanceIdentifier.empty());
        Assert.assertEquals(0L, shardForPath.longValue());
    }

    @Test
    public void testLocalAbort() {
        object().localAbort(new Throwable());
        Assert.assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Test
    public void testOnProxyDestroyed() {
        final ProxyHistory proxyHistory = Mockito.mock(ProxyHistory.class);
        when(proxyHistory.getIdentifier()).thenReturn(HISTORY_ID);

        object().onProxyDestroyed(proxyHistory);
        verify(proxyHistory).getIdentifier();
    }

    @Test
    public void testCreateTransaction() {
        final ClientTransaction transaction = object().createTransaction();
        Assert.assertNotNull(transaction);
    }

    @Test
    public void testTakeSnapshot() {
        final ClientSnapshot clientSnapshot = object().takeSnapshot();
        Assert.assertEquals(object().getIdentifier(), clientSnapshot.getIdentifier().getHistoryId());
    }

    @Test
    public void testStartReconnect() {
        // cookie and shard are the same
        final Long cookie = 0L;
        final Long shard = cookie;

        final ShardBackendInfo info = new ShardBackendInfo(clientActorContext().self(), 0L, ABIVersion.current(),
                SHARD_NAME, UnsignedLong.ZERO, Optional.of(tree), 10);
        final ConnectedClientConnection<ShardBackendInfo> newConn = AccessClientUtil.createConnectedConnection(
                clientActorContext(), cookie, info);
        object().createSnapshotProxy(TRANSACTION_ID, shard);

        final HistoryReconnectCohort reconnectCohort = object().startReconnect(newConn);
        Assert.assertNotNull(reconnectCohort);
    }

    @Test
    public void testStartReconnectMissingOldProxy() {
        // cookie and shard are different
        final Long cookie = 1L;
        final Long shard = 0L;

        final ShardBackendInfo info = new ShardBackendInfo(clientActorContext().self(), 0L, ABIVersion.current(),
                SHARD_NAME, UnsignedLong.ZERO, Optional.of(tree), 10);
        final ConnectedClientConnection<ShardBackendInfo> newConn = AccessClientUtil.createConnectedConnection(
                clientActorContext(), cookie, info);
        object().createSnapshotProxy(TRANSACTION_ID, shard);

        final HistoryReconnectCohort reconnectCohort = object().startReconnect(newConn);
        Assert.assertNull(reconnectCohort);
    }

    protected static ActorUtils createActorUtilsMock(final ActorSystem system, final ActorRef actor) {
        final ActorUtils mock = mock(ActorUtils.class);
        final Promise<PrimaryShardInfo> promise = new DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        when(mock.findPrimaryShardAsync(any())).thenReturn(promise.future());
        return mock;
    }
}