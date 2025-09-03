/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Single entry in a {@link AbstractClientConnection}. Tracks the request, the associated callback and time when
 * the request was first enqueued.
 */
public sealed class ConnectionEntry implements Immutable permits TransmittedConnectionEntry {
    private final Consumer<Response<?, ?>> callback;
    private final Request<?, ?> request;
    private final long enqueuedTicks;

    ConnectionEntry(final Request<?, ?> request, final Consumer<Response<?, ?>> callback, final long now) {
        this.request = requireNonNull(request);
        this.callback = requireNonNull(callback);
        enqueuedTicks = now;
    }

    ConnectionEntry(final ConnectionEntry entry) {
        this(entry.request, entry.callback, entry.enqueuedTicks);
    }

    /**
     * {@return the callback}
     */
    public final Consumer<Response<?, ?>> getCallback() {
        return callback;
    }

    /**
     * {@return the request}
     */
    public final Request<?, ?> getRequest() {
        return request;
    }

    /**
     * Invoke the callback with specified response.
     *
     * @param response the response.
     */
    public final void complete(final Response<?, ?> response) {
        callback.accept(response);
    }

    /**
     * {@return the logical time when this entry was first enqueued}
     */
    public final long getEnqueuedTicks() {
        return enqueuedTicks;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("request", request).add("enqueuedTicks", enqueuedTicks);
    }
}
