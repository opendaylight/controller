/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods a service that wishes to be aware of Neutron SecurityGroups needs to implement
 *
 */

public interface INeutronSecurityGroupAware {

    /**
     * Services provide this interface method to indicate if the specified SecurityGroup can be created
     *
     * @param secGroup
     *            instance of proposed new Neutron SecurityGroup object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canCreateSecurityGroup(NeutronSecurityGroup secGroup);

    /**
     * Services provide this interface method for taking action after a SecurityGroup has been created
     *
     * @param secGroup
     *            instance of new Neutron SecurityGroup object
     * @return void
     */
    public void neutronSecurityGroupCreated(NeutronSecurityGroup secGroup);

    /**
     * Services provide this interface method to indicate if the specified SecurityGroup can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the Neutron SecurityGroup object using patch semantics
     * @param original
     *            instance of the Neutron SecurityGroup object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canUpdateSecurityGroup(NeutronSecurityGroup delta, NeutronSecurityGroup original);

    /**
     * Services provide this interface method for taking action after a SecurityGroup has been updated
     *
     * @param secGroup
     *            instance of modified Neutron SecurityGroup object
     * @return void
     */
    public void neutronSecurityGroupUpdated(NeutronSecurityGroup secGroup);

    /**
     * Services provide this interface method to indicate if the specified SecurityGroup can be deleted
     *
     * @param secGroup
     *            instance of the Neutron SecurityGroup object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canDeleteSecurityGroup(NeutronSecurityGroup secGroup);

    /**
     * Services provide this interface method for taking action after a SecurityGroup has been deleted
     *
     * @param secGroup
     *            instance of deleted Neutron SecurityGroup object
     * @return void
     */
    public void neutronSecurityGroupDeleted(NeutronSecurityGroup secGroup);

    /**
     * Services provide this interface method to indicate if the specified SecurityGroupRule can be added
     *
     * @param secGroup
     *            instance of the Neutron SecurityGroup object
     * @param secGroupRule
     *            instance of the new Neutron SecurityGroupRule object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canAddSecurityGroupRule(NeutronSecurityGroup secGroup,
                                       NeutronSecurityGroupRule secGroupRule);

    /**
     * Services provide this interface method for taking action after a SecurityGroupRule has been added
     *
     * @param secGroup
     *            instance of the Neutron SecurityGroup object
     * @param secGroup
     *            instance of the Neutron SecurityGroup object
     * @param secGroupRule
     *            instance of the new Neutron SecurityGroupRule object
     * @return void
     */
    public void neutronSecurityGroupRuleAdded(NeutronSecurityGroup secGroup,
                                              NeutronSecurityGroupRule secGroupRule);

    /**
     * Services provide this interface method to indicate if the specified SecurityGroupRule can be updated
     *
     * @param secGroup
     *            instance of the Neutron SecurityGroup object
     * @param delta
     *            updates to the Neutron SecurityGroupRule object using patch semantics
     * @param original
     *            instance of the Neutron SecurityGroupRule object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canUpdateSecurityGroupRule(NeutronSecurityGroup secGroup,
                                          NeutronSecurityGroupRule delta,
                                          NeutronSecurityGroupRule original);

    /**
     * Services provide this interface method for taking action after a SecurityGroupRule has been updated
     *
     * @param secGroupRule
     *            instance of the new Neutron SecurityGroupRule object
     * @return void
     */
    public void neutronSecurityGroupRuleUpdated(NeutronSecurityGroup secGroup,
                                                NeutronSecurityGroupRule secGroupRule);

    /**
     * Services provide this interface method to indicate if the specified SecurityGroupRule can be removed
     *
     * @param secGroup
     *            instance of the Neutron SecurityGroup object
     * @param secGroupRule
     *            instance of the new Neutron SecurityGroupRule object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    public int canRemoveSecurityGroupRule(NeutronSecurityGroup secGroup,
                                          NeutronSecurityGroupRule secGroupRule);

    /**
     * Services provide this interface method for taking action after a SecurityGroupRule has been deleted
     *
     * @param secGroup
     *            instance of the Neutron SecurityGroup object
     * @param secGroupRule
     *            instance of the new Neutron SecurityGroupRule object
     * @return void
     */
    public void neutronSecurityGroupRuleRemoved(NeutronSecurityGroup secGroup,
                                                NeutronSecurityGroupRule secGroupRule);
}
