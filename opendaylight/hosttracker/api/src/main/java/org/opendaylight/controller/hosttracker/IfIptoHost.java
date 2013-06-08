
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.utils.Status;

/**
 * This interface defines the methods to retrieve information about
 * learned Hosts. Also provides methods to statically add/remove
 * Hosts from the local database.
 *
 */

public interface IfIptoHost {
    /**
     * Applications call this interface methods to determine IP address to MAC binding and its
     * connectivity to an OpenFlow switch in term of Node, Port, and VLAN. These
     * bindings are learned dynamically as well as can be added statically through
     * Northbound APIs. If a binding is unknown, then an ARP request is initiated
     * immediately to discover the host.
     *
     * @param networkAddress	IP Address of the Host encapsulated in class InetAddress
     * @return					{@link org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector}
     * 							Class that contains the Host info such as its MAC address,
     * 							Switch ID, port, VLAN. If Host is not found, returns NULL
     */
    public HostNodeConnector hostFind(InetAddress networkAddress);

    /**
     * Checks the local Host Database to see if a Host has been learned for a
     * given IP address.
     *
     * @param networkAddress	IP Address of the Host encapsulated in class InetAddress
     * @return					{@link org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector}
     * 							Class that contains the Host info such as its MAC address,
     * 							Switch ID, port, VLAN. If Host is not found, returns NULL
     *
     */
    public HostNodeConnector hostQuery(InetAddress networkAddress);

    /**
     * Initiates an immediate discovery of the Host for a given IP address. This
     * provides for the calling applications to block on the host discovery.
     *
     * @param networkAddress 		IP address encapsulated in InetAddress class
     * @return						Future {@link org.opendaylight.controller.hosttracker.HostTrackerCallable}
     */
    public Future<HostNodeConnector> discoverHost(InetAddress networkAddress);

    /**
     * Returns the Network Hierarchy for a given Host. This API is typically used by
     * applications like Hadoop for Rack Awareness functionality.
     *
     * @param 					IP address of the Host encapsulated in InetAddress class
     * @return					List of String ArrayList containing the Hierarchies.
     */
    public List<List<String>> getHostNetworkHierarchy(InetAddress hostAddress);

    /**
     * Returns all the the Hosts either learned dynamically or added statically via
     * Northbound APIs.
     *
     * @return					Set of {@link org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector}.
     * 							Class that contains the Host info such as its MAC address,
     * 							Switch ID, port, VLAN.
     */
    public Set<HostNodeConnector> getAllHosts();

    /**
     * Returns all the "Active Hosts" learned "Statically" via Northbound APIs. These Hosts
     * are categorized as "Active" because the Switch and Port they are connected to, are in
     * up state.
     *
     * @return					Set of {@link org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector}.
     * 							Class that contains the Host info such as MAC address,
     * 							Switch ID, port, VLAN. If Host is not found, returns NULL
     */
    public Set<HostNodeConnector> getActiveStaticHosts();

    /**
     * Returns all the "Inactive Hosts" learned "Statically" via Northbound APIs. These Hosts
     * are categorized as "Inactive" because either the Switch or the Port they are connected
     * to, is in down state.
     *
     * @return					Set of HostNodeConnector {@link org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector}.
     * 							HostNodeConnector is Class that
     * 							contains the Host info such as its MAC address, OpenFlowNode
     * 							ID, port, VLAN.
     */
    public Set<HostNodeConnector> getInactiveStaticHosts();

    /**
     * Hosts can be learned dynamically or added statically. This method allows the addition
     * of a Host to the local database statically.
     *
     * @param networkAddress		IP Address of the Host
     * @param dataLayerAddress		MAC Address of the Host
     * @param nc				    NodeConnector to which the host is attached
     * @param vlan					VLAN the host belongs to
     * @return						The status object as described in {@code Status}
     * 								indicating the result of this action.
     */
    public Status addStaticHost(String networkAddress, String dataLayerAddress,
                                NodeConnector nc, String vlan);

    /**
     * Allows the deletion of statically learned Host
     *
     * @param networkAddress
     * @return						The status object as described in {@code Status}
     * 								indicating the result of this action.
     */
    public Status removeStaticHost(String networkAddress);
}
