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
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronNetworkInterface implements INeutronNetworkCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronNetworkInterface.class);
    private ConcurrentMap<String, NeutronNetwork> networkDB = new ConcurrentHashMap<String, NeutronNetwork>();




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

    // IfNBNetworkCRUD methods

    @Override
    public boolean networkExists(String uuid) {
        return networkDB.containsKey(uuid);
    }

    @Override
    public NeutronNetwork getNetwork(String uuid) {
        if (!networkExists(uuid)) {
            return null;
        }
        return networkDB.get(uuid);
    }

    @Override
    public List<NeutronNetwork> getAllNetworks() {
        Set<NeutronNetwork> allNetworks = new HashSet<NeutronNetwork>();
        for (Entry<String, NeutronNetwork> entry : networkDB.entrySet()) {
            NeutronNetwork network = entry.getValue();
            allNetworks.add(network);
        }
        logger.debug("Exiting getAllNetworks, Found {} OpenStackNetworks", allNetworks.size());
        List<NeutronNetwork> ans = new ArrayList<NeutronNetwork>();
        ans.addAll(allNetworks);
        return ans;
    }

    @Override
    public boolean addNetwork(NeutronNetwork input) {
        if (networkExists(input.getID())) {
            return false;
        }
        networkDB.putIfAbsent(input.getID(), input);
      //TODO: add code to find INeutronNetworkAware services and call newtorkCreated on them
        return true;
    }

    @Override
    public boolean removeNetwork(String uuid) {
        if (!networkExists(uuid)) {
            return false;
        }
        networkDB.remove(uuid);
      //TODO: add code to find INeutronNetworkAware services and call newtorkDeleted on them
        return true;
    }

    @Override
    public boolean updateNetwork(String uuid, NeutronNetwork delta) {
        if (!networkExists(uuid)) {
            return false;
        }
        NeutronNetwork target = networkDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean networkInUse(String netUUID) {
        if (!networkExists(netUUID)) {
            return true;
        }
        NeutronNetwork target = networkDB.get(netUUID);
        if (target.getPortsOnNetwork().size() > 0) {
            return true;
        }
        return false;
    }
}
