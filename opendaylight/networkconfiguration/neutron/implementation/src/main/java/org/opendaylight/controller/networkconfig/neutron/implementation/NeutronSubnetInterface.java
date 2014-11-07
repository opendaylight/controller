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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronSubnetInterface implements INeutronSubnetCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronSubnetInterface.class);
    private ConcurrentMap<String, NeutronSubnet> subnetDB  = new ConcurrentHashMap<String, NeutronSubnet>();



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


    // IfNBSubnetCRUD methods

    @Override
    public boolean subnetExists(String uuid) {
        return subnetDB.containsKey(uuid);
    }

    @Override
    public NeutronSubnet getSubnet(String uuid) {
        if (!subnetExists(uuid)) {
            return null;
        }
        return subnetDB.get(uuid);
    }

    @Override
    public List<NeutronSubnet> getAllSubnets() {
        Set<NeutronSubnet> allSubnets = new HashSet<NeutronSubnet>();
        for (Entry<String, NeutronSubnet> entry : subnetDB.entrySet()) {
            NeutronSubnet subnet = entry.getValue();
            allSubnets.add(subnet);
        }
        logger.debug("Exiting getAllSubnets, Found {} OpenStackSubnets", allSubnets.size());
        List<NeutronSubnet> ans = new ArrayList<NeutronSubnet>();
        ans.addAll(allSubnets);
        return ans;
    }

    @Override
    public boolean addSubnet(NeutronSubnet input) {
        String id = input.getID();
        if (subnetExists(id)) {
            return false;
        }
        subnetDB.putIfAbsent(id, input);
        INeutronNetworkCRUD networkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);

        NeutronNetwork targetNet = networkIf.getNetwork(input.getNetworkUUID());
        targetNet.addSubnet(id);
        return true;
    }

    @Override
    public boolean removeSubnet(String uuid) {
        if (!subnetExists(uuid)) {
            return false;
        }
        NeutronSubnet target = subnetDB.get(uuid);
        INeutronNetworkCRUD networkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);

        NeutronNetwork targetNet = networkIf.getNetwork(target.getNetworkUUID());
        targetNet.removeSubnet(uuid);
        subnetDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateSubnet(String uuid, NeutronSubnet delta) {
        if (!subnetExists(uuid)) {
            return false;
        }
        NeutronSubnet target = subnetDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean subnetInUse(String subnetUUID) {
        if (!subnetExists(subnetUUID)) {
            return true;
        }
        NeutronSubnet target = subnetDB.get(subnetUUID);
        return (target.getPortsInSubnet().size() > 0);
    }
}
