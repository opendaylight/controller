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
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
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
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

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
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        system = ActorSystem.apply();

        final TestProbe clientContextProbe = new TestProbe(system, "client");
        final TestProbe actorContextProbe = new TestProbe(system, "actor-context");
        clientActorContext = AccessClientUtil.createClientActorContext(
                system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
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
        Assert.assertEquals(AbstractClientHistory.State.CLOSED, object().state());
    }

    @Override
    @Test
    public void testDoCreateTransaction() {
        final ClientTransaction clientTransaction = object().doCreateTransaction();
        Assert.assertEquals(object().getIdentifier(), clientTransaction.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionAbort() {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        Assert.assertTrue(clientSnapshot.abort());
    }

    @Override
    @Test
    public void testCreateHistoryProxy() {
        final AbstractClientConnection<ShardBackendInfo> clientConnection = behavior.getConnection(0L);
        final ProxyHistory historyProxy = object().createHistoryProxy(HISTORY_ID, clientConnection);
        Assert.assertEquals(object().getIdentifier(), historyProxy.getIdentifier());
    }

    @Override
    @Test
    public void testDoCreateSnapshot() {
        final ClientSnapshot clientSnapshot = object().doCreateSnapshot();
        Assert.assertEquals(new TransactionIdentifier(object().getIdentifier(), object().nextTx()).getHistoryId(),
                clientSnapshot.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionComplete() {
        final ClientTransaction tx = object().createTransaction();

        // make transaction ready
        object().onTransactionReady(tx, cohort);
        // state should be set to IDLE
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object.state());

        // complete transaction
        object().onTransactionComplete(tx.getIdentifier());
        // state is still IDLE
        Assert.assertEquals(AbstractClientHistory.State.IDLE, object.state());
    }

    @Override
    @Test
    public void testOnTransactionReady() {
        final AbstractTransactionCommitCohort result = object().onTransactionReady(
                object().createTransaction(), cohort);
        Assert.assertEquals(result, cohort);
    }

    @Override
    @Test(expected = IllegalStateException.class)
    public void testOnTransactionReadyDuplicate() {
        final ClientTransaction tx = object().createTransaction();
        object().onTransactionReady(tx, cohort);
        object().onTransactionReady(tx, cohort);
    }

    @Test
    public void testOnTransactionReadyAndComplete() {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.TX_OPEN);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                object().onTransactionReady(transaction, cohort);
        Assert.assertEquals(cohort, transactionCommitCohort);
    }

    @Test
    public void testOnTransactionReadyAndCompleteStateClosed() {
        object().updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.CLOSED);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                object().onTransactionReady(transaction, cohort);
        Assert.assertEquals(cohort, transactionCommitCohort);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnTransactionReadyAndCompleteIdleFail() {
        object().onTransactionReady(transaction, cohort);
    }
}
