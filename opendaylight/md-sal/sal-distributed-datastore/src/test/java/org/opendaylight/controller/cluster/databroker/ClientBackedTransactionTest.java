/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHandle;

public abstract class ClientBackedTransactionTest<T extends ClientBackedTransaction<?>> {
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    protected static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);

    private static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0);
    protected static final TransactionIdentifier TRANSACTION_ID = new TransactionIdentifier(HISTORY_ID, 0);

    abstract T object();

    @Test
    public void testClose() {
        final AbstractClientHandle<?> delegate = object().delegate();
        object().close();
        // Called twice because of immediate cleaning
        verify(delegate, times(2)).abort();
    }
}
