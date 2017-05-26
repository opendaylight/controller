/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package ntfbenchmark.impl;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;

public class NtfbenchNonblockingProducer extends AbstractNtfbenchProducer {

    private final SettableFuture<?> lastDeliveryFuture = SettableFuture.create();


    public NtfbenchNonblockingProducer(final NotificationPublishService publishService, final int iterations,
            final int payloadSize) {
        super(publishService, iterations, payloadSize);
    }


    @Override
    public void run() {
        int ntfOk = 0;
        int ntfError = 0;
        ListenableFuture<?> lastOkFuture = null;
        for (int i = 0; i < this.iterations; i++) {
            try {
                final ListenableFuture<?> result = this.publishService.offerNotification(this.ntf);
                if (NotificationPublishService.REJECTED == result) {
                    ntfError++;
                } else {
                    ntfOk++;
                    lastOkFuture = result;
                }
            } catch (final Exception e) {
                ntfError++;
            }
        }

        this.ntfOk = ntfOk;
        this.ntfError = ntfError;
        // We wait for last notification to be delivered to listeners.
        if (lastOkFuture != null) {
            try {
                lastOkFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw Throwables.propagate(e);
            }
        }
    }

}
