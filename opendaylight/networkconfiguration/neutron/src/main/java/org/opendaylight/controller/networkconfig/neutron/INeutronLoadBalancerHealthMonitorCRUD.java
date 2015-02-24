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
 * This interface defines the methods for CRUD of NB OpenStack LoadBalancerHealthMonitor objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerHealthMonitorCRUD}
 */

@Deprecated
public interface INeutronLoadBalancerHealthMonitorCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *LoadBalancerHealthMonitor object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerHealthMonitor object
     * @return boolean
     */

    public boolean neutronLoadBalancerHealthMonitorExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * LoadBalancerHealthMonitor object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerHealthMonitor object
     * @return {@link NeutronLoadBalancerHealthMonitor}
     *          OpenStackLoadBalancerHealthMonitor class
     */

    public NeutronLoadBalancerHealthMonitor getNeutronLoadBalancerHealthMonitor(String uuid);

    /**
     * Applications call this interface method to return all LoadBalancerHealthMonitor objects
     *
     * @return List of OpenStackNetworks objects
     */

    public List<NeutronLoadBalancerHealthMonitor> getAllNeutronLoadBalancerHealthMonitors();

    /**
     * Applications call this interface method to add a LoadBalancerHealthMonitor object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    public boolean addNeutronLoadBalancerHealthMonitor(NeutronLoadBalancerHealthMonitor input);

    /**
     * Applications call this interface method to remove a Neutron LoadBalancerHealthMonitor object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the LoadBalancerHealthMonitor object
     * @return boolean on whether the object was removed or not
     */

    public boolean removeNeutronLoadBalancerHealthMonitor(String uuid);

    /**
     * Applications call this interface method to edit a LoadBalancerHealthMonitor object
     *
     * @param uuid
     *            identifier of the LoadBalancerHealthMonitor object
     * @param delta
     *            OpenStackLoadBalancerHealthMonitor object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    public boolean updateNeutronLoadBalancerHealthMonitor(String uuid, NeutronLoadBalancerHealthMonitor delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the LoadBalancerHealthMonitor object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    public boolean neutronLoadBalancerHealthMonitorInUse(String uuid);

}
