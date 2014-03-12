/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.protocol.framework.util;

import io.netty.util.concurrent.Future;

import org.opendaylight.protocol.framework.ReconnectStrategy;

public final class ReconnectStrategyCloseable implements ReconnectStrategy, AutoCloseable {

    private final ReconnectStrategy inner;

    public ReconnectStrategyCloseable(final ReconnectStrategy inner) {
        this.inner = inner;
    }

    @Override
    public void close() {
    }

    @Override
    public int getConnectTimeout() throws Exception {
        return this.inner.getConnectTimeout();
    }

    @Override
    public Future<Void> scheduleReconnect(final Throwable cause) {
        return this.inner.scheduleReconnect(cause);
    }

    @Override
    public void reconnectSuccessful() {
        this.inner.reconnectSuccessful();
    }
}
