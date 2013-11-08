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
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronSubnetInterface implements INeutronSubnetCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronSubnetInterface.class);
    private String containerName = null;

    private IClusterContainerServices clusterContainerService = null;
    private ConcurrentMap<String, NeutronSubnet> subnetDB;

    // methods needed for creating caches

    void setClusterContainerService(IClusterContainerServices s) {
        logger.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            logger.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void allocateCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't create cache");
            return;
        }
        logger.debug("Creating Cache for Neutron Subnets");
        try {
            // neutron caches
            this.clusterContainerService.createCache("neutronSubnets",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache couldn't be created for Neutron Subnets -  check cache mode");
        } catch (CacheExistException cce) {
            logger.error("Cache for Neutron Subnets already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for Neutron Subnets");
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        logger.debug("Retrieving cache for Neutron Subnets");
        subnetDB = (ConcurrentMap<String, NeutronSubnet>) this.clusterContainerService
        .getCache("neutronSubnets");
        if (subnetDB == null) {
            logger.error("Cache couldn't be retrieved for Neutron Subnets");
        }
        logger.debug("Cache was successfully retrieved for Neutron Subnets");
    }

    @SuppressWarnings("deprecation")
    private void destroyCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterMger, can't destroy cache");
            return;
        }
        logger.debug("Destroying Cache for HostTracker");
        this.clusterContainerService.destroyCache("neutronSubnets");
    }

    private void startUp() {
        allocateCache();
        retrieveCache();
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
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
     *
     */
    void destroy() {
        destroyCache();
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

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

    public boolean subnetExists(String uuid) {
        return subnetDB.containsKey(uuid);
    }

    public NeutronSubnet getSubnet(String uuid) {
        if (!subnetExists(uuid))
            return null;
        return subnetDB.get(uuid);
    }

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

    public boolean addSubnet(NeutronSubnet input) {
        String id = input.getID();
        if (subnetExists(id))
            return false;
        subnetDB.putIfAbsent(id, input);
        INeutronNetworkCRUD networkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);

        NeutronNetwork targetNet = networkIf.getNetwork(input.getNetworkUUID());
        targetNet.addSubnet(id);
        return true;
    }

    public boolean removeSubnet(String uuid) {
        if (!subnetExists(uuid))
            return false;
        NeutronSubnet target = subnetDB.get(uuid);
        INeutronNetworkCRUD networkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);

        NeutronNetwork targetNet = networkIf.getNetwork(target.getNetworkUUID());
        targetNet.removeSubnet(uuid);
        subnetDB.remove(uuid);
        return true;
    }

    public boolean updateSubnet(String uuid, NeutronSubnet delta) {
        if (!subnetExists(uuid))
            return false;
        NeutronSubnet target = subnetDB.get(uuid);
        return overwrite(target, delta);
    }

    public boolean subnetInUse(String subnetUUID) {
        if (!subnetExists(subnetUUID))
            return true;
        NeutronSubnet target = subnetDB.get(subnetUUID);
        return (target.getPortsInSubnet().size() > 0);
    }
}
