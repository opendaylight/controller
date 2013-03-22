
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.hostAware;

import java.net.InetAddress;

/**
 * This Interface  defines the methods to trigger the discovery of
 * a Host and to probe if a learned Host is still in the network.
 *
 *
 *
 */
public interface IHostFinder {
    /**
     * This method initiates the discovery of a host based on its IP address. This is triggered
     * by query of an application to the HostTracker. The requested IP address
     * doesn't exist in the local database at this point.
     *
     * @param networkAddress	IP Address encapsulated in InetAddress class
     *
     */
    public void find(InetAddress networkAddress);

    /**
     * This method is called by HostTracker to see if a learned Host is still in the network.
     * Used mostly for ARP Aging.
     *
     * @param host			The Host that needs to be probed
     */
    public void probe(HostNodeConnector host);
}
