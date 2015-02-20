/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods a service that wishes to be aware of LoadBalancerPool Rules needs to implement
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerPoolAware}
 */

@Deprecated
public interface INeutronLoadBalancerPoolAware {

    /**
     * Services provide this interface method to indicate if the specified loadBalancerPool can be created
     *
     * @param loadBalancerPool
     *            instance of proposed new LoadBalancerPool object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canCreateNeutronLoadBalancerPool(NeutronLoadBalancerPool loadBalancerPool);

    /**
     * Services provide this interface method for taking action after a loadBalancerPool has been created
     *
     * @param loadBalancerPool
     *            instance of new LoadBalancerPool object
     * @return void
     */
    public void neutronLoadBalancerPoolCreated(NeutronLoadBalancerPool loadBalancerPool);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerPool can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the loadBalancerPool object using patch semantics
     * @param original
     *            instance of the LoadBalancerPool object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canUpdateNeutronLoadBalancerPool(NeutronLoadBalancerPool delta, NeutronLoadBalancerPool original);

    /**
     * Services provide this interface method for taking action after a loadBalancerPool has been updated
     *
     * @param loadBalancerPool
     *            instance of modified LoadBalancerPool object
     * @return void
     */
    public void neutronLoadBalancerPoolUpdated(NeutronLoadBalancerPool loadBalancerPool);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerPool can be deleted
     *
     * @param loadBalancerPool
     *            instance of the LoadBalancerPool object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canDeleteNeutronLoadBalancerPool(NeutronLoadBalancerPool loadBalancerPool);

    /**
     * Services provide this interface method for taking action after a loadBalancerPool has been deleted
     *
     * @param loadBalancerPool
     *            instance of deleted LoadBalancerPool object
     * @return void
     */
    public void neutronLoadBalancerPoolDeleted(NeutronLoadBalancerPool loadBalancerPool);
}
