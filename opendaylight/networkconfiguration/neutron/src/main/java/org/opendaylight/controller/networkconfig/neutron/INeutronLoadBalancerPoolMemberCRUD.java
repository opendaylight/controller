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
 * This interface defines the methods for CRUD of NB OpenStack INeutronLoadBalancerPoolMemberCRUD objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerPoolMemberCRUD}
 */

@Deprecated
public interface INeutronLoadBalancerPoolMemberCRUD {

    /**
     * Applications call this interface method to determine if a particular
     *NeutronLoadBalancerPoolMember object exists
     *
     * @param uuid
     *            UUID of the NeutronLoadBalancerPoolMember object
     * @return boolean
     */

    public boolean neutronLoadBalancerPoolMemberExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * NeutronLoadBalancerPoolMember object exists
     *
     * @param uuid
     *            UUID of the NeutronLoadBalancerPoolMember object
     * @return {@link org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember}
     *          OpenStackNeutronLoadBalancerPoolMember class
     */

    public NeutronLoadBalancerPoolMember getNeutronLoadBalancerPoolMember(String uuid);

    /**
     * Applications call this interface method to return all NeutronLoadBalancerPoolMember objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronLoadBalancerPoolMember> getAllNeutronLoadBalancerPoolMembers();

    /**
     * Applications call this interface method to add a NeutronLoadBalancerPoolMember object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember input);

    /**
     * Applications call this interface method to remove a Neutron NeutronLoadBalancerPoolMember object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the NeutronLoadBalancerPoolMember object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronLoadBalancerPoolMember(String uuid);

    /**
     * Applications call this interface method to edit a NeutronLoadBalancerPoolMember object
     *
     * @param uuid
     *            identifier of the NeutronLoadBalancerPoolMember object
     * @param delta
     *            OpenStackNeutronLoadBalancerPoolMember object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronLoadBalancerPoolMember(String uuid, NeutronLoadBalancerPoolMember delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the NeutronLoadBalancerPoolMember object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronLoadBalancerPoolMemberInUse(String uuid);

}