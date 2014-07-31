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
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewall;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class NeutronFirewallInterface implements INeutronFirewallCRUD, IConfigurationContainerAware, IObjectReader {
    private static final Logger logger = LoggerFactory.getLogger(NeutronFirewallInterface.class);
    private static final String FILE_NAME = "neutron.firewall.conf";
    private String containerName = null;

    private IClusterContainerServices clusterContainerService = null;
    private IConfigurationContainerService configurationService;
    private ConcurrentMap<String, NeutronFirewall> firewallDB;

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
        logger.debug("Creating Cache for Neutron Firewall");
        try {
            // neutron caches
            this.clusterContainerService.createCache("neutronFirewalls",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache couldn't be created for Neutron Firewall -  check cache mode");
        } catch (CacheExistException cce) {
            logger.error("Cache for Neutron Firewall already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for Neutron Firewall");
    }

    @SuppressWarnings ({"unchecked"})
    private void retrieveCache() {
        if (clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        logger.debug("Retrieving cache for Neutron Firewall");
        firewallDB = (ConcurrentMap<String, NeutronFirewall>) clusterContainerService
                .getCache("neutronFirewalls");
        if (firewallDB == null) {
            logger.error("Cache couldn't be retrieved for Neutron Firewall");
        }
        logger.debug("Cache was successfully retrieved for Neutron Firewall");
    }

    private void destroyCache() {
        if (clusterContainerService == null) {
            logger.error("un-initialized clusterMger, can't destroy cache");
            return;
        }
        logger.debug("Destroying Cache for HostTracker");
        clusterContainerService.destroyCache("neutronFirewalls");
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
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean neutronFirewallExists(String uuid) {
        return firewallDB.containsKey(uuid);
    }

    @Override
    public NeutronFirewall getNeutronFirewall(String uuid) {
        if (!neutronFirewallExists(uuid)) {
            logger.debug("No Firewall Have Been Defined");
            return null;
        }
        return firewallDB.get(uuid);
    }

    @Override
    public List<NeutronFirewall> getAllNeutronFirewalls() {
        Set<NeutronFirewall> allFirewalls = new HashSet<NeutronFirewall>();
        for (Entry<String, NeutronFirewall> entry : firewallDB.entrySet()) {
            NeutronFirewall firewall = entry.getValue();
            allFirewalls.add(firewall);
        }
        logger.debug("Exiting getFirewalls, Found {} OpenStackFirewall", allFirewalls.size());
        List<NeutronFirewall> ans = new ArrayList<NeutronFirewall>();
        ans.addAll(allFirewalls);
        return ans;
    }

    @Override
    public boolean addNeutronFirewall(NeutronFirewall input) {
        if (neutronFirewallExists(input.getFirewallUUID())) {
            return false;
        }
        firewallDB.putIfAbsent(input.getFirewallUUID(), input);
        return true;
    }

    @Override
    public boolean removeNeutronFirewall(String uuid) {
        if (!neutronFirewallExists(uuid)) {
            return false;
        }
        firewallDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateNeutronFirewall(String uuid, NeutronFirewall delta) {
        if (!neutronFirewallExists(uuid)) {
            return false;
        }
        NeutronFirewall target = firewallDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronFirewallInUse(String firewallUUID) {
        return !neutronFirewallExists(firewallUUID);
    }

    private void loadConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, FILE_NAME)) {
            NeutronFirewall nn = (NeutronFirewall) conf;
            firewallDB.put(nn.getFirewallUUID(), nn);
        }
    }

    @Override
    public Status saveConfiguration() {
        return configurationService.persistConfiguration(new ArrayList<ConfigurationObject>(firewallDB.values()),
                FILE_NAME);
    }

    @Override
    public Object readObject(ObjectInputStream ois) throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }

}