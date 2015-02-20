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
 * This interface defines the methods for CRUD of NB OpenStack Security Group objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronSecurityGroupCRUD}
 */

@Deprecated
public interface INeutronSecurityGroupCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Security Group object exists
     *
     * @param uuid UUID of the Security Group object
     * @return boolean
     */

    public boolean neutronSecurityGroupExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Security Group object exists
     *
     * @param uuid UUID of the Security Group object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup.OpenStackSecurity Groups}
     * OpenStack Security Group class
     */

    public NeutronSecurityGroup getNeutronSecurityGroup(String uuid);

    /**
     * Applications call this interface method to return all Security Group objects
     *
     * @return List of OpenStackSecurity Groups objects
     */

    public List<NeutronSecurityGroup> getAllNeutronSecurityGroups();

    /**
     * Applications call this interface method to add a Security Group object to the
     * concurrent map
     *
     * @param input OpenStackSecurity Group object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronSecurityGroup(NeutronSecurityGroup input);

    /**
     * Applications call this interface method to remove a Neutron Security Group object to the
     * concurrent map
     *
     * @param uuid identifier for the security group object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronSecurityGroup(String uuid);

    /**
     * Applications call this interface method to edit a Security Group object
     *
     * @param uuid  identifier of the security group object
     * @param delta OpenStackSecurity Group object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronSecurityGroup(String uuid, NeutronSecurityGroup delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid identifier of the security group object
     * @return boolean on whether the Security Groups is already in use
     */

    public boolean neutronSecurityGroupInUse(String uuid);

}
