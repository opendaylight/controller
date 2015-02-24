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
 * This interface defines the methods for CRUD of NB OpenStack LoadBalancerPool objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerPoolCRUD}
 */

@Deprecated
public interface INeutronLoadBalancerPoolCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *LoadBalancerPool object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerPool object
     * @return boolean
     */

    public boolean neutronLoadBalancerPoolExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * LoadBalancerPool object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerPool object
     * @return {@link NeutronLoadBalancerPool}
     *          OpenStackLoadBalancerPool class
     */

    public NeutronLoadBalancerPool getNeutronLoadBalancerPool(String uuid);

    /**
     * Applications call this interface method to return all LoadBalancerPool objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronLoadBalancerPool> getAllNeutronLoadBalancerPools();

    /**
     * Applications call this interface method to add a LoadBalancerPool object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronLoadBalancerPool(NeutronLoadBalancerPool input);

    /**
     * Applications call this interface method to remove a Neutron LoadBalancerPool object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the LoadBalancerPool object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronLoadBalancerPool(String uuid);

    /**
     * Applications call this interface method to edit a LoadBalancerPool object
     *
     * @param uuid
     *            identifier of the LoadBalancerPool object
     * @param delta
     *            OpenStackLoadBalancerPool object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronLoadBalancerPool(String uuid, NeutronLoadBalancerPool delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the LoadBalancerPool object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronLoadBalancerPoolInUse(String uuid);

}