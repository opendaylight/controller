/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronFloatingIPInterface extends AbstractNeutronInterface<NeutronFloatingIP>
                                        implements INeutronFloatingIPCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronFloatingIPInterface.class);

    @Override
    protected String getCacheName() {
        return "neutronFloatingIPs";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public boolean add(NeutronFloatingIP input) {
        if (!exists(input.getID()))
            return false;

        INeutronNetworkCRUD networkCRUD = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        INeutronSubnetCRUD subnetCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        INeutronPortCRUD portCRUD = NeutronCRUDInterfaces.getINeutronPortCRUD(this);

        //if floating_ip_address isn't there, allocate from the subnet pool
        NeutronSubnet subnet = subnetCRUD.get(networkCRUD.get(input.getFloatingNetworkUUID()).getSubnets().get(0));
        if (input.getFloatingIPAddress() == null)
            input.setFloatingIPAddress(subnet.getLowAddr());
        subnet.allocateIP(input.getFloatingIPAddress());

        //if port_id is there, bind port to this floating ip
        if (input.getPortUUID() != null) {
            NeutronPort port = portCRUD.get(input.getPortUUID());
            port.addFloatingIP(input.getFixedIPAddress(), input);
        }

        db.putIfAbsent(input.getID(), input);
        return true;
    }

    @Override
    public boolean remove(String uuid) {
        INeutronNetworkCRUD networkCRUD = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        INeutronSubnetCRUD subnetCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        INeutronPortCRUD portCRUD = NeutronCRUDInterfaces.getINeutronPortCRUD(this);

        if (!exists(uuid))
            return false;

        NeutronFloatingIP floatIP = get(uuid);
        //if floating_ip_address isn't there, allocate from the subnet pool
        NeutronSubnet subnet = subnetCRUD.get(networkCRUD.get(floatIP.getFloatingNetworkUUID()).getSubnets().get(0));
        subnet.releaseIP(floatIP.getFloatingIPAddress());
        if (floatIP.getPortUUID() != null) {
            NeutronPort port = portCRUD.get(floatIP.getPortUUID());
            port.removeFloatingIP(floatIP.getFixedIPAddress());
        }
        db.remove(uuid);
        return true;
    }

    @Override
    public boolean update(String uuid, NeutronFloatingIP delta) {
        INeutronPortCRUD portCRUD = NeutronCRUDInterfaces.getINeutronPortCRUD(this);

        if (!exists(uuid))
            return false;

        NeutronFloatingIP target = db.get(uuid);
        if (target.getPortUUID() != null) {
            NeutronPort port = portCRUD.get(target.getPortUUID());
            port.removeFloatingIP(target.getFixedIPAddress());
        }

        //if port_id is there, bind port to this floating ip
        if (delta.getPortUUID() != null) {
            NeutronPort port = portCRUD.get(delta.getPortUUID());
            port.addFloatingIP(delta.getFixedIPAddress(), delta);
        }

        target.setPortUUID(delta.getPortUUID());
        target.setFixedIPAddress(delta.getFixedIPAddress());
        return true;
    }
}
