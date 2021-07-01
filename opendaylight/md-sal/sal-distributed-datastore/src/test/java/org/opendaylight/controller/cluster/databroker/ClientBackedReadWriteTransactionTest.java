/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateFluentFuture;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateTrueFluentFuture;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ClientBackedReadWriteTransactionTest
        extends ClientBackedTransactionTest<ClientBackedReadWriteTransaction> {
    private ClientBackedReadWriteTransaction object;

    @Mock
    private ClientTransaction delegate;
    @Mock
    private NormalizedNode data;

    @Override
    ClientBackedReadWriteTransaction object() {
        return object;
    }

    @Before
    public void setUp() {
        doReturn(TRANSACTION_ID).when(delegate).getIdentifier();

        doReturn(immediateTrueFluentFuture()).when(delegate).exists(YangInstanceIdentifier.empty());
        doReturn(immediateFluentFuture(Optional.of(data))).when(delegate).read(YangInstanceIdentifier.empty());

        object = new ClientBackedReadWriteTransaction(delegate, null);
    }

    @Test
    public void testRead() throws Exception {
        final FluentFuture<Optional<NormalizedNode>> result = object().read(YangInstanceIdentifier.empty());
        final Optional<NormalizedNode> resultData = result.get();
        assertTrue(resultData.isPresent());
        assertEquals(data, resultData.get());
    }

    @Test
    public void testExists() throws Exception {
        assertEquals(Boolean.TRUE, object().exists(YangInstanceIdentifier.empty()).get());
    }
}
