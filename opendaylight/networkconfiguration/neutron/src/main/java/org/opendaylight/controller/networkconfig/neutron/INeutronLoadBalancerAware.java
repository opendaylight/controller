/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods a service that wishes to be aware of LoadBalancer Rules needs to implement
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronLoadBalancerAware}
 */

@Deprecated
public interface INeutronLoadBalancerAware {

    /**
     * Services provide this interface method to indicate if the specified loadBalancer can be created
     *
     * @param loadBalancer
     *            instance of proposed new LoadBalancer object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canCreateNeutronLoadBalancer(NeutronLoadBalancer loadBalancer);

    /**
     * Services provide this interface method for taking action after a loadBalancer has been created
     *
     * @param loadBalancer
     *            instance of new LoadBalancer object
     * @return void
     */
    public void neutronLoadBalancerCreated(NeutronLoadBalancer loadBalancer);

    /**
     * Services provide this interface method to indicate if the specified loadBalancer can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the loadBalancer object using patch semantics
     * @param original
     *            instance of the LoadBalancer object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canUpdateNeutronLoadBalancer(NeutronLoadBalancer delta, NeutronLoadBalancer original);

    /**
     * Services provide this interface method for taking action after a loadBalancer has been updated
     *
     * @param loadBalancer
     *            instance of modified LoadBalancer object
     * @return void
     */
    public void neutronLoadBalancerUpdated(NeutronLoadBalancer loadBalancer);

    /**
     * Services provide this interface method to indicate if the specified loadBalancer can be deleted
     *
     * @param loadBalancer
     *            instance of the LoadBalancer object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canDeleteNeutronLoadBalancer(NeutronLoadBalancer loadBalancer);

    /**
     * Services provide this interface method for taking action after a loadBalancer has been deleted
     *
     * @param loadBalancer
     *            instance of deleted LoadBalancer object
     * @return void
     */
    public void neutronLoadBalancerDeleted(NeutronLoadBalancer loadBalancer);
}
