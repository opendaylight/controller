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
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ClientBackedReadTransactionTest extends ClientBackedTransactionTest<ClientBackedReadTransaction> {

    @Override
    ClientBackedReadTransaction object() throws Exception {
        final ClientLocalHistory history = Mockito.mock(ClientLocalHistory.class);

        final Constructor snapshotConstructor = ClientSnapshot.class.getDeclaredConstructor(
                AbstractClientHistory.class, TransactionIdentifier.class);
        snapshotConstructor.setAccessible(true);
        final ClientSnapshot delegate = (ClientSnapshot) snapshotConstructor.newInstance(
                history, TRANSACTION_IDENTIFIER);

        final ClientBackedTransactionChain snapshot = new ClientBackedTransactionChain(history);
        return new ClientBackedReadTransaction(delegate, snapshot);
    }

    @Test
    public void read() throws Exception {
        Assert.assertNotNull(object().read(YangInstanceIdentifier.EMPTY));
    }

    @Test
    public void exists() throws Exception {
        Assert.assertTrue(object().exists(YangInstanceIdentifier.EMPTY).get());
    }

    @Test
    public void close() throws Exception {
        object().close();
    }
}