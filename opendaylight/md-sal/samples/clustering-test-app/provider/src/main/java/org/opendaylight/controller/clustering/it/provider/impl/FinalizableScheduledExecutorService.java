/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A simple ScheduledExecutorService, which shutds down its threads after a period of inactivity. It is safe to not
 * shutdown this
 *
 * @author Robert Varga
 */
final class FinalizableScheduledExecutorService extends ScheduledThreadPoolExecutor {

    private FinalizableScheduledExecutorService(final int maxThreads, final long time, final TimeUnit unit) {
        super(maxThreads);
        setKeepAliveTime(time, unit);
        allowCoreThreadTimeOut(true);
    }

    static ScheduledThreadPoolExecutor newSingleThread() {
        return new FinalizableScheduledExecutorService(1, 15, TimeUnit.SECONDS);
    }

    // This is a bit ugly, but allows
    @Override
    protected void finalize() {
        super.shutdownNow();
    }
}
