/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import java.util.concurrent.atomic.AtomicLong;

public final class SessionIdProvider {

    private final AtomicLong sessionCounter = new AtomicLong(0);

    public long getNextSessionId() {
        return sessionCounter.incrementAndGet();
    }

    public long getCurrentSessionId() {
        return sessionCounter.get();
    }
}
