/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerListenerCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerListener;
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

public class NeutronLoadBalancerListenerInterface implements INeutronLoadBalancerListenerCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancerListenerInterface.class);
    private ConcurrentMap<String, NeutronLoadBalancerListener> loadBalancerListenerDB  = new ConcurrentHashMap<String, NeutronLoadBalancerListener>();



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
    public boolean neutronLoadBalancerListenerExists(String uuid) {
        return loadBalancerListenerDB.containsKey(uuid);
    }

    @Override
    public NeutronLoadBalancerListener getNeutronLoadBalancerListener(String uuid) {
        if (!neutronLoadBalancerListenerExists(uuid)) {
            logger.debug("No LoadBalancerListener Have Been Defined");
            return null;
        }
        return loadBalancerListenerDB.get(uuid);
    }

    @Override
    public List<NeutronLoadBalancerListener> getAllNeutronLoadBalancerListeners() {
        Set<NeutronLoadBalancerListener> allLoadBalancerListeners = new HashSet<NeutronLoadBalancerListener>();
        for (Entry<String, NeutronLoadBalancerListener> entry : loadBalancerListenerDB.entrySet()) {
            NeutronLoadBalancerListener loadBalancerListener = entry.getValue();
            allLoadBalancerListeners.add(loadBalancerListener);
        }
        logger.debug("Exiting getLoadBalancerListeners, Found {} OpenStackLoadBalancerListener", allLoadBalancerListeners.size());
        List<NeutronLoadBalancerListener> ans = new ArrayList<NeutronLoadBalancerListener>();
        ans.addAll(allLoadBalancerListeners);
        return ans;
    }

    @Override
    public boolean addNeutronLoadBalancerListener(NeutronLoadBalancerListener input) {
        if (neutronLoadBalancerListenerExists(input.getLoadBalancerListenerID())) {
            return false;
        }
        loadBalancerListenerDB.putIfAbsent(input.getLoadBalancerListenerID(), input);
        //TODO: add code to find INeutronLoadBalancerListenerAware services and call newtorkCreated on them
        return true;
    }

    @Override
    public boolean removeNeutronLoadBalancerListener(String uuid) {
        if (!neutronLoadBalancerListenerExists(uuid)) {
            return false;
        }
        loadBalancerListenerDB.remove(uuid);
        //TODO: add code to find INeutronLoadBalancerListenerAware services and call newtorkDeleted on them
        return true;
    }

    @Override
    public boolean updateNeutronLoadBalancerListener(String uuid, NeutronLoadBalancerListener delta) {
        if (!neutronLoadBalancerListenerExists(uuid)) {
            return false;
        }
        NeutronLoadBalancerListener target = loadBalancerListenerDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronLoadBalancerListenerInUse(String loadBalancerListenerUUID) {
        return !neutronLoadBalancerListenerExists(loadBalancerListenerUUID);
    }
}
