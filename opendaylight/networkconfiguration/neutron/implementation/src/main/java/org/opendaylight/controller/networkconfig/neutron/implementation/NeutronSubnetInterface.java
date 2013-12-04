/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronSubnetInterface extends AbstractNeutronInterface<NeutronSubnet>
                                    implements INeutronSubnetCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronSubnetInterface.class);

    @Override
    protected String getCacheName() {
        return "neutronSubnets";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public boolean add(NeutronSubnet input) {
        if (!super.add(input)) {
            return false;
        }

        INeutronNetworkCRUD networkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);

        NeutronNetwork targetNet = networkIf.get(input.getNetworkUUID());
        targetNet.addSubnet(input.getID());
        return true;
    }

    @Override
    public boolean remove(String uuid) {
        if (!super.remove(uuid)) {
            return false;
        }

        NeutronSubnet target = get(uuid);
        INeutronNetworkCRUD networkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);

        NeutronNetwork targetNet = networkIf.get(target.getNetworkUUID());
        targetNet.removeSubnet(uuid);
        return true;
    }

    public boolean subnetInUse(String subnetUUID) {
        NeutronSubnet target = get(subnetUUID);
        return target != null && target.getPortsInSubnet().size() > 0;
    }
}
