/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ntfbenchmark.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.Ntfbench;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.NtfbenchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.payload.Payload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.payload.PayloadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.payload.PayloadKey;

public abstract class AbstractNtfbenchProducer implements Runnable {
    protected final NotificationPublishService publishService;
    protected final int iterations;
    protected final Ntfbench ntf;

    protected int ntfOk = 0;
    protected int ntfError = 0;

    /**
     * Return number of successful notifications.
     *
     * @return the ntfOk
     */
    public int getNtfOk() {
        return ntfOk;
    }

    /**
     * Return number of unsuccessful notifications.
     *
     * @return the ntfError
     */
    public int getNtfError() {
        return ntfError;
    }

    public AbstractNtfbenchProducer(final NotificationPublishService publishService, final int iterations,
            final int payloadSize) {
        this.publishService = publishService;
        this.iterations = iterations;

        final Builder<PayloadKey, Payload> listVals = ImmutableMap.builderWithExpectedSize(payloadSize);
        for (int i = 0; i < payloadSize; i++) {
            final PayloadKey key = new PayloadKey(i);
            listVals.put(key, new PayloadBuilder().withKey(key).build());
        }

        ntf = new NtfbenchBuilder().setPayload(listVals.build()).build();
    }
}
