/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class SingleClientHistoryTest extends AbstractClientHistoryTest {

    private static final SingleClientHistory OBJECT = new SingleClientHistory(CLIENT_BEHAVIOUR, LOCAL_HISTORY_IDENTIFIER);

    @Before
    public void setUp() throws Exception {

    }

    @Override
    protected AbstractClientHistory object() {
        return OBJECT;
    }

    @Override
    @Test
    public void testDoCreateTransaction() throws Exception {
        final ClientTransaction clientTransaction = OBJECT.doCreateTransaction();
        Assert.assertEquals(OBJECT.getIdentifier(), clientTransaction.getIdentifier().getHistoryId());
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