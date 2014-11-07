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

import org.opendaylight.controller.networkconfig.neutron.INeutronRouterCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronRouterInterface implements INeutronRouterCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronRouterInterface.class);
    private ConcurrentMap<String, NeutronRouter> routerDB  = new ConcurrentHashMap<String, NeutronRouter>();
    // methods needed for creating caches



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


    // IfNBRouterCRUD Interface methods

    @Override
    public boolean routerExists(String uuid) {
        return routerDB.containsKey(uuid);
    }

    @Override
    public NeutronRouter getRouter(String uuid) {
        if (!routerExists(uuid)) {
            return null;
        }
        return routerDB.get(uuid);
    }

    @Override
    public List<NeutronRouter> getAllRouters() {
        Set<NeutronRouter> allRouters = new HashSet<NeutronRouter>();
        for (Entry<String, NeutronRouter> entry : routerDB.entrySet()) {
            NeutronRouter router = entry.getValue();
            allRouters.add(router);
        }
        logger.debug("Exiting getAllRouters, Found {} Routers", allRouters.size());
        List<NeutronRouter> ans = new ArrayList<NeutronRouter>();
        ans.addAll(allRouters);
        return ans;
    }

    @Override
    public boolean addRouter(NeutronRouter input) {
        if (routerExists(input.getID())) {
            return false;
        }
        routerDB.putIfAbsent(input.getID(), input);
        return true;
    }

    @Override
    public boolean removeRouter(String uuid) {
        if (!routerExists(uuid)) {
            return false;
        }
        routerDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateRouter(String uuid, NeutronRouter delta) {
        if (!routerExists(uuid)) {
            return false;
        }
        NeutronRouter target = routerDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean routerInUse(String routerUUID) {
        if (!routerExists(routerUUID)) {
            return true;
        }
        NeutronRouter target = routerDB.get(routerUUID);
        return (target.getInterfaces().size() > 0);
    }

}
