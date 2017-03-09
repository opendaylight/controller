/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.base.Ticker;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;

public class HaltedTransmitQueueTest extends AbstractTransmitQueueTest<TransmitQueue.Halted> {

    @Override
    protected int getMaxInFlightMessages() {
        return 0;
    }

    @Override
    protected TransmitQueue.Halted createQueue() {
        return new TransmitQueue.Halted(0);
    }

    @Test
    @Override
    public void testCanTransmitCount() throws Exception {
        Assert.assertFalse(queue.canTransmitCount(0) > 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    @Override
    public void testTransmit() throws Exception {
        final Request request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        final ConnectionEntry entry = new ConnectionEntry(request, callback, now);
        queue.transmit(entry, now);
    }

}