/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ntfbenchmark.impl;

import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbench.payload.rev150709.Ntfbench;

public class NtfbenchWTCListener extends NtfbenchTestListener {
    private final int expectedCount;
    private final SettableFuture<?> allDone = SettableFuture.create();

    public NtfbenchWTCListener(final int expectedSize, final int expectedCount) {
        super(expectedSize);
        this.expectedCount = expectedCount;
    }

    @Override
    public void onNotification(final Ntfbench notification) {
        super.onNotification(notification);
        if (expectedCount == getReceived()) {
            allDone.set(null);
        }
    }

    @Override
    public SettableFuture<?> getAllDone() {
        return allDone;
    }
}
