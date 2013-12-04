/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods for CRUD of NB Subnet objects
 *
 */

public interface INeutronSubnetCRUD extends INeutronCRUD<NeutronSubnet> {

    /**
     * Applications call this interface method to determine if a Subnet object
     * is use
     *
     * @param subnetUUID
     *            identifier of the subnet object
     *
     * @return boolean on whether the subnet is in use or not
     */

    public boolean subnetInUse(String subnetUUID);
}
