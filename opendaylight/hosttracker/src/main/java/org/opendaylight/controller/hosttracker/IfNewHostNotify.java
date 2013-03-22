
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
 * This Interface defines the methods for client applications of
 * Host Tracker to get notifications when a new host is learned or 
 * existing host is removed from the network.
 *
 */
public interface IfNewHostNotify {
    /**
     * Notifies the HostTracker Clients that a new Host has been learned
     *
     * @param host		Host Info encapsulated in HostNodeConnector class
     */
    public void notifyHTClient(HostNodeConnector host);

    /**
     * Notifies the HostTracker Clients that a Host which was learned in
     * the past has been removed either due to switch/port down event or
     * due to ARP Aging
     *
     * @param host		Host Info encapsulated in HostNodeConnector class
     */
    public void notifyHTClientHostRemoved(HostNodeConnector host);
}
