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
import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class HostTrackerTest extends TestCase {

    @Test
    public void testHostTrackerCallable() throws UnknownHostException {

        HostTracker hostTracker = null;
        hostTracker = new HostTracker();
        Assert.assertFalse(hostTracker == null);

        InetAddress hostIP = InetAddress.getByName("192.168.0.8");

        HostTrackerCallable htCallable = new HostTrackerCallable(hostTracker,
                hostIP);
        Assert.assertTrue(htCallable.trackedHost.equals(hostIP));
        Assert.assertTrue(htCallable.hostTracker.equals(hostTracker));

        long count = htCallable.latch.getCount();
        htCallable.wakeup();
        Assert.assertTrue(htCallable.latch.getCount() == (count - 1));
    }

    @Test
    public void testHostTracker() throws UnknownHostException {
        HostTracker hostTracker = null;
        hostTracker = new HostTracker();
        Assert.assertFalse(hostTracker == null);

        InetAddress hostIP_1 = InetAddress.getByName("192.168.0.8");
        InetAddress hostIP_2 = InetAddress.getByName("192.168.0.18");
        hostTracker.discoverHost(hostIP_1);
        hostTracker.discoverHost(hostIP_2);
        hostTracker.nonClusterObjectCreate();
    }

}
