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
 * This interface defines the methods for CRUD of NB OpenStack Firewall Rule objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronFirewallRuleCRUD}
 */

@Deprecated
public interface INeutronFirewallRuleCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *FirewallRule object exists
     *
     * @param uuid
     *            UUID of the Firewall Rule object
     * @return boolean
     */

    public boolean neutronFirewallRuleExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * FirewallRule object exists
     *
     * @param uuid
     *            UUID of the Firewall Rule object
     * @return {@link NeutronFirewallRule}
     *          OpenStackFirewall Rule class
     */

    public NeutronFirewallRule getNeutronFirewallRule(String uuid);

    /**
     * Applications call this interface method to return all Firewall Rule objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronFirewallRule> getAllNeutronFirewallRules();

    /**
     * Applications call this interface method to add a Firewall Rule object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronFirewallRule(NeutronFirewallRule input);

    /**
     * Applications call this interface method to remove a Neutron FirewallRule object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Firewall Rule object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronFirewallRule(String uuid);

    /**
     * Applications call this interface method to edit a FirewallRule object
     *
     * @param uuid
     *            identifier of the Firewall Rule object
     * @param delta
     *            OpenStackFirewallRule object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronFirewallRule(String uuid, NeutronFirewallRule delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the Firewall Rule object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronFirewallRuleInUse(String uuid);

}
