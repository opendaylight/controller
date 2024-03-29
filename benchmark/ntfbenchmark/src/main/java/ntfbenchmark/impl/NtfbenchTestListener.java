/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ntfbenchmark.impl;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import org.opendaylight.mdsal.binding.api.NotificationService.Listener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.Ntfbench;

public class NtfbenchTestListener implements Listener<Ntfbench> {
    private final int expectedSize;
    private int received = 0;

    public NtfbenchTestListener(final int expectedSize) {
        this.expectedSize = expectedSize;
    }

    @Override
    public void onNotification(final Ntfbench notification) {
        if (expectedSize == notification.getPayload().size()) {
            received++;
        }
    }

    public int getReceived() {
        return received;
    }

    public Future<?> getAllDone() {
        return Futures.immediateFuture(null);
    }
}
