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
 * This interface defines the methods for CRUD of NB FloatingIP objects
 *
 */

public interface INeutronFloatingIPCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * FloatingIP object exists
     *
     * @param uuid
     *            UUID of the FloatingIP object
     * @return boolean
     */

    public boolean floatingIPExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * FloatingIP object exists
     *
     * @param uuid
     *            UUID of the FloatingIP object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP.OpenStackFloatingIPs}
     *          OpenStack FloatingIP class
     */

    public NeutronFloatingIP getFloatingIP(String uuid);

    /**
     * Applications call this interface method to return all FloatingIP objects
     *
     * @return a Set of OpenStackFloatingIPs objects
     */

    public List<NeutronFloatingIP> getAllFloatingIPs();

    /**
     * Applications call this interface method to add a FloatingIP object to the
     * concurrent map
     *
     * @param input
     *            OpenStackFloatingIP object
     * @return boolean on whether the object was added or not
     */

    public boolean addFloatingIP(NeutronFloatingIP input);

    /**
     * Applications call this interface method to remove a FloatingIP object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the FloatingIP object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeFloatingIP(String uuid);

    /**
     * Applications call this interface method to edit a FloatingIP object
     *
     * @param uuid
     *            identifier of the FloatingIP object
     * @param delta
     *            OpenStackFloatingIP object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateFloatingIP(String uuid, NeutronFloatingIP delta);
}
