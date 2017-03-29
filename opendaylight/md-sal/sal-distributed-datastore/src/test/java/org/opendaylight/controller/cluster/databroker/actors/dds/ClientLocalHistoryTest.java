/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

public class ClientLocalHistoryTest extends AbstractClientHistoryTest<ClientLocalHistory> {
    private ActorSystem system;
    private AbstractDataStoreClientBehavior behavior;
    private ClientActorContext clientActorContext;
    private ClientLocalHistory object;

    @Mock
    private AbstractTransactionCommitCohort cohort;
    @Mock
    private ClientTransaction transaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        system = ActorSystem.apply();

        final TestProbe clientContextProbe = new TestProbe(system, "client");
        final TestProbe actorContextProbe = new TestProbe(system, "actor-context");
        clientActorContext = AccessClientUtil.createClientActorContext(
                system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        final ActorContext actorContextMock = createActorContextMock(system, actorContextProbe.ref());
        behavior = new SimpleDataStoreClientBehavior(clientActorContext, actorContextMock, SHARD_NAME);

        object = new ClientLocalHistory(behavior, HISTORY_ID);
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
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
    public void testClose() throws Exception {
        object().close();
        Assert.assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Override
    @Test
    public void testDoCreateTransaction() throws Exception {
        final ClientTransaction clientTransaction = object().doCreateTransaction();
        Assert.assertEquals(object().getIdentifier(), clientTransaction.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionAbort() throws Exception {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        Assert.assertTrue(clientSnapshot.abort());
    }

    @Override
    @Test
    public void testCreateHistoryProxy() throws Exception {
        final AbstractClientConnection<ShardBackendInfo> clientConnection = behavior.getConnection(0L);
        final ProxyHistory historyProxy = object().createHistoryProxy(HISTORY_ID, clientConnection);
        Assert.assertEquals(object().getIdentifier(), historyProxy.getIdentifier());
    }

    @Override
    @Test
    public void testDoCreateSnapshot() throws Exception {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        Assert.assertEquals(new TransactionIdentifier(object().getIdentifier(), object().nextTx()).getHistoryId(),
                clientSnapshot.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionComplete() throws Exception {
        final ClientTransaction transaction = object().createTransaction();

        // make transaction ready
        object().onTransactionReady(transaction, cohort);
        // state should be set to IDLE
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object.state());

        // complete transaction
        object().onTransactionComplete(transaction.getIdentifier());
        // state is still IDLE
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object.state());
    }

    @Override
    @Test
    public void testOnTransactionReady() throws Exception {
        final AbstractTransactionCommitCohort result = object().onTransactionReady(
                object().createTransaction(), cohort);
        Assert.assertEquals(result, cohort);
    }

    @Override
    @Test(expected = IllegalStateException.class)
    public void testOnTransactionReadyDuplicate() throws Exception {
        final ClientTransaction transaction = object().createTransaction();
        object().onTransactionReady(transaction, cohort);
        object().onTransactionReady(transaction, cohort);
    }

    @Test
    public void testOnTransactionReadyAndComplete() throws Exception {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.TX_OPEN);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                object().onTransactionReady(transaction, cohort);
        Assert.assertEquals(cohort, transactionCommitCohort);
    }

    @Test
    public void testOnTransactionReadyAndCompleteStateClosed() throws Exception {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.CLOSED);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                object().onTransactionReady(transaction, cohort);
        Assert.assertEquals(cohort, transactionCommitCohort);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnTransactionReadyAndCompleteIdleFail() throws Exception {
        object().onTransactionReady(transaction, cohort);
    }
}