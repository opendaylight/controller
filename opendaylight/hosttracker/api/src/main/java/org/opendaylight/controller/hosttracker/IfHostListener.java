
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

/**
 * This interface defines the method to notify detected Host on the
 * network. The information includes Host's IP address, MAC address,
 * switch ID, port, and VLAN.
 *
 */

public interface IfHostListener {
    /**
     * Learns  new Hosts. Called by ArpHandler and implemented in
     * HostTracker.java. If a Host is learned for the first time then
     * adds it to the local database and informs other applications
     * of coming up a new Host. For the hosts which it has already
     * learned, it refreshes them.
     *
     * @param host		Host info encapsulated in HostNodeConnector class
     */
    public void hostListener(HostNodeConnector host);
}
