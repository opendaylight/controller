/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package ntfbenchmark.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;

public class NtfbenchBlockingProducer extends AbstractNtfbenchProducer {

    public NtfbenchBlockingProducer(final NotificationPublishService publishService, final int iterations,
            final int payloadSize) {
        super(publishService, iterations, payloadSize);
    }

    @Override
    public void run() {
        int ntfOk = 0;
        int ntfError = 0;

        for (int i = 0; i < this.iterations; i++) {
            try {
                this.publishService.putNotification(this.ntf);
                ntfOk++;
            } catch (final Exception e) {
                ntfError++;
            }
        }

        this.ntfOk = ntfOk;
        this.ntfError = ntfError;
    }
}
