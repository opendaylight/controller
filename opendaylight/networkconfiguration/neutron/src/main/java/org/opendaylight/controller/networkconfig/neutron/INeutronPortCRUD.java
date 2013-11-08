/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.List;

/**
 * This interface defines the methods for CRUD of NB Port objects
 *
 */

public interface INeutronPortCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Port object exists
     *
     * @param uuid
     *            UUID of the Port object
     * @return boolean
     */

    public boolean portExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Port object exists
     *
     * @param uuid
     *            UUID of the Port object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronPort.OpenStackPorts}
     *          OpenStack Port class
     */

    public NeutronPort getPort(String uuid);

    /**
     * Applications call this interface method to return all Port objects
     *
     * @return List of OpenStackPorts objects
     */

    public List<NeutronPort> getAllPorts();

    /**
     * Applications call this interface method to add a Port object to the
     * concurrent map
     *
     * @param input
     *            OpenStackPort object
     * @return boolean on whether the object was added or not
     */

    public boolean addPort(NeutronPort input);

    /**
     * Applications call this interface method to remove a Port object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Port object
     * @return boolean on whether the object was removed or not
     */

    public boolean removePort(String uuid);

    /**
     * Applications call this interface method to edit a Port object
     *
     * @param uuid
     *            identifier of the Port object
     * @param delta
     *            OpenStackPort object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updatePort(String uuid, NeutronPort delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param macAddress
     *            mac Address to be tested
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean macInUse(String macAddress);

    /**
     * Applications call this interface method to retrieve the port associated with
     * the gateway address of a subnet
     *
     * @param subnetUUID
     *            identifier of the subnet
     * @return OpenStackPorts object if the port exists and null if it does not
     */

    public NeutronPort getGatewayPort(String subnetUUID);
}
