/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NeutronSecurityGroupInterface implements INeutronSecurityGroupCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronSecurityGroupInterface.class);
    private ConcurrentMap<String, NeutronSecurityGroup> securityGroupDB  = new ConcurrentHashMap<String, NeutronSecurityGroup>();



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

    @Override
    public boolean neutronSecurityGroupExists(String uuid) {
        return securityGroupDB.containsKey(uuid);
    }

    @Override
    public NeutronSecurityGroup getNeutronSecurityGroup(String uuid) {
        if (!neutronSecurityGroupExists(uuid)) {
            logger.debug("No Security Groups Have Been Defined");
            return null;
        }
        return securityGroupDB.get(uuid);
    }

    @Override
    public List<NeutronSecurityGroup> getAllNeutronSecurityGroups() {
        Set<NeutronSecurityGroup> allSecurityGroups = new HashSet<NeutronSecurityGroup>();
        for (Entry<String, NeutronSecurityGroup> entry : securityGroupDB.entrySet()) {
            NeutronSecurityGroup securityGroup = entry.getValue();
            allSecurityGroups.add(securityGroup);
        }
        logger.debug("Exiting getSecurityGroups, Found {} OpenStackSecurityGroup", allSecurityGroups.size());
        List<NeutronSecurityGroup> ans = new ArrayList<NeutronSecurityGroup>();
        ans.addAll(allSecurityGroups);
        return ans;
    }

    @Override
    public boolean addNeutronSecurityGroup(NeutronSecurityGroup input) {
        if (neutronSecurityGroupExists(input.getSecurityGroupUUID())) {
            return false;
        }
        securityGroupDB.putIfAbsent(input.getSecurityGroupUUID(), input);
        return true;
    }

    @Override
    public boolean removeNeutronSecurityGroup(String uuid) {
        if (!neutronSecurityGroupExists(uuid)) {
            return false;
        }
        securityGroupDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateNeutronSecurityGroup(String uuid, NeutronSecurityGroup delta) {
        if (!neutronSecurityGroupExists(uuid)) {
            return false;
        }
        NeutronSecurityGroup target = securityGroupDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronSecurityGroupInUse(String securityGroupUUID) {
        return !neutronSecurityGroupExists(securityGroupUUID);
    }

}