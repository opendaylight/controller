package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods for CRUD of NB SecurityGroup objects
 *
 */

public interface INeutronSecurityGroupCRUD extends INeutronCRUD<NeutronSecurityGroup> {

    /**
     * Applications call this interface method to check if a SecurityGroup is in use
     *
     * @param uuid
     *            identifier of the SecurityGroup object
     * @return boolean on whether the SecurityGroup is in use or not
     */

    public boolean securityGroupInUse(String uuid);
}
