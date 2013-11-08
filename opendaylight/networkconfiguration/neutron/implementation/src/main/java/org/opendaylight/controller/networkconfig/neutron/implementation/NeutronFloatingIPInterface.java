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
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronFloatingIPInterface implements INeutronFloatingIPCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronFloatingIPInterface.class);
    private String containerName = null;

    private IClusterContainerServices clusterContainerService = null;
    private ConcurrentMap<String, NeutronFloatingIP> floatingIPDB;

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
        logger.debug("Creating Cache for Neutron FloatingIPs");
        try {
            // neutron caches
            this.clusterContainerService.createCache("neutronFloatingIPs",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache couldn't be created for Neutron -  check cache mode");
        } catch (CacheExistException cce) {
            logger.error("Cache for Neutron already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for NeutronFloatingIps");
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        logger.debug("Retrieving cache for Neutron FloatingIPs");
        floatingIPDB = (ConcurrentMap<String, NeutronFloatingIP>) this.clusterContainerService
        .getCache("neutronFloatingIPs");
        if (floatingIPDB == null) {
            logger.error("Cache couldn't be retrieved for Neutron FloatingIPs");
        }
        logger.debug("Cache was successfully retrieved for Neutron FloatingIPs");
    }

    @SuppressWarnings("deprecation")
    private void destroyCache() {
        if (this.clusterContainerService == null) {
            logger.error("un-initialized clusterMger, can't destroy cache");
            return;
        }
        logger.debug("Destroying Cache for HostTracker");
        this.clusterContainerService.destroyCache("neutronFloatingIPs");
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

    // IfNBFloatingIPCRUD interface methods

    public boolean floatingIPExists(String uuid) {
        return floatingIPDB.containsKey(uuid);
    }

    public NeutronFloatingIP getFloatingIP(String uuid) {
        if (!floatingIPExists(uuid))
            return null;
        return floatingIPDB.get(uuid);
    }

    public List<NeutronFloatingIP> getAllFloatingIPs() {
        Set<NeutronFloatingIP> allIPs = new HashSet<NeutronFloatingIP>();
        for (Entry<String, NeutronFloatingIP> entry : floatingIPDB.entrySet()) {
            NeutronFloatingIP floatingip = entry.getValue();
            allIPs.add(floatingip);
        }
        logger.debug("Exiting getAllFloatingIPs, Found {} FloatingIPs", allIPs.size());
        List<NeutronFloatingIP> ans = new ArrayList<NeutronFloatingIP>();
        ans.addAll(allIPs);
        return ans;
    }

    public boolean addFloatingIP(NeutronFloatingIP input) {
        INeutronNetworkCRUD networkCRUD = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        INeutronSubnetCRUD subnetCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        INeutronPortCRUD portCRUD = NeutronCRUDInterfaces.getINeutronPortCRUD(this);

        if (floatingIPExists(input.getID()))
            return false;
        //if floating_ip_address isn't there, allocate from the subnet pool
        NeutronSubnet subnet = subnetCRUD.getSubnet(networkCRUD.getNetwork(input.getFloatingNetworkUUID()).getSubnets().get(0));
        if (input.getFloatingIPAddress() == null)
            input.setFloatingIPAddress(subnet.getLowAddr());
        subnet.allocateIP(input.getFloatingIPAddress());

        //if port_id is there, bind port to this floating ip
        if (input.getPortUUID() != null) {
            NeutronPort port = portCRUD.getPort(input.getPortUUID());
            port.addFloatingIP(input.getFixedIPAddress(), input);
        }

        floatingIPDB.putIfAbsent(input.getID(), input);
        return true;
    }

    public boolean removeFloatingIP(String uuid) {
        INeutronNetworkCRUD networkCRUD = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        INeutronSubnetCRUD subnetCRUD = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        INeutronPortCRUD portCRUD = NeutronCRUDInterfaces.getINeutronPortCRUD(this);

        if (!floatingIPExists(uuid))
            return false;
        NeutronFloatingIP floatIP = getFloatingIP(uuid);
        //if floating_ip_address isn't there, allocate from the subnet pool
        NeutronSubnet subnet = subnetCRUD.getSubnet(networkCRUD.getNetwork(floatIP.getFloatingNetworkUUID()).getSubnets().get(0));
        subnet.releaseIP(floatIP.getFloatingIPAddress());
        if (floatIP.getPortUUID() != null) {
            NeutronPort port = portCRUD.getPort(floatIP.getPortUUID());
            port.removeFloatingIP(floatIP.getFixedIPAddress());
        }
        floatingIPDB.remove(uuid);
        return true;
    }

    public boolean updateFloatingIP(String uuid, NeutronFloatingIP delta) {
        INeutronPortCRUD portCRUD = NeutronCRUDInterfaces.getINeutronPortCRUD(this);

        if (!floatingIPExists(uuid))
            return false;
        NeutronFloatingIP target = floatingIPDB.get(uuid);
        if (target.getPortUUID() != null) {
            NeutronPort port = portCRUD.getPort(target.getPortUUID());
            port.removeFloatingIP(target.getFixedIPAddress());
        }

        //if port_id is there, bind port to this floating ip
        if (delta.getPortUUID() != null) {
            NeutronPort port = portCRUD.getPort(delta.getPortUUID());
            port.addFloatingIP(delta.getFixedIPAddress(), delta);
        }

        target.setPortUUID(delta.getPortUUID());
        target.setFixedIPAddress(delta.getFixedIPAddress());
        return true;
    }
}
