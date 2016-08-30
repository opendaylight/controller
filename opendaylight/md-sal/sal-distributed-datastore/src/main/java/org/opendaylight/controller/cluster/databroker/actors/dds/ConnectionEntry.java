/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.client.RequestCallback;
import org.opendaylight.controller.cluster.access.concepts.Request;

class ConnectionEntry {
    private final RequestCallback callback;
    private final Request<?, ?> request;
    private final long enqueuedTicks;

    ConnectionEntry(final Request<?, ?> request, final RequestCallback callback, final long now) {
        this.request = Preconditions.checkNotNull(request);
        this.callback = Preconditions.checkNotNull(callback);
        this.enqueuedTicks = now;
    }

    ConnectionEntry(final ConnectionEntry entry) {
        this(entry.request, entry.callback, entry.enqueuedTicks);
    }

    long getEnqueuedTicks() {
        return enqueuedTicks;
    }

    Request<?, ?> getRequest() {
        return request;
    }
}
