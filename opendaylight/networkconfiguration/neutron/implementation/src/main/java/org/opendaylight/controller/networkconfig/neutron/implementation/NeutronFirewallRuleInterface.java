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
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewallRule;
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

public class NeutronFirewallRuleInterface implements INeutronFirewallRuleCRUD, IConfigurationContainerAware, IObjectReader {
    private static final Logger logger = LoggerFactory.getLogger(NeutronFirewallRuleInterface.class);
    private static final String FILE_NAME ="neutron.firewallrules.conf";
    private String containerName = null;

    private IClusterContainerServices clusterContainerService = null;
    private IConfigurationContainerService configurationService;
    private ConcurrentMap<String, NeutronFirewallRule> firewallRuleDB;

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
        logger.debug("Creating Cache for Neutron Firewall Rule");
        try {
            // neutron caches
            this.clusterContainerService.createCache("neutronFirewallRules",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Cache couldn't be created for Neutron Firewall Rule -  check cache mode");
        } catch (CacheExistException cce) {
            logger.error("Cache for Neutron Firewall Rule already exists, destroy and recreate");
        }
        logger.debug("Cache successfully created for Neutron Firewall Rule");
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCache() {
        if (clusterContainerService == null) {
            logger.error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        logger.debug("Retrieving cache for Neutron Firewall Rule");
        firewallRuleDB = (ConcurrentMap<String, NeutronFirewallRule>) clusterContainerService
                .getCache("neutronFirewallRules");
        if (firewallRuleDB == null) {
            logger.error("Cache couldn't be retrieved for Neutron Firewall Rule");
        }
        logger.debug("Cache was successfully retrieved for Neutron Firewall Rule");
    }

    private void destroyCache() {
        if (clusterContainerService == null) {
            logger.error("un-initialized clusterMger, can't destroy cache");
            return;
        }
        logger.debug("Destroying Cache for HostTracker");
        clusterContainerService.destroyCache("neutronFirewallRules");
    }

    private void startUp() {
        allocateCache();
        retrieveCache();
        loadConfiguration();
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

    @Override
    public boolean neutronFirewallRuleExists(String uuid) {
        return firewallRuleDB.containsKey(uuid);
    }

    @Override
    public NeutronFirewallRule getNeutronFirewallRule(String uuid) {
        if (!neutronFirewallRuleExists(uuid)) {
            logger.debug("No Firewall Rule Have Been Defined");
            return null;
        }
        return firewallRuleDB.get(uuid);
    }

    @Override
    public List<NeutronFirewallRule> getAllNeutronFirewallRules() {
        Set<NeutronFirewallRule> allFirewallRules = new HashSet<NeutronFirewallRule>();
        for (Entry<String, NeutronFirewallRule> entry : firewallRuleDB.entrySet()) {
            NeutronFirewallRule firewallRule = entry.getValue();
            allFirewallRules.add(firewallRule);
        }
        logger.debug("Exiting getFirewallRules, Found {} OpenStackFirewallRule", allFirewallRules.size());
        List<NeutronFirewallRule> ans = new ArrayList<NeutronFirewallRule>();
        ans.addAll(allFirewallRules);
        return ans;
    }

    @Override
    public boolean addNeutronFirewallRule(NeutronFirewallRule input) {
        if (neutronFirewallRuleExists(input.getFirewallRuleUUID())) {
            return false;
        }
        firewallRuleDB.putIfAbsent(input.getFirewallRuleUUID(), input);
        return true;
    }

    @Override
    public boolean removeNeutronFirewallRule(String uuid) {
        if (!neutronFirewallRuleExists(uuid)) {
            return false;
        }
        firewallRuleDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateNeutronFirewallRule(String uuid, NeutronFirewallRule delta) {
        if (!neutronFirewallRuleExists(uuid)) {
            return false;
        }
        NeutronFirewallRule target = firewallRuleDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronFirewallRuleInUse(String firewallRuleUUID) {
        return !neutronFirewallRuleExists(firewallRuleUUID);
    }

    private void loadConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, FILE_NAME)) {
            NeutronFirewallRule nn = (NeutronFirewallRule) conf;
            firewallRuleDB.put(nn.getFirewallRuleUUID(), nn);
        }
    }

    @Override
    public Status saveConfiguration() {
        return configurationService.persistConfiguration(new ArrayList<ConfigurationObject>(firewallRuleDB.values()),
                FILE_NAME);
    }

    @Override
    public Object readObject(ObjectInputStream ois) throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }

}