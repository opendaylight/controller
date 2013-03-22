
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/*
 * Provides a mechanism for applications to do an inline Host Discovery as opposed
 * to a delayed discovery
 */
package org.opendaylight.controller.hosttracker;

/**
 * This Class provides methods to discover Host through a blocking call
 * mechanism. Applications can make use of these methods if they don't 
 * find a host in HostTracker's database and want to discover the host  
 * in the same thread without being called by a callback function.
 */
import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

public class HostTrackerCallable implements Callable<HostNodeConnector> {

    InetAddress trackedHost;
    HostTracker hostTracker;
    protected CountDownLatch latch;

    public HostTrackerCallable(HostTracker tracker, InetAddress inet) {
        trackedHost = inet;
        hostTracker = tracker;
        latch = new CountDownLatch(1);
    }

    @Override
    public HostNodeConnector call() throws Exception {
        HostNodeConnector h = hostTracker.hostFind(trackedHost);
        if (h != null)
            return h;
        hostTracker.setCallableOnPendingARP(trackedHost, this);
        latch.await();
        return hostTracker.hostQuery(trackedHost);
    }

    public void wakeup() {
        this.latch.countDown();
    }
}
