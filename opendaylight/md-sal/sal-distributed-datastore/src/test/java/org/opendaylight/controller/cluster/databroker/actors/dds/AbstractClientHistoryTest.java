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
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Promise;

public abstract class AbstractClientHistoryTest<T extends AbstractClientHistory> {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("type-1");
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    private static final String SHARD_NAME = "default";
    private static final String PERSISTENCE_ID = "per-1";
    private static final ActorSystem SYSTEM = ActorSystem.apply();
    private static final TestProbe CLIENT_CONTEXT_PROBE = new TestProbe(SYSTEM, "client");
    private static final TestProbe ACTOR_CONTEXT_PROBE = new TestProbe(SYSTEM, "actor-context");
    private static final ClientActorContext CLIENT_ACTOR_CONTEXT = AccessClientUtil.createClientActorContext(SYSTEM, CLIENT_CONTEXT_PROBE.ref(), CLIENT_ID, PERSISTENCE_ID);
    private static final ActorContext ACTOR_CONTEXT = createActorContextMock(SYSTEM, ACTOR_CONTEXT_PROBE.ref());

    protected static final AbstractDataStoreClientBehavior CLIENT_BEHAVIOUR = new SimpleDataStoreClientBehavior(
            CLIENT_ACTOR_CONTEXT, ACTOR_CONTEXT, SHARD_NAME);
    protected static final LocalHistoryIdentifier LOCAL_HISTORY_IDENTIFIER = new LocalHistoryIdentifier(CLIENT_ID, 1L);

    @Mock
    private ActorSystem system;
    @Mock
    private ActorRef actorRef;
    @Mock
    private DataTree dataTree;
    @Mock
    private ProxyHistory proxyHistory;

    protected abstract T object();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testCreateSnapshotProxy() throws Exception {
        Assert.assertNotNull(object().createSnapshotProxy(TRANSACTION_ID, 0L));
    }

    @Test
    public void testCreateTransactionProxy() throws Exception {
        Assert.assertNotNull(object().createTransactionProxy(TRANSACTION_ID, 0L));
    }

    @Test
    public void testState() throws Exception {
        resetIdleState(object());
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object().state());
    }

    @Test
    public void testUpdateState() throws Exception {
        resetIdleState(object());
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.IDLE);
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object().state());
    }

    @Test
    public void testDoClose() throws Exception {
        object().createTransactionProxy(TRANSACTION_ID, 0L);
        resetIdleState(object());
        object().doClose();
    }

    @Test
    public void testGetIdentifier() throws Exception {
        Assert.assertEquals(LOCAL_HISTORY_IDENTIFIER, object().getIdentifier());
    }

    @Test
    public void testNextTx() throws Exception {
        Assert.assertNotNull(object().nextTx());
    }

    @Test
    public void testResolveShardForPath() throws Exception {
        final Long shardForPath = object().resolveShardForPath(YangInstanceIdentifier.EMPTY);
        Assert.assertEquals(0L, shardForPath.longValue());
    }

    @Test
    public void testLocalAbort() throws Exception {
        object().localAbort(new Throwable());
        Assert.assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Test
    public abstract void testCreateHistoryProxy() throws Exception;

    @Test
    public void testOnProxyDestroyed() throws Exception {
        final ProxyHistory proxyHistory =
                object().createHistoryProxy(LOCAL_HISTORY_IDENTIFIER, CLIENT_BEHAVIOUR.getConnection(0L));
        object().onProxyDestroyed(proxyHistory);
    }

    @Test
    public void testCreateTransaction() throws Exception {
        resetIdleState(object());
        final ClientTransaction transaction = object().createTransaction();
        Assert.assertNotNull(transaction);
    }

    @Test
    public void testTakeSnapshot() throws Exception {
        resetIdleState(object());
        final ClientSnapshot clientSnapshot = object().takeSnapshot();
        Assert.assertEquals(object().getIdentifier(), clientSnapshot.getIdentifier().getHistoryId());
    }

    @Test
    public abstract void testDoCreateSnapshot() throws Exception;

    @Test
    public abstract void testDoCreateTransaction() throws Exception;

    @Test
    public void testOnTransactionAbort() throws Exception {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        clientSnapshot.abort();
        object().onTransactionAbort(clientSnapshot);
    }

    @Test
    public void testOnTransactionReadyAndComplete() throws Exception {
        final ClientTransaction clientTransaction = new ClientTransaction(object(), TRANSACTION_ID);
        final AbstractTransactionCommitCohort abstractTransactionCommitCohort =
                mock(AbstractTransactionCommitCohort.class);
        object().onTransactionReady(clientTransaction, abstractTransactionCommitCohort);
        object().onTransactionComplete(TRANSACTION_ID);
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

    void resetIdleState(final AbstractClientHistory clientHistory) {
        switch (clientHistory.state()) {
            case IDLE:
                break;
            case TX_OPEN:
                clientHistory.updateState(AbstractClientHistory.State.TX_OPEN, AbstractClientHistory.State.IDLE);
                break;
            case CLOSED:
                clientHistory.updateState(AbstractClientHistory.State.CLOSED, AbstractClientHistory.State.IDLE);
                break;
            default:
                break;
        }
    }

}