/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Single entry in a {@link AbstractClientConnection}. Tracks the request, the associated callback and time when
 * the request was first enqueued.
 *
 * @author Robert Varga
 */
@Beta
public class ConnectionEntry implements Immutable {
    private final Consumer<Response<?, ?>> callback;
    private final Request<?, ?> request;
    private final long enqueuedTicks;

    ConnectionEntry(final Request<?, ?> request, final Consumer<Response<?, ?>> callback, final long now) {
        this.request = Preconditions.checkNotNull(request);
        this.callback = Preconditions.checkNotNull(callback);
        this.enqueuedTicks = now;
    }

    ConnectionEntry(final ConnectionEntry entry) {
        this(entry.request, entry.callback, entry.enqueuedTicks);
    }

    public final Consumer<Response<?, ?>> getCallback() {
        return callback;
    }

    public final Request<?, ?> getRequest() {
        return request;
    }

    public void complete(final Response<?, ?> response) {
        callback.accept(response);
    }

    public final long getEnqueuedTicks() {
        return enqueuedTicks;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("request", request).add("enqueuedTicks", enqueuedTicks);
    }
}
