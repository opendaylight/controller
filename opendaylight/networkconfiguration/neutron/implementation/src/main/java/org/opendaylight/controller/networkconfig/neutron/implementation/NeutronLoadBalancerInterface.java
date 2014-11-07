/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;
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

public class NeutronLoadBalancerInterface implements INeutronLoadBalancerCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancerInterface.class);
    private ConcurrentMap<String, NeutronLoadBalancer> loadBalancerDB  = new ConcurrentHashMap<String, NeutronLoadBalancer>();


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
    public boolean neutronLoadBalancerExists(String uuid) {
        return loadBalancerDB.containsKey(uuid);
    }

    @Override
    public NeutronLoadBalancer getNeutronLoadBalancer(String uuid) {
        if (!neutronLoadBalancerExists(uuid)) {
            logger.debug("No LoadBalancer Have Been Defined");
            return null;
        }
        return loadBalancerDB.get(uuid);
    }

    @Override
    public List<NeutronLoadBalancer> getAllNeutronLoadBalancers() {
        Set<NeutronLoadBalancer> allLoadBalancers = new HashSet<NeutronLoadBalancer>();
        for (Entry<String, NeutronLoadBalancer> entry : loadBalancerDB.entrySet()) {
            NeutronLoadBalancer loadBalancer = entry.getValue();
            allLoadBalancers.add(loadBalancer);
        }
        logger.debug("Exiting getLoadBalancers, Found {} OpenStackLoadBalancer", allLoadBalancers.size());
        List<NeutronLoadBalancer> ans = new ArrayList<NeutronLoadBalancer>();
        ans.addAll(allLoadBalancers);
        return ans;
    }

    @Override
    public boolean addNeutronLoadBalancer(NeutronLoadBalancer input) {
        if (neutronLoadBalancerExists(input.getLoadBalancerID())) {
            return false;
        }
        loadBalancerDB.putIfAbsent(input.getLoadBalancerID(), input);
        //TODO: add code to find INeutronLoadBalancerAware services and call newtorkCreated on them
        return true;
    }

    @Override
    public boolean removeNeutronLoadBalancer(String uuid) {
        if (!neutronLoadBalancerExists(uuid)) {
            return false;
        }
        loadBalancerDB.remove(uuid);
        //TODO: add code to find INeutronLoadBalancerAware services and call newtorkDeleted on them
        return true;
    }

    @Override
    public boolean updateNeutronLoadBalancer(String uuid, NeutronLoadBalancer delta) {
        if (!neutronLoadBalancerExists(uuid)) {
            return false;
        }
        NeutronLoadBalancer target = loadBalancerDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronLoadBalancerInUse(String loadBalancerUUID) {
        return !neutronLoadBalancerExists(loadBalancerUUID);
    }

}
