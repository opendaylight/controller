/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateFluentFuture;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateTrueFluentFuture;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ClientBackedReadTransactionTest extends ClientBackedTransactionTest<ClientBackedReadTransaction> {
    private ClientBackedReadTransaction object;

    @Mock
    private NormalizedNode data;
    @Mock
    private ClientActorContext clientContext;
    @Mock
    private ClientSnapshot delegate;

    @Override
    ClientBackedReadTransaction object() {
        return object;
    }

    @Before
    public void setUp() {
        doReturn(TRANSACTION_ID).when(delegate).getIdentifier();

        doReturn(immediateTrueFluentFuture()).when(delegate).exists(YangInstanceIdentifier.empty());
        doReturn(immediateFluentFuture(Optional.of(data))).when(delegate).read(YangInstanceIdentifier.empty());

        object = new ClientBackedReadTransaction(delegate, null, null);
    }

    @Test
    public void testRead() throws Exception {
        assertEquals(Optional.of(data), object().read(YangInstanceIdentifier.empty()).get());
    }

    @Test
    public void testExists() throws Exception {
        assertEquals(Boolean.TRUE, object().exists(YangInstanceIdentifier.empty()).get());
    }
}
