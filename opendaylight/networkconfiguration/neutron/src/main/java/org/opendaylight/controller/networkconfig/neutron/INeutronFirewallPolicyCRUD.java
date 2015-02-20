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
 * This interface defines the methods for CRUD of NB OpenStack Firewall Policy objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronFirewallPolicyCRUD}
 */

@Deprecated
public interface INeutronFirewallPolicyCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *FirewallPolicy object exists
     *
     * @param uuid
     *            UUID of the Firewall Policy object
     * @return boolean
     */

    public boolean neutronFirewallPolicyExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * FirewallPolicy object exists
     *
     * @param uuid
     *            UUID of the Firewall Policy object
     * @return {@link NeutronFirewallPolicy}
     *          OpenStackFirewallPolicy class
     */

    public NeutronFirewallPolicy getNeutronFirewallPolicy(String uuid);

    /**
     * Applications call this interface method to return all Firewall Policy objects
     *
     * @return List of OpenStack Firewall Policy objects
     */

    public List<NeutronFirewallPolicy> getAllNeutronFirewallPolicies();

    /**
     * Applications call this interface method to add a Firewall Policy object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronFirewallPolicy(NeutronFirewallPolicy input);

    /**
     * Applications call this interface method to remove a Neutron FirewallPolicy object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Firewall Policy object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronFirewallPolicy(String uuid);

    /**
     * Applications call this interface method to edit a FirewallPolicy object
     *
     * @param uuid
     *            identifier of the Firewall Policy object
     * @param delta
     *            OpenStackFirewallPolicy object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronFirewallPolicy(String uuid, NeutronFirewallPolicy delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the Firewall Policy object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronFirewallPolicyInUse(String uuid);

}
