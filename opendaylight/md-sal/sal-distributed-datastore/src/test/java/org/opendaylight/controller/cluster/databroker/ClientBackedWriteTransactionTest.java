/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHandle;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ClientBackedWriteTransactionTest extends ClientBackedTransactionTest<ClientBackedWriteTransaction> {
    private ClientBackedWriteTransaction object;

    @Mock
    private ClientTransaction delegate;
    @Mock
    private NormalizedNode data;
    @Mock
    private YangInstanceIdentifier path;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final Field identifierField = AbstractClientHandle.class.getDeclaredField("transactionId");
        identifierField.setAccessible(true);
        identifierField.set(delegate, TRANSACTION_IDENTIFIER);

        object = new ClientBackedWriteTransaction(delegate);
    }

    @Override
    ClientBackedWriteTransaction object() throws Exception {
        return object;
    }

    @Test
    public void testWrite() throws Exception {
        object().write(path, data);
        Mockito.verify(delegate, Mockito.timeout(1)).write(path, data);
    }

    @Test
    public void testMerge() throws Exception {
        object().merge(path, data);
        Mockito.verify(delegate, Mockito.timeout(1)).merge(path, data);
    }

    @Test
    public void testDelete() throws Exception {
        object().delete(YangInstanceIdentifier.EMPTY);
        Mockito.verify(delegate, Mockito.timeout(1)).delete(path);
    }

    @Test
    public void testReady() throws Exception {
        object().ready();
        Mockito.verify(delegate, Mockito.times(1)).ready();
    }
}