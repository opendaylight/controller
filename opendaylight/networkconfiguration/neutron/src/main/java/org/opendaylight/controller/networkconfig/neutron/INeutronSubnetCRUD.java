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
 * This interface defines the methods for CRUD of NB Subnet objects
 *
 */

public interface INeutronSubnetCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Subnet object exists
     *
     * @param uuid
     *            UUID of the Subnet object
     * @return boolean
     */

    public boolean subnetExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Subnet object exists
     *
     * @param uuid
     *            UUID of the Subnet object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronSubnet.OpenStackSubnets}
     *          OpenStack Subnet class
     */

    public NeutronSubnet getSubnet(String uuid);

    /**
     * Applications call this interface method to return all Subnet objects
     *
     * @return List of OpenStackSubnets objects
     */

    public List<NeutronSubnet> getAllSubnets();

    /**
     * Applications call this interface method to add a Subnet object to the
     * concurrent map
     *
     * @param input
     *            OpenStackSubnet object
     * @return boolean on whether the object was added or not
     */

    public boolean addSubnet(NeutronSubnet input);

    /**
     * Applications call this interface method to remove a Subnet object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Subnet object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeSubnet(String uuid);

    /**
     * Applications call this interface method to edit a Subnet object
     *
     * @param uuid
     *            identifier of the Subnet object
     * @param delta
     *            OpenStackSubnet object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateSubnet(String uuid, NeutronSubnet delta);

    /**
     * Applications call this interface method to determine if a Subnet object
     * is use
     *
     * @param subnetUUID
     *            identifier of the subnet object
     *
     * @return boolean on whether the subnet is in use or not
     */

    public boolean subnetInUse(String subnetUUID);
}
