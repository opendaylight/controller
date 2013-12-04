/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This interface defines the methods for CRUD of NB Port objects
 *
 */

public interface INeutronPortCRUD extends INeutronCRUD<NeutronPort> {

    public boolean macInUse(String macAddress);

    /**
     * Applications call this interface method to retrieve the port associated with
     * the gateway address of a subnet
     *
     * @param subnetUUID
     *            identifier of the subnet
     * @return OpenStackPorts object if the port exists and null if it does not
     */

    public NeutronPort getGatewayPort(String subnetUUID);
}
