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
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronNetworkInterface implements INeutronNetworkCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronNetworkInterface.class);
    private String containerName = null;

    private ConcurrentMap<String, NeutronNetwork> networkDB;
    private IClusterContainerServices clusterContainerService = null;

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
        logger.debug("Creating Cache for Neutron Networks");
        try {
            // neutron caches
            this.clusterContainerService.createCache("neutronNetworks",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache couldn't be created for Neutron Networks -  check cache mode");
        } catch (CacheExistException cce) {
            logger.error("Cache for Neutron Networks already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for Neutron Networks");
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }
        logger.debug("Retrieving cache for Neutron Networks");
        networkDB = (ConcurrentMap<String, NeutronNetwork>) this.clusterContainerService.getCache("neutronNetworks");
        if (networkDB == null) {
            logger.error("Cache couldn't be retrieved for Neutron Networks");
        }
        logger.debug("Cache was successfully retrieved for Neutron Networks");
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

    @SuppressWarnings("deprecation")
    private void destroyCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterMger, can't destroy cache");
            return;
        }
        logger.debug("Destroying Cache for Neutron Networks");
        this.clusterContainerService.destroyCache("Neutron Networks");
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

    // IfNBNetworkCRUD methods

    public boolean networkExists(String uuid) {
        return networkDB.containsKey(uuid);
    }

    public NeutronNetwork getNetwork(String uuid) {
        if (!networkExists(uuid))
            return null;
        return networkDB.get(uuid);
    }

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

    public boolean addNetwork(NeutronNetwork input) {
        if (networkExists(input.getID()))
            return false;
        networkDB.putIfAbsent(input.getID(), input);
      //TODO: add code to find INeutronNetworkAware services and call newtorkCreated on them
        return true;
    }

    public boolean removeNetwork(String uuid) {
        if (!networkExists(uuid))
            return false;
        networkDB.remove(uuid);
      //TODO: add code to find INeutronNetworkAware services and call newtorkDeleted on them
        return true;
    }

    public boolean updateNetwork(String uuid, NeutronNetwork delta) {
        if (!networkExists(uuid))
            return false;
        NeutronNetwork target = networkDB.get(uuid);
        return overwrite(target, delta);
    }

    public boolean networkInUse(String netUUID) {
        if (!networkExists(netUUID))
            return true;
        NeutronNetwork target = networkDB.get(netUUID);
        if (target.getPortsOnNetwork().size() > 0)
            return true;
        return false;
    }
}
