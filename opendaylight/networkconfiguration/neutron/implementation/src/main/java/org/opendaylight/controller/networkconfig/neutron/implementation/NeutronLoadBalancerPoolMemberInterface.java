/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NeutronLoadBalancerPoolMemberInterface
        implements INeutronLoadBalancerPoolMemberCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancerPoolMemberInterface.class);
    private ConcurrentMap<String, NeutronLoadBalancerPoolMember> loadBalancerPoolMemberDB  = new ConcurrentHashMap<String, NeutronLoadBalancerPoolMember>();


    // this method uses reflection to update an object from it's delta.

    private boolean overwrite(Object target, Object delta) {
        Method[] methods = target.getClass().getMethods();

        for (Method toMethod : methods) {
            if (toMethod.getDeclaringClass().equals(target.getClass())
                    && toMethod.getName().startsWith("set")) {

                String toName = toMethod.getName();
                String fromName = toName.replace("set", "get");

                try {
                    Method fromMethod = delta.getClass().getMethod(fromName);
                    Object value = fromMethod.invoke(delta, (Object[]) null);
                    if (value != null) {
                        toMethod.invoke(target, value);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean neutronLoadBalancerPoolMemberExists(String uuid) {
        return loadBalancerPoolMemberDB.containsKey(uuid);
    }

    @Override
    public NeutronLoadBalancerPoolMember getNeutronLoadBalancerPoolMember(String uuid) {
        if (!neutronLoadBalancerPoolMemberExists(uuid)) {
            logger.debug("No LoadBalancerPoolMember Have Been Defined");
            return null;
        }
        return loadBalancerPoolMemberDB.get(uuid);
    }

    @Override
    public List<NeutronLoadBalancerPoolMember> getAllNeutronLoadBalancerPoolMembers() {
        Set<NeutronLoadBalancerPoolMember> allLoadBalancerPoolMembers = new HashSet<NeutronLoadBalancerPoolMember>();
        for (Map.Entry<String, NeutronLoadBalancerPoolMember> entry : loadBalancerPoolMemberDB.entrySet()) {
            NeutronLoadBalancerPoolMember loadBalancerPoolMember = entry.getValue();
            allLoadBalancerPoolMembers.add(loadBalancerPoolMember);
        }
        logger.debug("Exiting getLoadBalancerPoolMembers, Found {} OpenStackLoadBalancerPoolMember",
                allLoadBalancerPoolMembers.size());
        List<NeutronLoadBalancerPoolMember> ans = new ArrayList<NeutronLoadBalancerPoolMember>();
        ans.addAll(allLoadBalancerPoolMembers);
        return ans;
    }

    @Override
    public boolean addNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember input) {
        if (neutronLoadBalancerPoolMemberExists(input.getPoolMemberID())) {
            return false;
        }
        loadBalancerPoolMemberDB.putIfAbsent(input.getPoolMemberID(), input);
        return true;
    }

    @Override
    public boolean removeNeutronLoadBalancerPoolMember(String uuid) {
        if (!neutronLoadBalancerPoolMemberExists(uuid)) {
            return false;
        }
        loadBalancerPoolMemberDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateNeutronLoadBalancerPoolMember(String uuid, NeutronLoadBalancerPoolMember delta) {
        if (!neutronLoadBalancerPoolMemberExists(uuid)) {
            return false;
        }
        NeutronLoadBalancerPoolMember target = loadBalancerPoolMemberDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronLoadBalancerPoolMemberInUse(String loadBalancerPoolMemberID) {
        return !neutronLoadBalancerPoolMemberExists(loadBalancerPoolMemberID);
    }
}
