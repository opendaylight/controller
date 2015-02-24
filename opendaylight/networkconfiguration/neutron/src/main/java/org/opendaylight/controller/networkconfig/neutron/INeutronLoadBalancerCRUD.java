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
 * This interface defines the methods for CRUD of NB OpenStack LoadBalancer objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerCRUD}
 */

@Deprecated
public interface INeutronLoadBalancerCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *LoadBalancer object exists
     *
     * @param uuid
     *            UUID of the LoadBalancer object
     * @return boolean
     */

    public boolean neutronLoadBalancerExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * LoadBalancer object exists
     *
     * @param uuid
     *            UUID of the LoadBalancer object
     * @return {@link NeutronLoadBalancer}
     *          OpenStackLoadBalancer class
     */

    public NeutronLoadBalancer getNeutronLoadBalancer(String uuid);

    /**
     * Applications call this interface method to return all LoadBalancer objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronLoadBalancer> getAllNeutronLoadBalancers();

    /**
     * Applications call this interface method to add a LoadBalancer object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronLoadBalancer(NeutronLoadBalancer input);

    /**
     * Applications call this interface method to remove a Neutron LoadBalancer object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the LoadBalancer object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronLoadBalancer(String uuid);

    /**
     * Applications call this interface method to edit a LoadBalancer object
     *
     * @param uuid
     *            identifier of the LoadBalancer object
     * @param delta
     *            OpenStackLoadBalancer object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronLoadBalancer(String uuid, NeutronLoadBalancer delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the LoadBalancer object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronLoadBalancerInUse(String uuid);

}