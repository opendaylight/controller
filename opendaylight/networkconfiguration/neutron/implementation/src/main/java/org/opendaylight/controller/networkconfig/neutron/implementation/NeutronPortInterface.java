/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronPortInterface implements INeutronPortCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronPortInterface.class);
    private ConcurrentMap<String, NeutronPort> portDB = new ConcurrentHashMap<String, NeutronPort>();



    // this method uses reflection to update an object from it's delta.

    private boolean overwrite(Object target, Object delta) {
        Method[] methods = target.getClass().getMethods();

        for(Method toMethod: methods){
            if(toMethod.getDeclaringClass().equals(target.getClass())
                    && toMethod.getName().startsWith("set")){

                String toName = toMethod.getName();
                String fromName = toName.replace("set", "get");

                try {
                    Method fromMethod = delta.getClass().getMethod(fromName);
                    Object value = fromMethod.invoke(delta, (Object[])null);
                    if(value != null){
                        toMethod.invoke(target, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    // IfNBPortCRUD methods

    @Override
    public boolean portExists(String uuid) {
        return portDB.containsKey(uuid);
    }

    @Override
    public NeutronPort getPort(String uuid) {
        if (!portExists(uuid)) {
            return null;
        }
        return portDB.get(uuid);
    }

    @Override
    public List<NeutronPort> getAllPorts() {
        Set<NeutronPort> allPorts = new HashSet<NeutronPort>();
        for (Entry<String, NeutronPort> entry : portDB.entrySet()) {
            NeutronPort port = entry.getValue();
            allPorts.add(port);
        }
        logger.debug("Exiting getAllPorts, Found {} OpenStackPorts", allPorts.size());
        List<NeutronPort> ans = new ArrayList<NeutronPort>();
        ans.addAll(allPorts);
        return ans;
    }

    @Override
    public boolean addPort(NeutronPort input) {
        if (portExists(input.getID())) {
            return false;
        }
        portDB.putIfAbsent(input.getID(), input);
        // if there are no fixed IPs, allocate one for each subnet in the network
        INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        if (input.getFixedIPs() == null){
           input.setFixedIPs(new ArrayList<Neutron_IPs>());
        }
        if (input.getFixedIPs().size() == 0) {
            List<Neutron_IPs> list = input.getFixedIPs();
            Iterator<NeutronSubnet> subnetIterator = systemCRUD.getAllSubnets().iterator();
            while (subnetIterator.hasNext()) {
                NeutronSubnet subnet = subnetIterator.next();
                if (subnet.getNetworkUUID().equals(input.getNetworkUUID())) {
                    list.add(new Neutron_IPs(subnet.getID()));
                }
            }
        }
        Iterator<Neutron_IPs> fixedIPIterator = input.getFixedIPs().iterator();
        while (fixedIPIterator.hasNext()) {
            Neutron_IPs ip = fixedIPIterator.next();
            NeutronSubnet subnet = systemCRUD.getSubnet(ip.getSubnetUUID());
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

        NeutronNetwork network = networkIf.getNetwork(input.getNetworkUUID());
        network.addPort(input);
        return true;
    }

    @Override
    public boolean removePort(String uuid) {
        if (!portExists(uuid)) {
            return false;
        }
        NeutronPort port = getPort(uuid);
        portDB.remove(uuid);
        INeutronNetworkCRUD networkCRUD = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);

        NeutronNetwork network = networkCRUD.getNetwork(port.getNetworkUUID());
        network.removePort(port);
        Iterator<Neutron_IPs> fixedIPIterator = port.getFixedIPs().iterator();
        while (fixedIPIterator.hasNext()) {
            Neutron_IPs ip = fixedIPIterator.next();
            NeutronSubnet subnet = systemCRUD.getSubnet(ip.getSubnetUUID());
            if (!ip.getIpAddress().equals(subnet.getGatewayIP())) {
                subnet.releaseIP(ip.getIpAddress());
            }
            else {
                subnet.resetGatewayIPAllocated();
            }
            subnet.removePort(port);
        }
        return true;
    }

    @Override
    public boolean updatePort(String uuid, NeutronPort delta) {
        if (!portExists(uuid)) {
            return false;
        }
        NeutronPort target = portDB.get(uuid);
        // remove old Fixed_IPs
        if (delta.getFixedIPs() != null) {
            NeutronPort port = getPort(uuid);
            INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
            for (Neutron_IPs ip: port.getFixedIPs()) {
                NeutronSubnet subnet = systemCRUD.getSubnet(ip.getSubnetUUID());
                subnet.releaseIP(ip.getIpAddress());
            }

            // allocate new Fixed_IPs
            for (Neutron_IPs ip: delta.getFixedIPs()) {
                NeutronSubnet subnet = systemCRUD.getSubnet(ip.getSubnetUUID());
                if (ip.getIpAddress() == null) {
                    ip.setIpAddress(subnet.getLowAddr());
                }
                subnet.allocateIP(ip.getIpAddress());
            }
        }
        return overwrite(target, delta);
    }

    @Override
    public boolean macInUse(String macAddress) {
        List<NeutronPort> ports = getAllPorts();
        Iterator<NeutronPort> portIterator = ports.iterator();
        while (portIterator.hasNext()) {
            NeutronPort port = portIterator.next();
            if (macAddress.equalsIgnoreCase(port.getMacAddress())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NeutronPort getGatewayPort(String subnetUUID) {
        INeutronSubnetCRUD systemCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        NeutronSubnet subnet = systemCRUD.getSubnet(subnetUUID);
        Iterator<NeutronPort> portIterator = getAllPorts().iterator();
        while (portIterator.hasNext()) {
            NeutronPort port = portIterator.next();
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
