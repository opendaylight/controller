/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.hosttracker.IHostId;
import org.opendaylight.controller.hosttracker.IPHostId;

public class HostTrackerTest {

    @Test
    public void testHostTrackerCallable() throws UnknownHostException {

        HostTracker hostTracker = null;
        hostTracker = new HostTracker();

        InetAddress hostIP = InetAddress.getByName("192.168.0.8");
        IHostId id  = IPHostId.fromIP(hostIP);

        HostTrackerCallable htCallable = new HostTrackerCallable(hostTracker,
                id);
        Assert.assertTrue(htCallable.trackedHost.equals(id));
        Assert.assertTrue(htCallable.hostTracker.equals(hostTracker));

        long count = htCallable.latch.getCount();
        htCallable.wakeup();
        Assert.assertTrue(htCallable.latch.getCount() == (count - 1));
    }

    @Test
    public void testHostTracker() throws UnknownHostException {
        HostTracker hostTracker = null;
        hostTracker = new HostTracker();

        InetAddress hostIP_1 = InetAddress.getByName("192.168.0.8");
        IHostId id1 = IPHostId.fromIP(hostIP_1);
        InetAddress hostIP_2 = InetAddress.getByName("192.168.0.18");
        IHostId id2 = IPHostId.fromIP(hostIP_2);
        hostTracker.discoverHost(id1);
        hostTracker.discoverHost(id2);
        hostTracker.nonClusterObjectCreate();
    }

}
