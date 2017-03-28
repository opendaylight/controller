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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ClientBackedReadWriteTransactionTest extends ClientBackedTransactionTest<ClientBackedReadWriteTransaction>
{
    @Override
    ClientBackedReadWriteTransaction object() throws Exception {
        final ClientLocalHistory history = Mockito.mock(ClientLocalHistory.class);
        final Constructor transactionConstructor = ClientTransaction.class.getDeclaredConstructor(
                AbstractClientHistory.class, TransactionIdentifier.class);
        transactionConstructor.setAccessible(true);
        final ClientTransaction delegate = (ClientTransaction) transactionConstructor.newInstance(
                history, TRANSACTION_IDENTIFIER);
        return new ClientBackedReadWriteTransaction(delegate);
    }

    @Test
    public void read() throws Exception {
        Assert.assertNotNull(object().read(YangInstanceIdentifier.EMPTY));
    }

    @Test
    public void exists() throws Exception {
        Assert.assertTrue(object().exists(YangInstanceIdentifier.EMPTY).get());
    }
}