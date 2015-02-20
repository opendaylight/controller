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
 * This interface defines the methods for CRUD of NB OpenStack LoadBalancerListener objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerListenerCRUD}
 */

@Deprecated
public interface INeutronLoadBalancerListenerCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *LoadBalancerListener object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerListener object
     * @return boolean
     */

    public boolean neutronLoadBalancerListenerExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * LoadBalancerListener object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerListener object
     * @return {@link NeutronLoadBalancerListener}
     *          OpenStackLoadBalancerListener class
     */

    public NeutronLoadBalancerListener getNeutronLoadBalancerListener(String uuid);

    /**
     * Applications call this interface method to return all LoadBalancerListener objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronLoadBalancerListener> getAllNeutronLoadBalancerListeners();

    /**
     * Applications call this interface method to add a LoadBalancerListener object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronLoadBalancerListener(NeutronLoadBalancerListener input);

    /**
     * Applications call this interface method to remove a Neutron LoadBalancerListener object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the LoadBalancerListener object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronLoadBalancerListener(String uuid);

    /**
     * Applications call this interface method to edit a LoadBalancerListener object
     *
     * @param uuid
     *            identifier of the LoadBalancerListener object
     * @param delta
     *            OpenStackLoadBalancerListener object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronLoadBalancerListener(String uuid, NeutronLoadBalancerListener delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the LoadBalancerListener object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronLoadBalancerListenerInUse(String uuid);

}