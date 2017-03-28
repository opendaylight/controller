/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import java.lang.reflect.Constructor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ClientBackedWriteTransactionTest extends ClientBackedTransactionTest<ClientBackedWriteTransaction> {

    @Override
    ClientBackedWriteTransaction object() throws Exception {
        final ClientLocalHistory history = Mockito.mock(ClientLocalHistory.class);
        final Constructor transactionConstructor = ClientTransaction.class.getDeclaredConstructor(
                AbstractClientHistory.class, TransactionIdentifier.class);
        transactionConstructor.setAccessible(true);
        final ClientTransaction delegate = (ClientTransaction) transactionConstructor.newInstance(
                history, TRANSACTION_IDENTIFIER);
        return new ClientBackedWriteTransaction(delegate);
    }

    @Test
    public void write() throws Exception {
        object().write(YangInstanceIdentifier.EMPTY, null);
    }

    @Test
    public void merge() throws Exception {
        object().merge(YangInstanceIdentifier.EMPTY, null);
    }

    @Test
    public void delete() throws Exception {
        object().delete(YangInstanceIdentifier.EMPTY);
    }

    @Test
    public void ready() throws Exception {
        final DOMStoreThreePhaseCommitCohort cohort = object().ready();
        Assert.assertTrue(cohort.canCommit().get());
    }
}