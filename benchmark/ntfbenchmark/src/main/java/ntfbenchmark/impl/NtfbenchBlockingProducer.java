/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ntfbenchmark.impl;

import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtfbenchBlockingProducer extends AbstractNtfbenchProducer {
    private static final Logger LOG = LoggerFactory.getLogger(NtfbenchBlockingProducer.class);

    public NtfbenchBlockingProducer(final NotificationPublishService publishService, final int iterations,
            final int payloadSize) {
        super(publishService, iterations, payloadSize);
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    public void run() {
        int ntfOk = 0;
        int ntfError = 0;

        for (int i = 0; i < iterations; i++) {
            try {
                publishService.putNotification(ntf);
                ntfOk++;
            } catch (final Exception e) {
                ntfError++;
                LOG.debug("Failed to push notification", e);
            }
        }

        this.ntfOk = ntfOk;
        this.ntfError = ntfError;
    }
}
