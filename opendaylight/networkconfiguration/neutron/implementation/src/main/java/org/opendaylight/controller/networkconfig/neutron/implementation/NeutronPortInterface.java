/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.util.List;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronPortInterface extends AbstractNeutronInterface<NeutronPort>
                                  implements INeutronPortCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronPortInterface.class);

    @Override
    protected String getCacheName() {
        return "neutronPorts";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public boolean add(NeutronPort input) {
        if (!super.add(input)) {
            return false;
        }

        // if there are no fixed IPs, allocate one for each subnet in the network
        INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        if (input.getFixedIPs().size() == 0) {
            List<Neutron_IPs> list = input.getFixedIPs();
            for (NeutronSubnet subnet : systemCRUD.getAll()) {
                if (subnet.getNetworkUUID().equals(input.getNetworkUUID())) {
                    list.add(new Neutron_IPs(subnet.getID()));
                }
            }
        }
        for (Neutron_IPs ip : input.getFixedIPs()) {
            NeutronSubnet subnet = systemCRUD.get(ip.getSubnetUUID());
            if (ip.getIpAddress() == null) {
                ip.setIpAddress(subnet.getLowAddr());
            }
            if (!ip.getIpAddress().equals(subnet.getGatewayIP())) {
                subnet.allocateIP(ip.getIpAddress());
            }
            else {
                subnet.setGatewayIPAllocated();
            }
            subnet.addPort(input);
        }
        INeutronNetworkCRUD networkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        NeutronNetwork network = networkIf.get(input.getNetworkUUID());
        network.addPort(input);

        addPortToSecurityGroups(input, input.getSecurityGroups(),
                                NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this));
        return true;
    }

    @Override
    public boolean remove(String uuid) {
        if (!exists(uuid)) {
            return false;
        }
        NeutronPort port = get(uuid);
        db.remove(uuid);
        INeutronNetworkCRUD networkCRUD = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);

        NeutronNetwork network = networkCRUD.get(port.getNetworkUUID());
        network.removePort(port);
        for (Neutron_IPs ip : port.getFixedIPs()) {
            NeutronSubnet subnet = systemCRUD.get(ip.getSubnetUUID());
            if (!ip.getIpAddress().equals(subnet.getGatewayIP())) {
                subnet.releaseIP(ip.getIpAddress());
            } else {
                subnet.resetGatewayIPAllocated();
            }
            subnet.removePort(port);
        }

        removePortFromSecurityGroups(port, port.getSecurityGroups(),
                                     NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this));
        return true;
    }

    @Override
    public boolean update(String uuid, NeutronPort delta) {
        if (!exists(uuid)) {
            return false;
        }
        NeutronPort target = db.get(uuid);
        // remove old Fixed_IPs
        if (delta.getFixedIPs() != null) {
            NeutronPort port = get(uuid);
            INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
            for (Neutron_IPs ip: port.getFixedIPs()) {
                NeutronSubnet subnet = systemCRUD.get(ip.getSubnetUUID());
                subnet.releaseIP(ip.getIpAddress());
            }

            // allocate new Fixed_IPs
            for (Neutron_IPs ip: delta.getFixedIPs()) {
                NeutronSubnet subnet = systemCRUD.get(ip.getSubnetUUID());
                if (ip.getIpAddress() == null) {
                    ip.setIpAddress(subnet.getLowAddr());
                }
                subnet.allocateIP(ip.getIpAddress());
            }
        }

        INeutronSecurityGroupCRUD secGroupCRUD =
                NeutronCRUDInterfaces.getNeutronSecurityGroupCRUD(this);
        NeutronPortSecurityGroupDiff diff =
                new NeutronPortSecurityGroupDiff(target.getSecurityGroups(),
                                                 delta.getSecurityGroups());

        addPortToSecurityGroups(target, diff.getJoining(), secGroupCRUD);
        removePortFromSecurityGroups(target, diff.getLeaving(), secGroupCRUD);

        return overwrite(target, delta);
    }

    private static void addPortToSecurityGroups(NeutronPort port,
                                                Iterable<String> secGroups,
                                                INeutronSecurityGroupCRUD secGroupCRUD) {
        for (String uuid : secGroups)
            secGroupCRUD.get(uuid).addPort(port);
    }

    private static void removePortFromSecurityGroups(NeutronPort port,
                                                     Iterable<String> secGroups,
                                                     INeutronSecurityGroupCRUD secGroupCRUD) {
        for (String uuid : secGroups)
            secGroupCRUD.get(uuid).removePort(port);
    }

    @Override
    public boolean macInUse(String macAddress) {
        for (NeutronPort port : getAll()) {
            if (macAddress.equalsIgnoreCase(port.getMacAddress())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NeutronPort getGatewayPort(String subnetUUID) {
        INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        NeutronSubnet subnet = systemCRUD.get(subnetUUID);

        for (NeutronPort port : getAll()) {
            List<Neutron_IPs> fixedIPs = port.getFixedIPs();
            if (fixedIPs.size() == 1) {
                if (subnet.getGatewayIP().equals(fixedIPs.get(0).getIpAddress())) {
                    return port;
                }
            }
        }
        return null;
    }
}
