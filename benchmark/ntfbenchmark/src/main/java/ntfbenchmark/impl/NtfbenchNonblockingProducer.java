/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ntfbenchmark.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtfbenchNonblockingProducer extends AbstractNtfbenchProducer {
    private static final Logger LOG = LoggerFactory.getLogger(NtfbenchNonblockingProducer.class);

    public NtfbenchNonblockingProducer(final NotificationPublishService publishService, final int iterations,
            final int payloadSize) {
        super(publishService, iterations, payloadSize);
    }


    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public void run() {
        int ntfOk = 0;
        int ntfError = 0;
        ListenableFuture<?> lastOkFuture = null;
        for (int i = 0; i < iterations; i++) {
            try {
                final ListenableFuture<?> result = publishService.offerNotification(ntf);
                if (NotificationPublishService.REJECTED == result) {
                    ntfError++;
                } else {
                    ntfOk++;
                    lastOkFuture = result;
                }
            } catch (final Exception e) {
                LOG.debug("Failed to publish notification", e);
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
                throw new IllegalStateException(e);
            }
        }
    }
}
