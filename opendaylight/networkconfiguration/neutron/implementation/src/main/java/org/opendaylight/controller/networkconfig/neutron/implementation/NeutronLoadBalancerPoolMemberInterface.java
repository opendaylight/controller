/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.ConfigurationObject;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class NeutronLoadBalancerPoolMemberInterface
        implements INeutronLoadBalancerPoolMemberCRUD, IConfigurationContainerAware,
        IObjectReader {
    private static final Logger logger = LoggerFactory.getLogger(NeutronLoadBalancerPoolMemberInterface.class);
    private static final String FILE_NAME = "neutron.loadBalancerPoolMember.conf";
    private String containerName = null;

    private IClusterContainerServices clusterContainerService = null;
    private IConfigurationContainerService configurationService;
    private ConcurrentMap<String, NeutronLoadBalancerPoolMember> loadBalancerPoolMemberDB;

    // methods needed for creating caches
    void setClusterContainerService(IClusterContainerServices s) {
        logger.debug("Cluster Service set");
        clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (clusterContainerService == s) {
            logger.debug("Cluster Service removed!");
            clusterContainerService = null;
        }
    }

    public void setConfigurationContainerService(IConfigurationContainerService service) {
        logger.trace("Configuration service set: {}", service);
        configurationService = service;
    }

    public void unsetConfigurationContainerService(IConfigurationContainerService service) {
        logger.trace("Configuration service removed: {}", service);
        configurationService = null;
    }

    private void allocateCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't create cache");
            return;
        }
        logger.debug("Creating Cache for Neutron LoadBalancerPoolMember");
        try {
            // neutron caches
            this.clusterContainerService.createCache("neutronLoadBalancerPoolMembers",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch(CacheConfigException cce) {
            logger.error("Cache couldn't be created for Neutron LoadBalancerPoolMember -  check cache mode");
        } catch(CacheExistException cce) {
            logger.error("Cache for Neutron LoadBalancerPoolMember already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for Neutron LoadBalancerPoolMember");
    }

    @SuppressWarnings({"unchecked"})
    private void retrieveCache() {
        if (clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        logger.debug("Retrieving cache for Neutron LoadBalancerPoolMember");
        loadBalancerPoolMemberDB = (ConcurrentMap<String, NeutronLoadBalancerPoolMember>) clusterContainerService
                .getCache("neutronLoadBalancerPoolMembers");
        if (loadBalancerPoolMemberDB == null) {
            logger.error("Cache couldn't be retrieved for Neutron LoadBalancerPoolMember");
        }
        logger.debug("Cache was successfully retrieved for Neutron LoadBalancerPoolMember");
    }

    private void destroyCache() {
        if (clusterContainerService == null) {
            logger.error("un-initialized clusterMger, can't destroy cache");
            return;
        }
        logger.debug("Destroying Cache for HostTracker");
        clusterContainerService.destroyCache("neutronLoadBalancerPoolMembers");
    }

    private void startUp() {
        allocateCache();
        retrieveCache();
        loadConfiguration();
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     */
    void init(Component c) {
        Dictionary<?, ?> props = c.getServiceProperties();
        if (props != null) {
            this.containerName = (String) props.get("containerName");
            logger.debug("Running containerName: {}", this.containerName);
        } else {
            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
        startUp();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     */
    void destroy() {
        destroyCache();
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     */
    void stop() {
    }

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

    private void loadConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, FILE_NAME)) {
            NeutronLoadBalancerPoolMember nn = (NeutronLoadBalancerPoolMember) conf;
            loadBalancerPoolMemberDB.put(nn.getPoolMemberID(), nn);
        }
    }

    @Override
    public Status saveConfiguration() {
        return configurationService.persistConfiguration(
                new ArrayList<ConfigurationObject>(loadBalancerPoolMemberDB.values()),
                FILE_NAME);
    }

    @Override
    public Object readObject(ObjectInputStream ois) throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }
}
