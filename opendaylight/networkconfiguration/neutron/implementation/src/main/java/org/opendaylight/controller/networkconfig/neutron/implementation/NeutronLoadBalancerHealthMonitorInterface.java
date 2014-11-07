/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerHealthMonitorCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerHealthMonitor;
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

public class NeutronLoadBalancerHealthMonitorInterface implements INeutronLoadBalancerHealthMonitorCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancerHealthMonitorInterface.class);
    private ConcurrentMap<String, NeutronLoadBalancerHealthMonitor> loadBalancerHealthMonitorDB = new ConcurrentHashMap<String, NeutronLoadBalancerHealthMonitor>();


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
    public boolean neutronLoadBalancerHealthMonitorExists(String uuid) {
        return loadBalancerHealthMonitorDB.containsKey(uuid);
    }

    @Override
    public NeutronLoadBalancerHealthMonitor getNeutronLoadBalancerHealthMonitor(String uuid) {
        if (!neutronLoadBalancerHealthMonitorExists(uuid)) {
            logger.debug("No LoadBalancerHealthMonitor has Been Defined");
            return null;
        }
        return loadBalancerHealthMonitorDB.get(uuid);
    }

    @Override
    public List<NeutronLoadBalancerHealthMonitor> getAllNeutronLoadBalancerHealthMonitors() {
        Set<NeutronLoadBalancerHealthMonitor> allLoadBalancerHealthMonitors = new HashSet<NeutronLoadBalancerHealthMonitor>();
        for (Entry<String, NeutronLoadBalancerHealthMonitor> entry : loadBalancerHealthMonitorDB.entrySet()) {
            NeutronLoadBalancerHealthMonitor loadBalancerHealthMonitor = entry.getValue();
            allLoadBalancerHealthMonitors.add(loadBalancerHealthMonitor);
        }
        logger.debug("Exiting getLoadBalancerHealthMonitors, Found {} OpenStackLoadBalancerHealthMonitor", allLoadBalancerHealthMonitors.size());
        List<NeutronLoadBalancerHealthMonitor> ans = new ArrayList<NeutronLoadBalancerHealthMonitor>();
        ans.addAll(allLoadBalancerHealthMonitors);
        return ans;
    }

    @Override
    public boolean addNeutronLoadBalancerHealthMonitor(NeutronLoadBalancerHealthMonitor input) {
        if (neutronLoadBalancerHealthMonitorExists(input.getLoadBalancerHealthMonitorID())) {
            return false;
        }
        loadBalancerHealthMonitorDB.putIfAbsent(input.getLoadBalancerHealthMonitorID(), input);
        //TODO: add code to find INeutronLoadBalancerHealthMonitorAware services and call newtorkCreated on them
        return true;
    }

    @Override
    public boolean removeNeutronLoadBalancerHealthMonitor(String uuid) {
        if (!neutronLoadBalancerHealthMonitorExists(uuid)) {
            return false;
        }
        loadBalancerHealthMonitorDB.remove(uuid);
        //TODO: add code to find INeutronLoadBalancerHealthMonitorAware services and call newtorkDeleted on them
        return true;
    }

    @Override
    public boolean updateNeutronLoadBalancerHealthMonitor(String uuid, NeutronLoadBalancerHealthMonitor delta) {
        if (!neutronLoadBalancerHealthMonitorExists(uuid)) {
            return false;
        }
        NeutronLoadBalancerHealthMonitor target = loadBalancerHealthMonitorDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronLoadBalancerHealthMonitorInUse(String loadBalancerHealthMonitorUUID) {
        return !neutronLoadBalancerHealthMonitorExists(loadBalancerHealthMonitorUUID);
    }
}
