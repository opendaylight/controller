/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods a service that wishes to be aware of LoadBalancerHealthMonitor Rules needs to implement
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerHealthMonitorAware}
 */

@Deprecated
public interface INeutronLoadBalancerHealthMonitorAware {

    /**
     * Services provide this interface method to indicate if the specified loadBalancerHealthMonitor can be created
     *
     * @param loadBalancerHealthMonitor
     *            instance of proposed new LoadBalancerHealthMonitor object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canCreateNeutronLoadBalancerHealthMonitor(NeutronLoadBalancerHealthMonitor loadBalancerHealthMonitor);

    /**
     * Services provide this interface method for taking action after a loadBalancerHealthMonitor has been created
     *
     * @param loadBalancerHealthMonitor
     *            instance of new LoadBalancerHealthMonitor object
     * @return void
     */
    public void neutronLoadBalancerHealthMonitorCreated(NeutronLoadBalancerHealthMonitor loadBalancerHealthMonitor);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerHealthMonitor can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the loadBalancerHealthMonitor object using patch semantics
     * @param original
     *            instance of the LoadBalancerHealthMonitor object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canUpdateNeutronLoadBalancerHealthMonitor(NeutronLoadBalancerHealthMonitor delta,
            NeutronLoadBalancerHealthMonitor original);

    /**
     * Services provide this interface method for taking action after a loadBalancerHealthMonitor has been updated
     *
     * @param loadBalancerHealthMonitor
     *            instance of modified LoadBalancerHealthMonitor object
     * @return void
     */
    public void neutronLoadBalancerHealthMonitorUpdated(NeutronLoadBalancerHealthMonitor loadBalancerHealthMonitor);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerHealthMonitor can be deleted
     *
     * @param loadBalancerHealthMonitor
     *            instance of the LoadBalancerHealthMonitor object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canDeleteNeutronLoadBalancerHealthMonitor(NeutronLoadBalancerHealthMonitor loadBalancerHealthMonitor);

    /**
     * Services provide this interface method for taking action after a loadBalancerHealthMonitor has been deleted
     *
     * @param loadBalancerHealthMonitor
     *            instance of deleted LoadBalancerHealthMonitor object
     * @return void
     */
    public void neutronLoadBalancerHealthMonitorDeleted(NeutronLoadBalancerHealthMonitor loadBalancerHealthMonitor);
}
