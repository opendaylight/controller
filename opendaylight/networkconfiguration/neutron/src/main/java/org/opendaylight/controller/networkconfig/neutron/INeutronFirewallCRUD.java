/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.List;

/**
 * This interface defines the methods for CRUD of NB OpenStack Firewall objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronFirewallCRUD}
 */

@Deprecated
public interface INeutronFirewallCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *Firewall object exists
     *
     * @param uuid
     *            UUID of the Firewall object
     * @return boolean
     */

    public boolean neutronFirewallExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Firewall object exists
     *
     * @param uuid
     *            UUID of the Firewall object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronFirewall}
     *          OpenStackFirewall class
     */

    public NeutronFirewall getNeutronFirewall(String uuid);

    /**
     * Applications call this interface method to return all Firewall objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronFirewall> getAllNeutronFirewalls();

    /**
     * Applications call this interface method to add a Firewall object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronFirewall(NeutronFirewall input);

    /**
     * Applications call this interface method to remove a Neutron Firewall object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Firewall object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronFirewall(String uuid);

    /**
     * Applications call this interface method to edit a Firewall object
     *
     * @param uuid
     *            identifier of the Firewall object
     * @param delta
     *            OpenStackFirewall object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronFirewall(String uuid, NeutronFirewall delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the Firewall object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronFirewallInUse(String uuid);

}
