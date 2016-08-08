/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import com.google.common.base.Ticker;

/**
 * Utility {@link Ticker} where passage of time is explicitly controlled via {@link #increment(long)}.
 *
 * @author Robert Varga
 */
public final class TestTicker extends Ticker {
    private long counter = 0;

    @Override
    public long read() {
        return counter;
    }

    public long increment(final long ticks) {
        counter += ticks;
        return counter;
    }
}
