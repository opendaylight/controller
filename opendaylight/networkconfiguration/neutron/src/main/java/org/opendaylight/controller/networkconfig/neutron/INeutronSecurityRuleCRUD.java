/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.List;

/**
 * This interface defines the methods for CRUD of NB OpenStack Security Rule objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronSecurityRuleCRUD}
 */

@Deprecated
public interface INeutronSecurityRuleCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Security Rule object exists
     *
     * @param uuid UUID of theSecurity Rule object
     * @return boolean
     */

    public boolean neutronSecurityRuleExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Security Rule object exists
     *
     * @param uuid UUID of the security rule object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronSecurityRule.OpenStackNetworks}
     * OpenStackSecurity Rule class
     */

    public NeutronSecurityRule getNeutronSecurityRule(String uuid);

    /**
     * Applications call this interface method to return all Security Rule objects
     *
     * @return List of OpenStack SecurityRules objects
     */

    public List<NeutronSecurityRule> getAllNeutronSecurityRules();

    /**
     * Applications call this interface method to add a Security Rule object to the
     * concurrent map
     *
     * @param input OpenStack security rule object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronSecurityRule(NeutronSecurityRule input);

    /**
     * Applications call this interface method to remove a Neutron Security Rule object to the
     * concurrent map
     *
     * @param uuid identifier for the security rule object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronSecurityRule(String uuid);

    /**
     * Applications call this interface method to edit aSecurity Rule object
     *
     * @param uuid  identifier of the security rule object
     * @param delta OpenStackSecurity Rule object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronSecurityRule(String uuid, NeutronSecurityRule delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid identifier of the security rule object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronSecurityRuleInUse(String uuid);

}
