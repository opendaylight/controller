/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods a service that wishes to be aware of LoadBalancerListener Rules needs to implement
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerListenerAware}
 */

@Deprecated
public interface INeutronLoadBalancerListenerAware {

    /**
     * Services provide this interface method to indicate if the specified loadBalancerListener can be created
     *
     * @param loadBalancerListener
     *            instance of proposed new LoadBalancerListener object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canCreateNeutronLoadBalancerListener(NeutronLoadBalancerListener loadBalancerListener);

    /**
     * Services provide this interface method for taking action after a loadBalancerListener has been created
     *
     * @param loadBalancerListener
     *            instance of new LoadBalancerListener object
     * @return void
     */
    public void neutronLoadBalancerListenerCreated(NeutronLoadBalancerListener loadBalancerListener);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerListener can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the loadBalancerListener object using patch semantics
     * @param original
     *            instance of the LoadBalancerListener object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canUpdateNeutronLoadBalancerListener(NeutronLoadBalancerListener delta,
            NeutronLoadBalancerListener original);

    /**
     * Services provide this interface method for taking action after a loadBalancerListener has been updated
     *
     * @param loadBalancerListener
     *            instance of modified LoadBalancerListener object
     * @return void
     */
    public void neutronLoadBalancerListenerUpdated(NeutronLoadBalancerListener loadBalancerListener);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerListener can be deleted
     *
     * @param loadBalancerListener
     *            instance of the LoadBalancerListener object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canDeleteNeutronLoadBalancerListener(NeutronLoadBalancerListener loadBalancerListener);

    /**
     * Services provide this interface method for taking action after a loadBalancerListener has been deleted
     *
     * @param loadBalancerListener
     *            instance of deleted LoadBalancerListener object
     * @return void
     */
    public void neutronLoadBalancerListenerDeleted(NeutronLoadBalancerListener loadBalancerListener);
}
