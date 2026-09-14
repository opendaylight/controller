/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;

class HaltedTransmitQueueTest extends AbstractTransmitQueueTest<TransmitQueue.Halted> {
    @Override
    int getMaxInFlightMessages() {
        return 0;
    }

    @Override
    TransmitQueue.Halted createQueue() {
        return new TransmitQueue.Halted(0);
    }

    @Test
    @Override
    void testCanTransmitCount() {
        assertThat(queue.canTransmitCount(0)).isNotPositive();
    }

    @Test
    @Override
    void testTransmit() {
        final var request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final var callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        final var entry = new ConnectionEntry(request, callback, now);
        assertThrows(UnsupportedOperationException.class, () -> queue.transmit(entry, now));
    }
}
