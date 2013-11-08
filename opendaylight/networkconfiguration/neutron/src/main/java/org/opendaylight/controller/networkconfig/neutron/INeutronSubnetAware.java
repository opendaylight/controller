/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods a service that wishes to be aware of Neutron Subnets needs to implement
 *
 */

public interface INeutronSubnetAware {

    /**
     * Services provide this interface method to indicate if the specified subnet can be created
     *
     * @param subnet
     *            instance of proposed new Neutron Subnet object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canCreateSubnet(NeutronSubnet subnet);

    /**
     * Services provide this interface method for taking action after a subnet has been created
     *
     * @param subnet
     *            instance of new Neutron Subnet object
     * @return void
     */
    public void neutronSubnetCreated(NeutronSubnet subnet);

    /**
     * Services provide this interface method to indicate if the specified subnet can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the subnet object using patch semantics
     * @param subnet
     *            instance of the Neutron Subnet object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canUpdateSubnet(NeutronSubnet delta, NeutronSubnet original);

    /**
     * Services provide this interface method for taking action after a subnet has been updated
     *
     * @param subnet
     *            instance of modified Neutron Subnet object
     * @return void
     */
    public void neutronSubnetUpdated(NeutronSubnet subnet);

    /**
     * Services provide this interface method to indicate if the specified subnet can be deleted
     *
     * @param subnet
     *            instance of the Subnet Router object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canDeleteSubnet(NeutronSubnet subnet);

    /**
     * Services provide this interface method for taking action after a subnet has been deleted
     *
     * @param subnet
     *            instance of deleted Router Subnet object
     * @return void
     */
    public void neutronSubnetDeleted(NeutronSubnet subnet);

}
