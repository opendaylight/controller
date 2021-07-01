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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

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
}
