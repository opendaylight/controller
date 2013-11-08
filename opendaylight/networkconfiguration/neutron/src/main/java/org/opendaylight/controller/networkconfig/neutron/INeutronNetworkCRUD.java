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
 * This interface defines the methods for CRUD of NB network objects
 *
 */

public interface INeutronNetworkCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Network object exists
     *
     * @param uuid
     *            UUID of the Network object
     * @return boolean
     */

    public boolean networkExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Network object exists
     *
     * @param uuid
     *            UUID of the Network object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronNetwork.OpenStackNetworks}
     *          OpenStack Network class
     */

    public NeutronNetwork getNetwork(String uuid);

    /**
     * Applications call this interface method to return all Network objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronNetwork> getAllNetworks();

    /**
     * Applications call this interface method to add a Network object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNetwork(NeutronNetwork input);

    /**
     * Applications call this interface method to remove a Network object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the network object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNetwork(String uuid);

    /**
     * Applications call this interface method to edit a Network object
     *
     * @param uuid
     *            identifier of the network object
     * @param delta
     *            OpenStackNetwork object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNetwork(String uuid, NeutronNetwork delta);

    /**
     * Applications call this interface method to determine if a Network object
     * is use
     *
     * @param netUUID
     *            identifier of the network object
     *
     * @return boolean on whether the network is in use or not
     */

    public boolean networkInUse(String netUUID);
}
