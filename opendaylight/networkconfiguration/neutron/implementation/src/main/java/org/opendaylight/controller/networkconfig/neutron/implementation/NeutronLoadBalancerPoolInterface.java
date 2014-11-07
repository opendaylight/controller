/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NeutronLoadBalancerPoolInterface implements INeutronLoadBalancerPoolCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancerPoolInterface.class);
    private ConcurrentMap<String, NeutronLoadBalancerPool> loadBalancerPoolDB = new ConcurrentHashMap<String, NeutronLoadBalancerPool>();


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
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean neutronLoadBalancerPoolExists(String uuid) {
        return loadBalancerPoolDB.containsKey(uuid);
    }

    @Override
    public NeutronLoadBalancerPool getNeutronLoadBalancerPool(String uuid) {
        if (!neutronLoadBalancerPoolExists(uuid)) {
            logger.debug("No LoadBalancerPool has Been Defined");
            return null;
        }
        return loadBalancerPoolDB.get(uuid);
    }

    @Override
    public List<NeutronLoadBalancerPool> getAllNeutronLoadBalancerPools() {
        Set<NeutronLoadBalancerPool> allLoadBalancerPools = new HashSet<NeutronLoadBalancerPool>();
        for (Entry<String, NeutronLoadBalancerPool> entry : loadBalancerPoolDB.entrySet()) {
            NeutronLoadBalancerPool loadBalancerPool = entry.getValue();
            allLoadBalancerPools.add(loadBalancerPool);
        }
        logger.debug("Exiting getLoadBalancerPools, Found {} OpenStackLoadBalancerPool", allLoadBalancerPools.size());
        List<NeutronLoadBalancerPool> ans = new ArrayList<NeutronLoadBalancerPool>();
        ans.addAll(allLoadBalancerPools);
        return ans;
    }

    @Override
    public boolean addNeutronLoadBalancerPool(NeutronLoadBalancerPool input) {
        if (neutronLoadBalancerPoolExists(input.getLoadBalancerPoolID())) {
            return false;
        }
        loadBalancerPoolDB.putIfAbsent(input.getLoadBalancerPoolID(), input);
        //TODO: add code to find INeutronLoadBalancerPoolAware services and call newtorkCreated on them
        return true;
    }

    @Override
    public boolean removeNeutronLoadBalancerPool(String uuid) {
        if (!neutronLoadBalancerPoolExists(uuid)) {
            return false;
        }
        loadBalancerPoolDB.remove(uuid);
        //TODO: add code to find INeutronLoadBalancerPoolAware services and call newtorkDeleted on them
        return true;
    }

    @Override
    public boolean updateNeutronLoadBalancerPool(String uuid, NeutronLoadBalancerPool delta) {
        if (!neutronLoadBalancerPoolExists(uuid)) {
            return false;
        }
        NeutronLoadBalancerPool target = loadBalancerPoolDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronLoadBalancerPoolInUse(String loadBalancerPoolUUID) {
        return !neutronLoadBalancerPoolExists(loadBalancerPoolUUID);
    }

}
