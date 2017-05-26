/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package ntfbenchmark.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.Ntfbench;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.NtfbenchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.payload.Payload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.payload.PayloadBuilder;

public abstract class AbstractNtfbenchProducer implements Runnable {
    protected final NotificationPublishService publishService;
    protected final int iterations;
    protected final Ntfbench ntf;

    /**
     * @return the ntfOk
     */
    public int getNtfOk() {
        return ntfOk;
    }

    /**
     * @return the ntfError
     */
    public int getNtfError() {
        return ntfError;
    }

    protected int ntfOk = 0;
    protected int ntfError = 0;

    public AbstractNtfbenchProducer(final NotificationPublishService publishService, final int iterations,
            final int payloadSize) {
        this.publishService = publishService;
        this.iterations = iterations;

        final List<Payload> listVals = new ArrayList<>();
        for (int i = 0; i < payloadSize; i++) {
            listVals.add(new PayloadBuilder().setId(i).build());
        }

        ntf = new NtfbenchBuilder().setPayload(listVals).build();
    }
}
