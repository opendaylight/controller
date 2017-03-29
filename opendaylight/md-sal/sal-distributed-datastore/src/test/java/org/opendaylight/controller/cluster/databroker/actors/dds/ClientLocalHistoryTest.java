/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class ClientLocalHistoryTest extends AbstractClientHistoryTest<ClientLocalHistory> {

    private static final ClientLocalHistory OBJECT = new ClientLocalHistory(CLIENT_BEHAVIOUR, LOCAL_HISTORY_IDENTIFIER);

    @Override
    protected ClientLocalHistory object() {
        return OBJECT;
    }

    @Test
    public void testClose() throws Exception {
        OBJECT.close();
    }

    @Test
    public void testDoCreateTransaction() throws Exception {
        final ClientTransaction clientTransaction = OBJECT.doCreateTransaction();
        Assert.assertEquals(OBJECT.getIdentifier(), clientTransaction.getIdentifier().getHistoryId());
    }

    @Override
    @Test
    public void testOnTransactionAbort() throws Exception {
        final ClientSnapshot clientSnapshot = OBJECT.doCreateSnapshot();
        Assert.assertTrue(clientSnapshot.abort());
    }

    @Override
    @Test
    public void testOnTransactionReadyAndComplete() throws Exception {
        resetIdleState(OBJECT);
        OBJECT.updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.TX_OPEN);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                OBJECT.onTransactionReady(clientTransaction, abstractTransactionCommitCohort);
        Assert.assertEquals(abstractTransactionCommitCohort, transactionCommitCohort);
        OBJECT.onTransactionComplete(TRANSACTION_ID);
    }

    @Test
    public void testOnTransactionReadyAndCompleteStateClosed() throws Exception {
        resetIdleState(OBJECT);
        OBJECT.updateState(AbstractClientHistory.State.IDLE, AbstractClientHistory.State.CLOSED);
        final AbstractTransactionCommitCohort transactionCommitCohort =
                OBJECT.onTransactionReady(clientTransaction, abstractTransactionCommitCohort);
        Assert.assertEquals(abstractTransactionCommitCohort, transactionCommitCohort);
        OBJECT.onTransactionComplete(TRANSACTION_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnTransactionReadyAndCompleteIdleFail() throws Exception {
        resetIdleState(OBJECT);
        OBJECT.onTransactionReady(clientTransaction, abstractTransactionCommitCohort);
    }

    @Override
    @Test
    public void testCreateHistoryProxy() throws Exception {
        final AbstractClientConnection<ShardBackendInfo> clientConnection = CLIENT_BEHAVIOUR.getConnection(0L);
        final ProxyHistory historyProxy = OBJECT.createHistoryProxy(LOCAL_HISTORY_IDENTIFIER, clientConnection);
        Assert.assertEquals(OBJECT.getIdentifier(), historyProxy.getIdentifier());
    }

    @Override
    public void testDoCreateSnapshot() throws Exception {
        resetIdleState(OBJECT);
        final ClientSnapshot clientSnapshot = OBJECT.doCreateSnapshot();
        Assert.assertEquals(new TransactionIdentifier(OBJECT.getIdentifier(), OBJECT.nextTx()).getHistoryId(),
                clientSnapshot.getIdentifier().getHistoryId());
    }

}