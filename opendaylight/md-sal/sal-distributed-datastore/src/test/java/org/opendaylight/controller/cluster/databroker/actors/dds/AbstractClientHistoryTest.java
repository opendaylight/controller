/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import scala.concurrent.Promise;

public abstract class AbstractClientHistoryTest<T extends  AbstractClientHistory> {

    private static final String SHARD_NAME = "default";
    private static final String PERSISTENCE_ID = "per-1";
    private static final ActorSystem SYSTEM = ActorSystem.apply();
    private static final TestProbe CLIENT_CONTEXT_PROBE = new TestProbe(SYSTEM, "client");
    private static final TestProbe ACTOR_CONTEXT_PROBE = new TestProbe(SYSTEM, "actor-context");
    private static final ClientActorContext CLIENT_ACTOR_CONTEXT = AccessClientUtil.createClientActorContext(SYSTEM, CLIENT_CONTEXT_PROBE.ref(), CLIENT_ID, PERSISTENCE_ID);
    private static final ActorContext ACTOR_CONTEXT = createActorContextMock(SYSTEM, ACTOR_CONTEXT_PROBE.ref());

    protected static final  AbstractDataStoreClientBehavior CLIENT_BEHAVIOUR = new SimpleDataStoreClientBehavior(
            CLIENT_ACTOR_CONTEXT, ACTOR_CONTEXT, SHARD_NAME);
    protected static final  LocalHistoryIdentifier LOCAL_HISTORY_IDENTIFIER = new LocalHistoryIdentifier(CLIENT_ID, 1L);

    protected abstract T object();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCreateSnapshotProxy() throws Exception {
        object().createSnapshotProxy(TRANSACTION_ID, 0L);
    }

    @Test
    public void testCreateTransactionProxy() throws Exception {
        object().createTransactionProxy(TRANSACTION_ID, 0L);
    }

    @Test
    public void testState() throws Exception {
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object().state());
    }

    @Test
    public void testUpdateState() throws Exception {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.IDLE);
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object().state());
    }

    @Test
    public void testDoClose() throws Exception {
        if(object().state().equals(AbstractClientHistory.State.TX_OPEN)){
            object().updateState(AbstractClientHistory.State.TX_OPEN, AbstractClientHistory.State.CLOSED);
        }
        object().doClose();
    }

    @Test
    public void testOnProxyDestroyed() throws Exception {
        //object().onProxyDestroyed();
    }

    @Test
    public void testGetIdentifier() throws Exception {
        Assert.assertEquals(LOCAL_HISTORY_IDENTIFIER, object().getIdentifier());
    }

    @Test
    public void testNextTx() throws Exception {
        object().nextTx();
    }

    @Test
    public void testResolveShardForPath() throws Exception {
        object().resolveShardForPath(YangInstanceIdentifier.EMPTY);
    }

    @Test
    public void testLocalAbort() throws Exception {
        object().localAbort(new Throwable());
    }

    @Test
    public void testCreateHistoryProxy() throws Exception {
        object().createHistoryProxy(LOCAL_HISTORY_IDENTIFIER, CLIENT_BEHAVIOUR.getConnection(0L));
    }

    @Test
    public void testCreateTransaction() throws Exception {
        if(object().state().equals(AbstractClientHistory.State.CLOSED)){
        object().updateState(AbstractClientHistory.State.CLOSED, AbstractClientHistory.State.IDLE);}
        object().createTransaction();
    }

    @Test
    public void testTakeSnapshot() throws Exception {
        object().takeSnapshot();
    }

    @Test
    public void testDoCreateSnapshot() throws Exception {
        object().doCreateSnapshot();
    }

    @Test
    public void testDoCreateTransaction() throws Exception {
        object().doCreateTransaction();
    }

    @Test
    public void testOnTransactionReady() throws Exception {
        object().onTransactionReady();
    }

    @Test
    public void testOnTransactionAbort() throws Exception {
        object().onTransactionAbort();
    }

    @Test
    public void testOnTransactionComplete() throws Exception {
        object().onTransactionComplete();
    }

    @Test
    public void testStartReconnect() throws Exception {
        object().startReconnect();
    }

    private static ActorContext createActorContextMock(final ActorSystem system, final ActorRef actor) {
        final ActorContext mock = mock(ActorContext.class);
        final Promise<PrimaryShardInfo> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        when(mock.findPrimaryShardAsync(any())).thenReturn(promise.future());
        return mock;
    }

}