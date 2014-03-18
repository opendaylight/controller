/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.ConfigurationObject;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.ForwardingMode;
import org.opendaylight.controller.sal.core.MacAddress;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IInventoryService;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISpanAware;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.switchmanager.SpanConfig;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class describes SwitchManager which is the central repository of all the
 * inventory data including nodes, node connectors, properties attached, Layer3
 * configurations, Span configurations, node configurations, network device
 * representations viewed by Controller Web applications. One SwitchManager
 * instance per container of the network. All the node/nodeConnector properties
 * are maintained in the default container only.
 */
public class SwitchManager implements ISwitchManager, IConfigurationContainerAware,
                                      IObjectReader, IListenInventoryUpdates, CommandProvider {
    private static Logger log = LoggerFactory.getLogger(SwitchManager.class);
    private static final String SUBNETS_FILE_NAME = "subnets.conf";
    private static final String SPAN_FILE_NAME = "spanPorts.conf";
    private static final String SWITCH_CONFIG_FILE_NAME = "switchConfig.conf";
    private final List<NodeConnector> spanNodeConnectors = new CopyOnWriteArrayList<NodeConnector>();
    // Collection of Subnets keyed by the InetAddress
    private ConcurrentMap<InetAddress, Subnet> subnets;
    private ConcurrentMap<String, SubnetConfig> subnetsConfigList;
    private ConcurrentMap<SpanConfig, SpanConfig> spanConfigList;
    // manually configured parameters for the node such as name, tier, mode
    private ConcurrentMap<String, SwitchConfig> nodeConfigList;
    private ConcurrentMap<Node, Map<String, Property>> nodeProps;
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps;
    private ConcurrentMap<Node, Map<String, NodeConnector>> nodeConnectorNames;
    private ConcurrentMap<String, Property> controllerProps;
    private IInventoryService inventoryService;
    private IStatisticsManager statisticsManager;
    private IControllerProperties controllerProperties;
    private IConfigurationContainerService configurationService;
    private final Set<ISwitchManagerAware> switchManagerAware = Collections
            .synchronizedSet(new HashSet<ISwitchManagerAware>());
    private final Set<IInventoryListener> inventoryListeners = Collections
            .synchronizedSet(new HashSet<IInventoryListener>());
    private final Set<ISpanAware> spanAware = Collections.synchronizedSet(new HashSet<ISpanAware>());
    private IClusterContainerServices clusterContainerService = null;
    private String containerName = null;
    private boolean isDefaultContainer = true;
    private static final int REPLACE_RETRY = 1;

    /* Information about the default subnet. If there have been no configured subnets, i.e.,
     * subnets.size() == 0 or subnetsConfigList.size() == 0, then this subnet will be the
     * only subnet returned. As soon as a user-configured subnet is created this one will
     * vanish.
     */
    protected static final SubnetConfig DEFAULT_SUBNETCONFIG;
    protected static final Subnet DEFAULT_SUBNET;
    protected static final String DEFAULT_SUBNET_NAME = "default (cannot be modifed)";
    static{
        DEFAULT_SUBNETCONFIG = new SubnetConfig(DEFAULT_SUBNET_NAME, "0.0.0.0/0", new ArrayList<String>());
        DEFAULT_SUBNET = new Subnet(DEFAULT_SUBNETCONFIG);
    }

    public void notifySubnetChange(Subnet sub, boolean add) {
        synchronized (switchManagerAware) {
            for (Object subAware : switchManagerAware) {
                try {
                    ((ISwitchManagerAware) subAware).subnetNotify(sub, add);
                } catch (Exception e) {
                    log.error("Failed to notify Subnet change {}",
                            e.getMessage());
                }
            }
        }
    }

    public void notifySpanPortChange(Node node, List<NodeConnector> ports, boolean add) {
        synchronized (spanAware) {
            for (Object sa : spanAware) {
                try {
                    ((ISpanAware) sa).spanUpdate(node, ports, add);
                } catch (Exception e) {
                    log.error("Failed to notify Span Interface change {}",
                            e.getMessage());
                }
            }
        }
    }

    private void notifyModeChange(Node node, boolean proactive) {
        synchronized (switchManagerAware) {
            for (ISwitchManagerAware service : switchManagerAware) {
                try {
                    service.modeChangeNotify(node, proactive);
                } catch (Exception e) {
                    log.error("Failed to notify Subnet change {}",
                            e.getMessage());
                }
            }
        }
    }

    public void startUp() {
        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();

        // Add controller MAC, if first node in the cluster
        if ((!controllerProps.containsKey(MacAddress.name)) && (controllerProperties != null)) {
            Property controllerMac = controllerProperties.getControllerProperty(MacAddress.name);
            if (controllerMac != null) {
                Property existing = controllerProps.putIfAbsent(MacAddress.name, controllerMac);
                if (existing == null && log.isTraceEnabled()) {
                    log.trace("Container {}: Setting controller MAC address in the cluster: {}", getContainerName(),
                            controllerMac);
                }
            }
        }
    }

    public void shutDown() {
    }

    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            this.nonClusterObjectCreate();
            log.warn("un-initialized clusterContainerService, can't create cache");
            return;
        }

        try {
            clusterContainerService.createCache(
                    "switchmanager.subnetsConfigList",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.spanConfigList",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.nodeConfigList",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.subnets",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.nodeProps",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache(
                    "switchmanager.nodeConnectorProps",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache(
                    "switchmanager.nodeConnectorNames",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache(
                    "switchmanager.controllerProps",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            log.error("\nCache already exits - destroy and recreate if needed");
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        if (this.clusterContainerService == null) {
            log.warn("un-initialized clusterContainerService, can't create cache");
            return;
        }

        subnetsConfigList = (ConcurrentMap<String, SubnetConfig>) clusterContainerService
                .getCache("switchmanager.subnetsConfigList");
        if (subnetsConfigList == null) {
            log.error("\nFailed to get cache for subnetsConfigList");
        }

        spanConfigList = (ConcurrentMap<SpanConfig, SpanConfig>) clusterContainerService
                .getCache("switchmanager.spanConfigList");
        if (spanConfigList == null) {
            log.error("\nFailed to get cache for spanConfigList");
        }

        nodeConfigList = (ConcurrentMap<String, SwitchConfig>) clusterContainerService
                .getCache("switchmanager.nodeConfigList");
        if (nodeConfigList == null) {
            log.error("\nFailed to get cache for nodeConfigList");
        }

        subnets = (ConcurrentMap<InetAddress, Subnet>) clusterContainerService
                .getCache("switchmanager.subnets");
        if (subnets == null) {
            log.error("\nFailed to get cache for subnets");
        }

        nodeProps = (ConcurrentMap<Node, Map<String, Property>>) clusterContainerService
                .getCache("switchmanager.nodeProps");
        if (nodeProps == null) {
            log.error("\nFailed to get cache for nodeProps");
        }

        nodeConnectorProps = (ConcurrentMap<NodeConnector, Map<String, Property>>) clusterContainerService
                .getCache("switchmanager.nodeConnectorProps");
        if (nodeConnectorProps == null) {
            log.error("\nFailed to get cache for nodeConnectorProps");
        }

        nodeConnectorNames = (ConcurrentMap<Node, Map<String, NodeConnector>>) clusterContainerService
                .getCache("switchmanager.nodeConnectorNames");
        if (nodeConnectorNames == null) {
            log.error("\nFailed to get cache for nodeConnectorNames");
        }

        controllerProps = (ConcurrentMap<String, Property>) clusterContainerService
                .getCache("switchmanager.controllerProps");
        if (controllerProps == null) {
            log.error("\nFailed to get cache for controllerProps");
        }
    }

    private void nonClusterObjectCreate() {
        subnetsConfigList = new ConcurrentHashMap<String, SubnetConfig>();
        spanConfigList = new ConcurrentHashMap<SpanConfig, SpanConfig>();
        nodeConfigList = new ConcurrentHashMap<String, SwitchConfig>();
        subnets = new ConcurrentHashMap<InetAddress, Subnet>();
        nodeProps = new ConcurrentHashMap<Node, Map<String, Property>>();
        nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
        nodeConnectorNames = new ConcurrentHashMap<Node, Map<String, NodeConnector>>();
        controllerProps = new ConcurrentHashMap<String, Property>();
    }

    @Override
    public List<SubnetConfig> getSubnetsConfigList() {
        // if there are no subnets, return the default subnet
        if(subnetsConfigList.size() == 0){
            return Collections.singletonList(DEFAULT_SUBNETCONFIG);
        }else{
            return new ArrayList<SubnetConfig>(subnetsConfigList.values());
        }
    }

    @Override
    public SubnetConfig getSubnetConfig(String subnet) {
        // if there are no subnets, return the default subnet
        if(subnetsConfigList.isEmpty() && subnet.equalsIgnoreCase(DEFAULT_SUBNET_NAME)){
            return DEFAULT_SUBNETCONFIG;
        }else{
            return subnetsConfigList.get(subnet);
        }
    }

    private List<SpanConfig> getSpanConfigList(Node node) {
        List<SpanConfig> confList = new ArrayList<SpanConfig>();
        String nodeId = node.toString();
        for (SpanConfig conf : spanConfigList.values()) {
            if (conf.matchNode(nodeId)) {
                confList.add(conf);
            }
        }
        return confList;
    }

    public List<SwitchConfig> getNodeConfigList() {
        return new ArrayList<SwitchConfig>(nodeConfigList.values());
    }

    @Override
    public SwitchConfig getSwitchConfig(String switchId) {
        return nodeConfigList.get(switchId);
    }

    public Switch getSwitchByNode(Node node) {
        Switch sw = new Switch(node);
        sw.setNode(node);
        MacAddress mac = (MacAddress) this.getNodeProp(node,
                MacAddress.name);
        if (mac != null) {
            sw.setDataLayerAddress(mac.getMacAddress());
        }
        Set<NodeConnector> ncSet = getPhysicalNodeConnectors(node);
        sw.setNodeConnectors(ncSet);

        List<NodeConnector> ncList = new ArrayList<NodeConnector>();
        for (NodeConnector nodeConnector : ncSet) {
            if (spanNodeConnectors.contains(nodeConnector)) {
                ncList.add(nodeConnector);
            }
        }
        sw.addSpanPorts(ncList);

        return sw;
    }

    @Override
    public List<Switch> getNetworkDevices() {
        List<Switch> swList = new ArrayList<Switch>();
        for (Node node : getNodes()) {
            swList.add(getSwitchByNode(node));
        }
        return swList;
    }

    private Status updateConfig(SubnetConfig conf, boolean add) {
        if (add) {
            if(subnetsConfigList.putIfAbsent(conf.getName(), conf) != null) {
                String msg = "Cluster conflict: Subnet with name " + conf.getName() + "already exists.";
                return new Status(StatusCode.CONFLICT, msg);
            }
        } else {
            subnetsConfigList.remove(conf.getName());
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status updateDatabase(SubnetConfig conf, boolean add) {
        if (add) {
            Subnet subnetCurr = subnets.get(conf.getIPAddress());
            Subnet subnet;
            if (subnetCurr == null) {
                subnet = new Subnet(conf);
            } else {
                subnet = subnetCurr.clone();
            }
            // In case of API3 call we may receive the ports along with the
            // subnet creation
            if (!conf.isGlobal()) {
                subnet.addNodeConnectors(conf.getNodeConnectors());
            }
            boolean putNewSubnet = false;
            if(subnetCurr == null) {
                if(subnets.putIfAbsent(conf.getIPAddress(), subnet) == null) {
                    putNewSubnet = true;
                }
            } else {
                putNewSubnet = subnets.replace(conf.getIPAddress(), subnetCurr, subnet);
            }
            if(!putNewSubnet) {
                String msg = "Cluster conflict: Conflict while adding the subnet " + conf.getIPAddress();
                return new Status(StatusCode.CONFLICT, msg);
            }

        // Subnet removal case
        } else {
            subnets.remove(conf.getIPAddress());
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status semanticCheck(SubnetConfig conf) {
        Set<InetAddress> IPs = subnets.keySet();
        if (IPs == null) {
            return new Status(StatusCode.SUCCESS);
        }
        Subnet newSubnet = new Subnet(conf);
        for (InetAddress i : IPs) {
            Subnet existingSubnet = subnets.get(i);
            if ((existingSubnet != null) && !existingSubnet.isMutualExclusive(newSubnet)) {
                return new Status(StatusCode.CONFLICT, "This subnet conflicts with an existing one.");
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status addRemoveSubnet(SubnetConfig conf, boolean isAdding) {
        // Valid configuration check
        Status status = conf.validate();
        if (!status.isSuccess()) {
            log.warn(status.getDescription());
            return status;
        }

        if (isAdding) {
            // Presence check
            if (subnetsConfigList.containsKey(conf.getName())) {
                return new Status(StatusCode.CONFLICT,
                        "Subnet with the specified name already exists.");
            }
            // Semantic check
            status = semanticCheck(conf);
            if (!status.isSuccess()) {
                return status;
            }
        } else {
            if (conf.getName().equalsIgnoreCase(DEFAULT_SUBNET_NAME)) {
                return new Status(StatusCode.NOTALLOWED, "The specified subnet gateway cannot be removed");
            }
        }

        // Update Database
        status = updateDatabase(conf, isAdding);

        if (status.isSuccess()) {
            // Update Configuration
            status = updateConfig(conf, isAdding);
            if(!status.isSuccess()) {
                updateDatabase(conf, (!isAdding));
            } else {
                // update the listeners
                Subnet subnetCurr = subnets.get(conf.getIPAddress());
                Subnet subnet;
                if (subnetCurr == null) {
                    subnet = new Subnet(conf);
                } else {
                    subnet = subnetCurr.clone();
                }
                notifySubnetChange(subnet, isAdding);
            }
        }

        return status;
    }

    /**
     * Adds Subnet configured in GUI or API3
     */
    @Override
    public Status addSubnet(SubnetConfig conf) {
        return this.addRemoveSubnet(conf, true);
    }

    @Override
    public Status removeSubnet(SubnetConfig conf) {
        return this.addRemoveSubnet(conf, false);
    }

    @Override
    public Status removeSubnet(String name) {
        if (name.equalsIgnoreCase(DEFAULT_SUBNET_NAME)) {
            return new Status(StatusCode.NOTALLOWED, "The specified subnet gateway cannot be removed");
        }
        SubnetConfig conf = subnetsConfigList.get(name);
        if (conf == null) {
            return new Status(StatusCode.SUCCESS, "Subnet not present");
        }
        return this.addRemoveSubnet(conf, false);
    }

    @Override
    public Status modifySubnet(SubnetConfig conf) {
        // Sanity check
        if (conf == null) {
            return new Status(StatusCode.BADREQUEST, "Invalid Subnet configuration: null");
        }

        // Valid configuration check
        Status status = conf.validate();
        if (!status.isSuccess()) {
            log.warn(status.getDescription());
            return status;
        }

        // If a subnet configuration with this name does not exist, consider this is a creation
        SubnetConfig target = subnetsConfigList.get(conf.getName());
        if (target == null) {
            return this.addSubnet(conf);
        }

        // No change
        if (target.equals(conf)) {
            return new Status(StatusCode.SUCCESS);
        }

        // Check not allowed modifications
        if (!target.getSubnet().equals(conf.getSubnet())) {
            return new Status(StatusCode.BADREQUEST, "IP address change is not allowed");
        }

        // Derive the set of node connectors that are being removed
        Set<NodeConnector> toRemove = target.getNodeConnectors();
        toRemove.removeAll(conf.getNodeConnectors());
        List<String> nodeConnectorStrings = null;
        if (!toRemove.isEmpty()) {
            nodeConnectorStrings = new ArrayList<String>();
            for (NodeConnector nc : toRemove) {
                nodeConnectorStrings.add(nc.toString());
            }
            status = this.removePortsFromSubnet(conf.getName(), nodeConnectorStrings);
            if (!status.isSuccess()) {
                return status;
            }
        }

        // Derive the set of node connectors that are being added
        Set<NodeConnector> toAdd = conf.getNodeConnectors();
        toAdd.removeAll(target.getNodeConnectors());
        if (!toAdd.isEmpty()) {
            List<String> nodeConnectorStringRemoved = nodeConnectorStrings;
            nodeConnectorStrings = new ArrayList<String>();
            for (NodeConnector nc : toAdd) {
                nodeConnectorStrings.add(nc.toString());
            }
            status = this.addPortsToSubnet(conf.getName(), nodeConnectorStrings);
            if (!status.isSuccess()) {
                // If any port was removed, add it back as a best recovery effort
                if (!toRemove.isEmpty()) {
                    this.addPortsToSubnet(conf.getName(), nodeConnectorStringRemoved);
                }
                return status;
            }
        }

        // Update Configuration
        subnetsConfigList.put(conf.getName(), conf);

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status addPortsToSubnet(String name, List<String> switchPorts) {
        if (name == null) {
            return new Status(StatusCode.BADREQUEST, "Null subnet name");
        }
        SubnetConfig confCurr = subnetsConfigList.get(name);
        if (confCurr == null) {
            return new Status(StatusCode.NOTFOUND, "Subnet does not exist");
        }

        if (switchPorts == null || switchPorts.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Null or empty port set");
        }

        Subnet subCurr = subnets.get(confCurr.getIPAddress());
        if (subCurr == null) {
            log.debug("Cluster conflict: Subnet entry {} is not present in the subnets cache.", confCurr.getIPAddress());
            return new Status(StatusCode.NOTFOUND, "Subnet does not exist");
        }

        // Update Database
        Subnet sub = subCurr.clone();
        Set<NodeConnector> sp = NodeConnector.fromString(switchPorts);
        sub.addNodeConnectors(sp);
        boolean subnetsReplaced = subnets.replace(confCurr.getIPAddress(), subCurr, sub);
        if (!subnetsReplaced) {
            String msg = "Cluster conflict: Conflict while adding ports to the subnet " + name;
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Update Configuration
        SubnetConfig conf = confCurr.clone();
        conf.addNodeConnectors(switchPorts);
        boolean configReplaced = subnetsConfigList.replace(name, confCurr, conf);
        if (!configReplaced) {
            // TODO: recovery using Transactionality
            String msg = "Cluster conflict: Conflict while adding ports to the subnet " + name;
            return new Status(StatusCode.CONFLICT, msg);
        }

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status removePortsFromSubnet(String name, List<String> switchPorts) {
        if (name == null) {
            return new Status(StatusCode.BADREQUEST, "Null subnet name");
        }
        SubnetConfig confCurr = subnetsConfigList.get(name);
        if (confCurr == null) {
            return new Status(StatusCode.NOTFOUND, "Subnet does not exist");
        }

        if (switchPorts == null || switchPorts.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Null or empty port set");
        }

        Subnet subCurr = subnets.get(confCurr.getIPAddress());
        if (subCurr == null) {
            log.debug("Cluster conflict: Subnet entry {} is not present in the subnets cache.", confCurr.getIPAddress());
            return new Status(StatusCode.NOTFOUND, "Subnet does not exist");
        }

        // Validation check
        Status status = SubnetConfig.validatePorts(switchPorts);
        if (!status.isSuccess()) {
            return status;
        }
        // Update Database
        Subnet sub = subCurr.clone();
        Set<NodeConnector> sp = NodeConnector.fromString(switchPorts);
        sub.deleteNodeConnectors(sp);
        boolean subnetsReplace = subnets.replace(confCurr.getIPAddress(), subCurr, sub);
        if (!subnetsReplace) {
            String msg = "Cluster conflict: Conflict while removing ports from the subnet " + name;
            return new Status(StatusCode.CONFLICT, msg);
        }

        // Update Configuration
        SubnetConfig conf = confCurr.clone();
        conf.removeNodeConnectors(switchPorts);
        boolean result = subnetsConfigList.replace(name, confCurr, conf);
        if (!result) {
            // TODO: recovery using Transactionality
            String msg = "Cluster conflict: Conflict while removing ports from " + conf;
            return new Status(StatusCode.CONFLICT, msg);
        }

        return new Status(StatusCode.SUCCESS);
    }

    public String getContainerName() {
        if (containerName == null) {
            return GlobalConstants.DEFAULT.toString();
        }
        return containerName;
    }

    @Override
    public Subnet getSubnetByNetworkAddress(InetAddress networkAddress) {
        // if there are no subnets, return the default subnet
        if (subnets.size() == 0) {
            return DEFAULT_SUBNET;
        }

        for(Map.Entry<InetAddress,Subnet> subnetEntry : subnets.entrySet()) {
            if(subnetEntry.getValue().isSubnetOf(networkAddress)) {
                return subnetEntry.getValue();
            }
        }
        return null;
    }

    @Override
    public Object readObject(ObjectInputStream ois)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        // Perform the class deserialization locally, from inside the package
        // where the class is defined
        return ois.readObject();
    }

    private void loadSubnetConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, SUBNETS_FILE_NAME)) {
            addSubnet((SubnetConfig) conf);
        }
    }

    private void loadSpanConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, SPAN_FILE_NAME)) {
            addSpanConfig((SpanConfig) conf);
        }
    }

    private void loadSwitchConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, SWITCH_CONFIG_FILE_NAME)) {
            updateNodeConfig((SwitchConfig) conf);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void updateSwitchConfig(SwitchConfig cfgObject) {
        // update default container only
        if (!isDefaultContainer) {
            return;
        }

        SwitchConfig sc = nodeConfigList.get(cfgObject.getNodeId());
        if (sc == null) {
            if (nodeConfigList.putIfAbsent(cfgObject.getNodeId(), cfgObject) != null) {
                return;
            }
        } else {
            if (!nodeConfigList.replace(cfgObject.getNodeId(), sc, cfgObject)) {
                return;
            }
        }

        boolean modeChange = false;

        if ((sc == null) || !cfgObject.getMode().equals(sc.getMode())) {
            modeChange = true;
        }

        String nodeId = cfgObject.getNodeId();
        Node node = Node.fromString(nodeId);
        Map<String, Property> propMapCurr = nodeProps.get(node);
        if (propMapCurr == null) {
            return;
        }
        Map<String, Property> propMap = new HashMap<String, Property>(propMapCurr);
        Property desc = new Description(cfgObject.getNodeDescription());
        propMap.put(desc.getName(), desc);
        Property tier = new Tier(Integer.parseInt(cfgObject.getTier()));
        propMap.put(tier.getName(), tier);

        if (!nodeProps.replace(node, propMapCurr, propMap)) {
            // TODO rollback using Transactionality
            return;
        }

        log.trace("Set Node {}'s Mode to {}", nodeId, cfgObject.getMode());

        if (modeChange) {
            notifyModeChange(node, cfgObject.isProactive());
        }
    }

    @Override
    public Status updateNodeConfig(SwitchConfig switchConfig) {
        Status status = switchConfig.validate();
        if (!status.isSuccess()) {
            return status;
        }

        Map<String, Property> updateProperties = switchConfig.getNodeProperties();
        ForwardingMode mode = (ForwardingMode) updateProperties.get(ForwardingMode.name);
        if (mode != null) {
            if (isDefaultContainer) {
                if (!mode.isValid()) {
                    return new Status(StatusCode.BADREQUEST, "Invalid Forwarding Mode Value");
                }
            } else {
                return new Status(StatusCode.NOTACCEPTABLE,
                        "Forwarding Mode modification is allowed only in default container");
            }
        }

        Description description = (Description) switchConfig.getProperty(Description.propertyName);
        String nodeId = switchConfig.getNodeId();
        Node node = Node.fromString(nodeId);
        NodeDescription nodeDesc = (this.statisticsManager == null) ? null : this.statisticsManager
                .getNodeDescription(node);
        String advertisedDesc = (nodeDesc == null) ? "" : nodeDesc.getDescription();
        if (description != null && description.getValue() != null) {
            if (description.getValue().isEmpty() || description.getValue().equals(advertisedDesc)) {
                updateProperties.remove(Description.propertyName);
                switchConfig = new SwitchConfig(nodeId, updateProperties);
            } else {
                // check if description is configured or was published by any other node
                for (Map.Entry<Node, Map<String, Property>> entry : nodeProps.entrySet()) {
                    Node n = entry.getKey();
                    Description desc = (Description) getNodeProp(n, Description.propertyName);
                    NodeDescription nDesc = (this.statisticsManager == null) ? null : this.statisticsManager
                            .getNodeDescription(n);
                    String advDesc = (nDesc == null) ? "" : nDesc.getDescription();
                    if ((description.equals(desc) || description.getValue().equals(advDesc)) && !node.equals(n)) {
                        return new Status(StatusCode.CONFLICT, "Node name already in use");
                    }
                }
            }
        }

        boolean modeChange = false;
        SwitchConfig sc = nodeConfigList.get(nodeId);
        Map<String, Property> prevNodeProperties = new HashMap<String, Property>();
        if (sc == null) {
            if ((mode != null) && mode.isProactive()) {
                modeChange = true;
            }
            if (!updateProperties.isEmpty()) {
                if (nodeConfigList.putIfAbsent(nodeId, switchConfig) != null) {
                    return new Status(StatusCode.CONFLICT, "Cluster conflict: Unable to update node configuration");
                }
            }
        } else {
            prevNodeProperties = new HashMap<String, Property>(sc.getNodeProperties());
            ForwardingMode prevMode = (ForwardingMode) sc.getProperty(ForwardingMode.name);
            if (mode == null) {
                if ((prevMode != null) && (prevMode.isProactive())) {
                    modeChange = true;
                }
            } else {
                if (((prevMode != null) && (prevMode.getValue() != mode.getValue()))
                        || (prevMode == null && mode.isProactive())) {
                    modeChange = true;
                }
            }
            if (updateProperties.isEmpty()) {
                nodeConfigList.remove(nodeId);
            } else {
                if (!nodeConfigList.replace(nodeId, sc, switchConfig)) {
                    return new Status(StatusCode.CONFLICT, "Cluster conflict: Unable to update node configuration");
                }
            }
        }
        Map<String, Property> propMapCurr = nodeProps.get(node);
        if (propMapCurr == null) {
            return new Status(StatusCode.SUCCESS);
        }
        Map<String, Property> propMap = new HashMap<String, Property>(propMapCurr);
        for (Map.Entry<String, Property> entry : prevNodeProperties.entrySet()) {
            String prop = entry.getKey();
            if (!updateProperties.containsKey(prop)) {
                if (prop.equals(Description.propertyName)) {
                    if (advertisedDesc != null) {
                        if (!advertisedDesc.isEmpty()) {
                            Property desc = new Description(advertisedDesc);
                            propMap.put(Description.propertyName, desc);
                        }
                    }
                    else {
                        propMap.remove(prop);
                    }
                    continue;
                } else if (prop.equals(ForwardingMode.name)) {
                    Property defaultMode = new ForwardingMode(ForwardingMode.REACTIVE_FORWARDING);
                    propMap.put(ForwardingMode.name, defaultMode);
                    continue;
                }
                propMap.remove(prop);
            }
        }
        propMap.putAll(updateProperties);
        if (!nodeProps.replace(node, propMapCurr, propMap)) {
            // TODO rollback using Transactionality
            return new Status(StatusCode.CONFLICT, "Cluster conflict: Unable to update node configuration");
        }
        if (modeChange) {
            notifyModeChange(node, (mode == null) ? false : mode.isProactive());
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status removeNodeConfig(String nodeId) {
        if ((nodeId == null) || (nodeId.isEmpty())) {
            return new Status(StatusCode.BADREQUEST, "nodeId cannot be empty.");
        }
        Map<String, Property> nodeProperties = getSwitchConfig(nodeId).getNodeProperties();
        Node node = Node.fromString(nodeId);
        Map<String, Property> propMapCurr = nodeProps.get(node);
        if ((propMapCurr != null) && (nodeProperties != null) && (!nodeProperties.isEmpty())) {
            Map<String, Property> propMap = new HashMap<String, Property>(propMapCurr);
            for (Map.Entry<String, Property> entry : nodeProperties.entrySet()) {
                String prop = entry.getKey();
                if (prop.equals(Description.propertyName)) {
                    Map<Node, Map<String, Property>> nodeProp = this.inventoryService.getNodeProps();
                    if (nodeProp.get(node) != null) {
                        propMap.put(Description.propertyName, nodeProp.get(node).get(Description.propertyName));
                        continue;
                    }
                }
                propMap.remove(prop);
            }
            if (!nodeProps.replace(node, propMapCurr, propMap)) {
                return new Status(StatusCode.CONFLICT, "Cluster conflict: Unable to update node configuration.");
            }
        }
        if (nodeConfigList != null) {
            nodeConfigList.remove(nodeId);
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status saveSwitchConfig() {
        return saveSwitchConfigInternal();
    }

    public Status saveSwitchConfigInternal() {
        Status status;
        short number = 0;
        status = configurationService.persistConfiguration(
                new ArrayList<ConfigurationObject>(subnetsConfigList.values()), SUBNETS_FILE_NAME);
        if (status.isSuccess()) {
            number++;
        } else {
            log.warn("Failed to save subnet gateway configurations: " + status.getDescription());
        }
        status = configurationService.persistConfiguration(new ArrayList<ConfigurationObject>(spanConfigList.values()),
                SPAN_FILE_NAME);
        if (status.isSuccess()) {
            number++;
        } else {
            log.warn("Failed to save span port configurations: " + status.getDescription());
        }
        status = configurationService.persistConfiguration(new ArrayList<ConfigurationObject>(nodeConfigList.values()),
                SWITCH_CONFIG_FILE_NAME);
        if (status.isSuccess()) {
            number++;
        } else {
            log.warn("Failed to save node configurations: " + status.getDescription());
        }
        if (number == 0) {
            return new Status(StatusCode.INTERNALERROR, "Save failed");
        }
        if (number < 3) {
            return new Status(StatusCode.INTERNALERROR, "Partial save failure");
        }
        return status;
    }

    @Override
    public List<SpanConfig> getSpanConfigList() {
        return new ArrayList<SpanConfig>(spanConfigList.values());
    }

    @Override
    public Status addSpanConfig(SpanConfig conf) {
        // Valid config check
        if (!conf.isValidConfig()) {
            String msg = "Invalid Span configuration";
            log.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        // Presence check
        if (spanConfigList.containsKey(conf)) {
            return new Status(StatusCode.CONFLICT, "Same span config exists");
        }

        // Update configuration
        if (spanConfigList.putIfAbsent(conf, conf) == null) {
            // Update database and notify clients
            addSpanPorts(conf.getNode(), conf.getPortArrayList());
        }

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status removeSpanConfig(SpanConfig conf) {
        removeSpanPorts(conf.getNode(), conf.getPortArrayList());

        // Update configuration
        spanConfigList.remove(conf);

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public List<NodeConnector> getSpanPorts(Node node) {
        List<NodeConnector> ncList = new ArrayList<NodeConnector>();

        for (NodeConnector nodeConnector : spanNodeConnectors) {
            if (nodeConnector.getNode().equals(node)) {
                ncList.add(nodeConnector);
            }
        }
        return ncList;
    }

    private void addNode(Node node, Set<Property> props) {
        log.trace("{} added, props: {}", node, props);
        if (nodeProps == null) {
            return;
        }

        Map<String, Property> propMapCurr = nodeProps.get(node);
        Map<String, Property> propMap = (propMapCurr == null) ? new HashMap<String, Property>()
                : new HashMap<String, Property>(propMapCurr);

        // copy node properties from plugin
        if (props != null) {
            for (Property prop : props) {
                propMap.put(prop.getName(), prop);
            }
        }

        boolean forwardingModeChanged = false;

        // copy node properties from config
        if (nodeConfigList != null) {
            String nodeId = node.toString();
            SwitchConfig conf = nodeConfigList.get(nodeId);
            if (conf != null && (conf.getNodeProperties() != null)) {
                Map<String, Property> nodeProperties = conf.getNodeProperties();
                propMap.putAll(nodeProperties);
                if (nodeProperties.get(ForwardingMode.name) != null) {
                    ForwardingMode mode = (ForwardingMode) nodeProperties.get(ForwardingMode.name);
                    forwardingModeChanged = mode.isProactive();
                }
            }
        }

        if (!propMap.containsKey(ForwardingMode.name)) {
            Property defaultMode = new ForwardingMode(ForwardingMode.REACTIVE_FORWARDING);
            propMap.put(ForwardingMode.name, defaultMode);
        }

        boolean propsAdded = false;
        // Attempt initial add
        if (nodeProps.putIfAbsent(node, propMap) == null) {
                propsAdded = true;

                /* Notify listeners only for initial node addition
                 * to avoid expensive tasks triggered by redundant notifications
                 */
                notifyNode(node, UpdateType.ADDED, propMap);
        } else {

            propsAdded = nodeProps.replace(node, propMapCurr, propMap);

            // check whether forwarding mode changed
            if (propMapCurr.get(ForwardingMode.name) != null) {
                ForwardingMode mode = (ForwardingMode) propMapCurr.get(ForwardingMode.name);
                forwardingModeChanged ^= mode.isProactive();
            }
        }
        if (!propsAdded) {
            log.debug("Cluster conflict while adding node {}. Overwriting with latest props: {}", node.getID(), props);
            addNodeProps(node, propMap);
        }

        // check if span ports are configured
        addSpanPorts(node);
        // notify proactive mode forwarding
        if (forwardingModeChanged) {
            notifyModeChange(node, true);
        }
    }

    private void removeNode(Node node) {
        log.trace("{} removed", node);
        if (nodeProps == null) {
            return;
        }

        if (nodeProps.remove(node) == null) {
            log.debug("Received redundant node REMOVED udate for {}. Skipping..", node);
            return;
        }

        nodeConnectorNames.remove(node);
        Set<NodeConnector> removeNodeConnectorSet = new HashSet<NodeConnector>();
        for (Map.Entry<NodeConnector, Map<String, Property>> entry : nodeConnectorProps.entrySet()) {
            NodeConnector nodeConnector = entry.getKey();
            if (nodeConnector.getNode().equals(node)) {
                removeNodeConnectorSet.add(nodeConnector);
            }
        }
        for (NodeConnector nc : removeNodeConnectorSet) {
            nodeConnectorProps.remove(nc);
        }

        // check if span ports need to be cleaned up
        removeSpanPorts(node);

        /* notify node listeners */
        notifyNode(node, UpdateType.REMOVED, null);
    }

    private void updateNode(Node node, Set<Property> props) {
        log.trace("{} updated, props: {}", node, props);
        if (nodeProps == null || props == null) {
            return;
        }

        Map<String, Property> propMapCurr = nodeProps.get(node);
        Map<String, Property> propMap = (propMapCurr == null) ? new HashMap<String, Property>()
                : new HashMap<String, Property>(propMapCurr);

        // copy node properties from plugin
        String nodeId = node.toString();
        for (Property prop : props) {
            if (nodeConfigList != null) {
                SwitchConfig conf = nodeConfigList.get(nodeId);
                if (conf != null && (conf.getNodeProperties() != null)
                        && conf.getNodeProperties().containsKey(prop.getName())) {
                    continue;
                }
            }
            propMap.put(prop.getName(), prop);
        }

        if (propMapCurr == null) {
            if (nodeProps.putIfAbsent(node, propMap) != null) {
                log.debug("Cluster conflict: Conflict while updating the node. Node: {}  Properties: {}",
                        node.getID(), props);
                addNodeProps(node, propMap);
            }
        } else {
            if (!nodeProps.replace(node, propMapCurr, propMap)) {
                log.debug("Cluster conflict: Conflict while updating the node. Node: {}  Properties: {}",
                        node.getID(), props);
                addNodeProps(node, propMap);
            }
        }

        /* notify node listeners */
        notifyNode(node, UpdateType.CHANGED, propMap);
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        log.debug("updateNode: {} type {} props {} for container {}",
                new Object[] { node, type, props, containerName });
        switch (type) {
        case ADDED:
            addNode(node, props);
            break;
        case CHANGED:
            updateNode(node, props);
            break;
        case REMOVED:
            removeNode(node);
            break;
        default:
            break;
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        Map<String, Property> propMap = new HashMap<String, Property>();
        boolean update = true;

        log.debug("updateNodeConnector: {} type {} props {} for container {}",
                new Object[] { nodeConnector, type, props, containerName });

        if (nodeConnectorProps == null) {
            return;
        }

        switch (type) {
        case ADDED:
            // Skip redundant ADDED update (e.g. cluster switch-over)
            if (nodeConnectorProps.containsKey(nodeConnector)) {
                log.debug("Redundant nodeconnector ADDED for {}, props {} for container {}",
                        nodeConnector, props, containerName);
                update = false;
            }

            if (props != null) {
                for (Property prop : props) {
                    addNodeConnectorProp(nodeConnector, prop);
                    propMap.put(prop.getName(), prop);
                }
            } else {
                addNodeConnectorProp(nodeConnector, null);
            }


            addSpanPort(nodeConnector);
            break;
        case CHANGED:
            if (!nodeConnectorProps.containsKey(nodeConnector) || (props == null)) {
                update = false;
            } else {
                for (Property prop : props) {
                    addNodeConnectorProp(nodeConnector, prop);
                    propMap.put(prop.getName(), prop);
                }
            }
            break;
        case REMOVED:
            if (!nodeConnectorProps.containsKey(nodeConnector)) {
                update = false;
            }
            removeNodeConnectorAllProps(nodeConnector);

            // clean up span config
            removeSpanPort(nodeConnector);
            break;
        default:
            update = false;
            break;
        }

        if (update) {
            notifyNodeConnector(nodeConnector, type, propMap);
        }
    }

    @Override
    public Set<Node> getNodes() {
        return (nodeProps != null) ? new HashSet<Node>(nodeProps.keySet()) : new HashSet<Node>();
    }

    @Override
    public Map<String, Property> getControllerProperties() {
        return new HashMap<String, Property>(this.controllerProps);
    }

    @Override
    public Property getControllerProperty(String propertyName) {
        if (propertyName != null) {
            HashMap<String, Property> propertyMap =  new HashMap<String, Property>(this.controllerProps);
            return propertyMap.get(propertyName);
        }
        return null;
    }

    @Override
    public Status setControllerProperty(Property property) {
        if (property != null) {
            this.controllerProps.put(property.getName(), property);
            return new Status(StatusCode.SUCCESS);
        }
        return new Status(StatusCode.BADREQUEST, "Invalid property provided when setting property");
    }

    @Override
    public Status removeControllerProperty(String propertyName) {
        if (propertyName != null) {
            if (this.controllerProps.containsKey(propertyName)) {
                this.controllerProps.remove(propertyName);
                if (!this.controllerProps.containsKey(propertyName)) {
                    return new Status(StatusCode.SUCCESS);
                }
            }
            String msg = "Unable to remove property " + propertyName + " from Controller";
            return new Status(StatusCode.BADREQUEST, msg);
        }
        String msg = "Invalid property provided when removing property from Controller";
        return new Status(StatusCode.BADREQUEST, msg);
    }

    /*
     * Returns a copy of a list of properties for a given node
     *
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.switchmanager.ISwitchManager#getNodeProps
     * (org.opendaylight.controller.sal.core.Node)
     */
    @Override
    public Map<String, Property> getNodeProps(Node node) {
        Map<String, Property> rv = new HashMap<String, Property>();
        if (this.nodeProps != null) {
            rv = this.nodeProps.get(node);
            if (rv != null) {
                /* make a copy of it */
                rv = new HashMap<String, Property>(rv);
            }
        }
        return rv;
    }

    @Override
    public Property getNodeProp(Node node, String propName) {
        Map<String, Property> propMap = getNodeProps(node);
        return (propMap != null) ? propMap.get(propName) : null;
    }

    @Override
    public void setNodeProp(Node node, Property prop) {

        for (int i = 0; i <= REPLACE_RETRY; i++) {
            /* Get a copy of the property map */
            Map<String, Property> propMapCurr = getNodeProps(node);
            if (propMapCurr == null) {
                return;
            }

            Map<String, Property> propMap = new HashMap<String, Property>(propMapCurr);
            propMap.put(prop.getName(), prop);

            if (nodeProps.replace(node, propMapCurr, propMap)) {
                return;
            }
        }
        log.warn("Cluster conflict: Unable to add property {} to node {}.", prop.getName(), node.getID());
    }

    @Override
    public Status removeNodeProp(Node node, String propName) {
        for (int i = 0; i <= REPLACE_RETRY; i++) {
            Map<String, Property> propMapCurr = getNodeProps(node);
            if (propMapCurr != null) {
                if (!propMapCurr.containsKey(propName)) {
                    return new Status(StatusCode.SUCCESS);
                }
                Map<String, Property> propMap = new HashMap<String, Property>(propMapCurr);
                propMap.remove(propName);
                if (nodeProps.replace(node, propMapCurr, propMap)) {
                    return new Status(StatusCode.SUCCESS);
                }
            } else {
                return new Status(StatusCode.SUCCESS);
            }
        }
        String msg = "Cluster conflict: Unable to remove property " + propName + " for node " + node.getID();
        return new Status(StatusCode.CONFLICT, msg);
    }

    @Override
    public Status removeNodeAllProps(Node node) {
        this.nodeProps.remove(node);
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Set<NodeConnector> getUpNodeConnectors(Node node) {
        if (nodeConnectorProps == null) {
            return null;
        }

        Set<NodeConnector> nodeConnectorSet = new HashSet<NodeConnector>();
        for (Map.Entry<NodeConnector, Map<String, Property>> entry : nodeConnectorProps.entrySet()) {
            NodeConnector nodeConnector = entry.getKey();
            if (!nodeConnector.getNode().equals(node)) {
                continue;
            }
            if (isNodeConnectorEnabled(nodeConnector)) {
                nodeConnectorSet.add(nodeConnector);
            }
        }

        return nodeConnectorSet;
    }

    @Override
    public Set<NodeConnector> getNodeConnectors(Node node) {
        if (nodeConnectorProps == null) {
            return null;
        }

        Set<NodeConnector> nodeConnectorSet = new HashSet<NodeConnector>();
        for (Map.Entry<NodeConnector, Map<String, Property>> entry : nodeConnectorProps.entrySet()) {
            NodeConnector nodeConnector = entry.getKey();
            if (!nodeConnector.getNode().equals(node)) {
                continue;
            }
            nodeConnectorSet.add(nodeConnector);
        }

        return nodeConnectorSet;
    }

    @Override
    public Set<NodeConnector> getPhysicalNodeConnectors(Node node) {
        if (nodeConnectorProps == null) {
            return null;
        }

        Set<NodeConnector> nodeConnectorSet = new HashSet<NodeConnector>();
        for (Map.Entry<NodeConnector, Map<String, Property>> entry : nodeConnectorProps.entrySet()) {
            NodeConnector nodeConnector = entry.getKey();
            if (!nodeConnector.getNode().equals(node)
                    || isSpecial(nodeConnector)) {
                continue;
            }
            nodeConnectorSet.add(nodeConnector);
        }

        return nodeConnectorSet;
    }

    @Override
    public Map<String, Property> getNodeConnectorProps(NodeConnector nodeConnector) {
        Map<String, Property> rv = new HashMap<String, Property>();
        if (this.nodeConnectorProps != null) {
            rv = this.nodeConnectorProps.get(nodeConnector);
            if (rv != null) {
                rv = new HashMap<String, Property>(rv);
            }
        }
        return rv;
    }

    @Override
    public Property getNodeConnectorProp(NodeConnector nodeConnector,
            String propName) {
        Map<String, Property> propMap = getNodeConnectorProps(nodeConnector);
        return (propMap != null) ? propMap.get(propName) : null;
    }

    @Override
    public byte[] getControllerMAC() {
        MacAddress macProperty = (MacAddress)controllerProps.get(MacAddress.name);
        return (macProperty == null) ? null : macProperty.getMacAddress();
    }

    @Override
    public NodeConnector getNodeConnector(Node node, String nodeConnectorName) {
        if (nodeConnectorNames == null) {
            return null;
        }

        Map<String, NodeConnector> map = nodeConnectorNames.get(node);
        if (map == null) {
            return null;
        }

        return map.get(nodeConnectorName);
    }

    /**
     * Adds a node connector and its property if any
     *
     * @param nodeConnector
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propName
     *            name of {@link org.opendaylight.controller.sal.core.Property}
     * @return success or failed reason
     */
    @Override
    public Status addNodeConnectorProp(NodeConnector nodeConnector,
            Property prop) {
        Map<String, Property> propMapCurr = getNodeConnectorProps(nodeConnector);
        Map<String, Property> propMap = (propMapCurr == null) ? new HashMap<String, Property>()
                : new HashMap<String, Property>(propMapCurr);

        String msg = "Cluster conflict: Unable to add NodeConnector Property.";
        // Just add the nodeConnector if prop is not available (in a non-default
        // container)
        if (prop == null) {
            if (propMapCurr == null) {
                if (nodeConnectorProps.putIfAbsent(nodeConnector, propMap) != null) {
                    return new Status(StatusCode.CONFLICT, msg);
                }
            } else {
                if (!nodeConnectorProps.replace(nodeConnector, propMapCurr, propMap)) {
                    return new Status(StatusCode.CONFLICT, msg);
                }
            }
            return new Status(StatusCode.SUCCESS);
        }

        propMap.put(prop.getName(), prop);
        if (propMapCurr == null) {
            if (nodeConnectorProps.putIfAbsent(nodeConnector, propMap) != null) {
                return new Status(StatusCode.CONFLICT, msg);
            }
        } else {
            if (!nodeConnectorProps.replace(nodeConnector, propMapCurr, propMap)) {
                return new Status(StatusCode.CONFLICT, msg);
            }
        }

        if (prop.getName().equals(Name.NamePropName)) {
            if (nodeConnectorNames != null) {
                Node node = nodeConnector.getNode();
                Map<String, NodeConnector> mapCurr = nodeConnectorNames.get(node);
                Map<String, NodeConnector> map = new HashMap<String, NodeConnector>();
                if (mapCurr != null) {
                    for (Map.Entry<String, NodeConnector> entry : mapCurr.entrySet()) {
                        String s = entry.getKey();
                        try {
                            map.put(s, new NodeConnector(entry.getValue()));
                        } catch (ConstructionException e) {
                            log.error("An error occured",e);
                        }
                    }
                }

                map.put(((Name) prop).getValue(), nodeConnector);
                if (mapCurr == null) {
                    if (nodeConnectorNames.putIfAbsent(node, map) != null) {
                        // TODO: recovery using Transactionality
                        return new Status(StatusCode.CONFLICT, msg);
                    }
                } else {
                    if (!nodeConnectorNames.replace(node, mapCurr, map)) {
                        // TODO: recovery using Transactionality
                        return new Status(StatusCode.CONFLICT, msg);
                    }
                }
            }
        }

        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Removes one property of a node connector
     *
     * @param nodeConnector
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propName
     *            name of {@link org.opendaylight.controller.sal.core.Property}
     * @return success or failed reason
     */
    @Override
    public Status removeNodeConnectorProp(NodeConnector nodeConnector, String propName) {
        Map<String, Property> propMapCurr = getNodeConnectorProps(nodeConnector);

        if (propMapCurr == null) {
            /* Nothing to remove */
            return new Status(StatusCode.SUCCESS);
        }

        Map<String, Property> propMap = new HashMap<String, Property>(propMapCurr);
        propMap.remove(propName);
        boolean result = nodeConnectorProps.replace(nodeConnector, propMapCurr, propMap);
        String msg = "Cluster conflict: Unable to remove NodeConnector property.";
        if (!result) {
            return new Status(StatusCode.CONFLICT, msg);
        }

        if (propName.equals(Name.NamePropName)) {
            if (nodeConnectorNames != null) {
                Name name = ((Name) getNodeConnectorProp(nodeConnector, Name.NamePropName));
                if (name != null) {
                    Node node = nodeConnector.getNode();
                    Map<String, NodeConnector> mapCurr = nodeConnectorNames.get(node);
                    if (mapCurr != null) {
                        Map<String, NodeConnector> map = new HashMap<String, NodeConnector>();
                        for (Map.Entry<String, NodeConnector> entry : mapCurr.entrySet()) {
                            String s = entry.getKey();
                            try {
                                map.put(s, new NodeConnector(entry.getValue()));
                            } catch (ConstructionException e) {
                                log.error("An error occured",e);
                            }
                        }
                        map.remove(name.getValue());
                        if (!nodeConnectorNames.replace(node, mapCurr, map)) {
                            // TODO: recovery using Transactionality
                            return new Status(StatusCode.CONFLICT, msg);
                        }
                    }
                }
            }
        }

        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Removes all the properties of a node connector
     *
     * @param nodeConnector
     *            {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return success or failed reason
     */
    @Override
    public Status removeNodeConnectorAllProps(NodeConnector nodeConnector) {
        if (nodeConnectorNames != null) {
            Name name = ((Name) getNodeConnectorProp(nodeConnector, Name.NamePropName));
            if (name != null) {
                Node node = nodeConnector.getNode();
                Map<String, NodeConnector> mapCurr = nodeConnectorNames.get(node);
                if (mapCurr != null) {
                    Map<String, NodeConnector> map = new HashMap<String, NodeConnector>();
                    for (Map.Entry<String, NodeConnector> entry : mapCurr.entrySet()) {
                        String s = entry.getKey();
                        try {
                            map.put(s, new NodeConnector(entry.getValue()));
                        } catch (ConstructionException e) {
                            log.error("An error occured",e);
                        }
                    }
                    map.remove(name.getValue());
                    if (map.isEmpty()) {
                        nodeConnectorNames.remove(node);
                    } else {
                        if (!nodeConnectorNames.replace(node, mapCurr, map)) {
                            log.warn("Cluster conflict: Unable remove Name property of nodeconnector {}, skip.",
                                    nodeConnector.getID());
                        }
                    }
                }

            }
        }
        nodeConnectorProps.remove(nodeConnector);

        return new Status(StatusCode.SUCCESS);
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
            log.trace("Running containerName: {}", this.containerName);
        } else {
            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
        isDefaultContainer = containerName.equals(GlobalConstants.DEFAULT
                .toString());

    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        shutDown();
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        startUp();

        /*
         * Read startup and build database if we are the coordinator
         */
        loadSubnetConfiguration();
        loadSpanConfiguration();
        loadSwitchConfiguration();

        // OSGI console
        registerWithOSGIConsole();
    }

    /**
     * Function called after registered the service in OSGi service registry.
     */
    void started() {
        // solicit for existing inventories
        getInventories();
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    public void setConfigurationContainerService(IConfigurationContainerService service) {
        log.trace("Got configuration service set request {}", service);
        this.configurationService = service;
    }

    public void unsetConfigurationContainerService(IConfigurationContainerService service) {
        log.trace("Got configuration service UNset request");
        this.configurationService = null;
    }

    public void setInventoryService(IInventoryService service) {
        log.trace("Got inventory service set request {}", service);
        this.inventoryService = service;

        // solicit for existing inventories
        getInventories();
    }

    public void unsetInventoryService(IInventoryService service) {
        log.trace("Got a service UNset request");
        this.inventoryService = null;

        // clear existing inventories
        clearInventories();
    }

    public void setStatisticsManager(IStatisticsManager statisticsManager) {
        log.trace("Got statistics manager set request {}", statisticsManager);
        this.statisticsManager = statisticsManager;
    }

    public void unsetStatisticsManager(IStatisticsManager statisticsManager) {
        log.trace("Got statistics manager UNset request");
        this.statisticsManager = null;
    }

    public void setSwitchManagerAware(ISwitchManagerAware service) {
        log.trace("Got inventory service set request {}", service);
        if (this.switchManagerAware != null) {
            this.switchManagerAware.add(service);
        }

        // bulk update for newly joined
        switchManagerAwareNotify(service);
    }

    public void unsetSwitchManagerAware(ISwitchManagerAware service) {
        log.trace("Got a service UNset request");
        if (this.switchManagerAware != null) {
            this.switchManagerAware.remove(service);
        }
    }

    public void setInventoryListener(IInventoryListener service) {
        log.trace("Got inventory listener set request {}", service);
        if (this.inventoryListeners != null) {
            this.inventoryListeners.add(service);
        }

        // bulk update for newly joined
        bulkUpdateService(service);
    }

    public void unsetInventoryListener(IInventoryListener service) {
        log.trace("Got a service UNset request");
        if (this.inventoryListeners != null) {
            this.inventoryListeners.remove(service);
        }
    }

    public void setSpanAware(ISpanAware service) {
        log.trace("Got SpanAware set request {}", service);
        if (this.spanAware != null) {
            this.spanAware.add(service);
        }

        // bulk update for newly joined
        spanAwareNotify(service);
    }

    public void unsetSpanAware(ISpanAware service) {
        log.trace("Got a service UNset request");
        if (this.spanAware != null) {
            this.spanAware.remove(service);
        }
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.trace("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.trace("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    public void setControllerProperties(IControllerProperties controllerProperties) {
        log.trace("Got controller properties set request {}", controllerProperties);
        this.controllerProperties = controllerProperties;
    }

    public void unsetControllerProperties(IControllerProperties controllerProperties) {
        log.trace("Got controller properties UNset request");
        this.controllerProperties = null;
    }

    private void getInventories() {
        if (inventoryService == null) {
            log.trace("inventory service not avaiable");
            return;
        }

        Map<Node, Map<String, Property>> nodeProp = this.inventoryService.getNodeProps();
        for (Map.Entry<Node, Map<String, Property>> entry : nodeProp.entrySet()) {
            Node node = entry.getKey();
            log.debug("getInventories: {} added for container {}", new Object[] { node, containerName });
            Map<String, Property> propMap = entry.getValue();
            Set<Property> props = new HashSet<Property>();
            for (Property property : propMap.values()) {
                props.add(property);
            }
            addNode(node, props);
        }

        Map<NodeConnector, Map<String, Property>> nodeConnectorProp = this.inventoryService.getNodeConnectorProps();
        for (Map.Entry<NodeConnector, Map<String, Property>> entry : nodeConnectorProp.entrySet()) {
            Map<String, Property> propMap = entry.getValue();
            for (Property property : propMap.values()) {
                addNodeConnectorProp(entry.getKey(), property);
            }
        }
    }

    private void clearInventories() {
        nodeProps.clear();
        nodeConnectorProps.clear();
        nodeConnectorNames.clear();
        spanNodeConnectors.clear();
    }

    private void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        synchronized (inventoryListeners) {
            for (IInventoryListener service : inventoryListeners) {
                service.notifyNode(node, type, propMap);
            }
        }
    }

    private void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {
        synchronized (inventoryListeners) {
            for (IInventoryListener service : inventoryListeners) {
                service.notifyNodeConnector(nodeConnector, type, propMap);
            }
        }
    }

    /*
     * For those joined late, bring them up-to-date.
     */
    private void switchManagerAwareNotify(ISwitchManagerAware service) {
        for (Subnet sub : subnets.values()) {
            service.subnetNotify(sub, true);
        }

        for (Node node : getNodes()) {
            SwitchConfig sc = getSwitchConfig(node.toString());
            if ((sc != null) && isDefaultContainer) {
                ForwardingMode mode = (ForwardingMode) sc.getProperty(ForwardingMode.name);
                service.modeChangeNotify(node, (mode == null) ? false : mode.isProactive());
            }
        }
    }

    private void bulkUpdateService(IInventoryListener service) {
        Map<String, Property> propMap;
        UpdateType type = UpdateType.ADDED;

        for (Node node : getNodes()) {
            propMap = nodeProps.get(node);
            service.notifyNode(node, type, propMap);
        }

        for (Map.Entry<NodeConnector, Map<String, Property>> entry : nodeConnectorProps.entrySet()) {
            NodeConnector nodeConnector = entry.getKey();
            propMap = nodeConnectorProps.get(nodeConnector);
            service.notifyNodeConnector(nodeConnector, type, propMap);
        }
    }

    private void spanAwareNotify(ISpanAware service) {
        for (Node node : getNodes()) {
            for (SpanConfig conf : getSpanConfigList(node)) {
                service.spanUpdate(node, conf.getPortArrayList(), true);
            }
        }
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    @Override
    public Boolean isNodeConnectorEnabled(NodeConnector nodeConnector) {
        if (nodeConnector == null) {
            return false;
        }

        Config config = (Config) getNodeConnectorProp(nodeConnector,
                Config.ConfigPropName);
        State state = (State) getNodeConnectorProp(nodeConnector,
                State.StatePropName);
        return ((config != null) && (config.getValue() == Config.ADMIN_UP)
                && (state != null) && (state.getValue() == State.EDGE_UP));
    }

    @Override
    public boolean doesNodeConnectorExist(NodeConnector nc) {
        return (nc != null && nodeConnectorProps != null
                && nodeConnectorProps.containsKey(nc));
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---Switch Manager---\n");
        help.append("\t pencs <node id>        - Print enabled node connectors for a given node\n");
        help.append("\t pdm <node id>          - Print switch ports in device map\n");
        return help.toString();
    }

    public void _pencs(CommandInterpreter ci) {
        String st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter node id");
            return;
        }

        Node node = Node.fromString(st);
        if (node == null) {
            ci.println("Please enter node id");
            return;
        }

        Set<NodeConnector> nodeConnectorSet = getUpNodeConnectors(node);
        if (nodeConnectorSet == null) {
            return;
        }
        for (NodeConnector nodeConnector : nodeConnectorSet) {
            if (nodeConnector == null) {
                continue;
            }
            ci.println(nodeConnector);
        }
        ci.println("Total number of NodeConnectors: " + nodeConnectorSet.size());
    }

    public void _pdm(CommandInterpreter ci) {
        String st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter node id");
            return;
        }

        Node node = Node.fromString(st);
        if (node == null) {
            ci.println("Please enter node id");
            return;
        }

        Switch sw = getSwitchByNode(node);

        ci.println("          NodeConnector                        Name");

        Set<NodeConnector> nodeConnectorSet = sw.getNodeConnectors();
        String nodeConnectorName;
        if (nodeConnectorSet != null && nodeConnectorSet.size() > 0) {
            for (NodeConnector nodeConnector : nodeConnectorSet) {
                Map<String, Property> propMap = getNodeConnectorProps(nodeConnector);
                nodeConnectorName = (propMap == null) ? null : ((Name) propMap
                        .get(Name.NamePropName)).getValue();
                if (nodeConnectorName != null) {
                    Node nd = nodeConnector.getNode();
                    if (!nd.equals(node)) {
                        log.debug("node not match {} {}", nd, node);
                    }
                    Map<String, NodeConnector> map = nodeConnectorNames
                            .get(node);
                    if (map != null) {
                        NodeConnector nc = map.get(nodeConnectorName);
                        if (nc == null) {
                            log.debug("no nodeConnector named {}",
                                    nodeConnectorName);
                        } else if (!nc.equals(nodeConnector)) {
                            log.debug("nodeConnector not match {} {}", nc,
                                    nodeConnector);
                        }
                    }
                }

                ci.println(nodeConnector
                        + "            "
                        + ((nodeConnectorName == null) ? "" : nodeConnectorName)
                        + "(" + nodeConnector.getID() + ")");
            }
            ci.println("Total number of NodeConnectors: "
                    + nodeConnectorSet.size());
        }
    }

    @Override
    public byte[] getNodeMAC(Node node) {
        MacAddress mac = (MacAddress) this.getNodeProp(node,
                MacAddress.name);
        return (mac != null) ? mac.getMacAddress() : null;
    }

    @Override
    public boolean isSpecial(NodeConnector p) {
        if (p.getType().equals(NodeConnectorIDType.CONTROLLER)
                || p.getType().equals(NodeConnectorIDType.ALL)
                || p.getType().equals(NodeConnectorIDType.SWSTACK)
                || p.getType().equals(NodeConnectorIDType.HWPATH)) {
            return true;
        }
        return false;
    }

    /*
     * Add span configuration to local cache and notify clients
     */
    private void addSpanPorts(Node node, List<NodeConnector> nodeConnectors) {
        List<NodeConnector> ncLists = new ArrayList<NodeConnector>();

        for (NodeConnector nodeConnector : nodeConnectors) {
            if (!spanNodeConnectors.contains(nodeConnector)) {
                ncLists.add(nodeConnector);
            }
        }

        if (ncLists.size() > 0) {
            spanNodeConnectors.addAll(ncLists);
            notifySpanPortChange(node, ncLists, true);
        }
    }

    private void addSpanPorts(Node node) {
        for (SpanConfig conf : getSpanConfigList(node)) {
            addSpanPorts(node, conf.getPortArrayList());
        }
    }

    private void addSpanPort(NodeConnector nodeConnector) {
        // only add if span is configured on this nodeConnector
        for (SpanConfig conf : getSpanConfigList(nodeConnector.getNode())) {
            if (conf.getPortArrayList().contains(nodeConnector)) {
                List<NodeConnector> ncList = new ArrayList<NodeConnector>();
                ncList.add(nodeConnector);
                addSpanPorts(nodeConnector.getNode(), ncList);
                return;
            }
        }
    }

    /*
     * Remove span configuration to local cache and notify clients
     */
    private void removeSpanPorts(Node node, List<NodeConnector> nodeConnectors) {
        List<NodeConnector> ncLists = new ArrayList<NodeConnector>();

        for (NodeConnector nodeConnector : nodeConnectors) {
            if (spanNodeConnectors.contains(nodeConnector)) {
                ncLists.add(nodeConnector);
            }
        }

        if (ncLists.size() > 0) {
            spanNodeConnectors.removeAll(ncLists);
            notifySpanPortChange(node, ncLists, false);
        }
    }

    private void removeSpanPorts(Node node) {
        for (SpanConfig conf : getSpanConfigList(node)) {
            addSpanPorts(node, conf.getPortArrayList());
        }
    }

    private void removeSpanPort(NodeConnector nodeConnector) {
        if (spanNodeConnectors.contains(nodeConnector)) {
            List<NodeConnector> ncLists = new ArrayList<NodeConnector>();
            ncLists.add(nodeConnector);
            removeSpanPorts(nodeConnector.getNode(), ncLists);
        }
    }

    private void addNodeProps(Node node, Map<String, Property> propMap) {
        if (propMap == null) {
            propMap = new HashMap<String, Property>();
        }
        nodeProps.put(node, propMap);
    }

    @Override
    public Status saveConfiguration() {
        return saveSwitchConfig();
    }

    /**
     * Creates a Name/Tier/Bandwidth/MacAddress(controller property) Property
     * object based on given property name and value. Other property types are
     * not supported yet.
     *
     * @param propName
     *            Name of the Property
     * @param propValue
     *            Value of the Property
     * @return {@link org.opendaylight.controller.sal.core.Property}
     */
    @Override
    public Property createProperty(String propName, String propValue) {
        if (propName == null) {
            log.debug("propName is null");
            return null;
        }
        if (propValue == null) {
            log.debug("propValue is null");
            return null;
        }

        try {
            if (propName.equalsIgnoreCase(Description.propertyName)) {
                return new Description(propValue);
            } else if (propName.equalsIgnoreCase(Tier.TierPropName)) {
                int tier = Integer.parseInt(propValue);
                return new Tier(tier);
            } else if (propName.equalsIgnoreCase(Bandwidth.BandwidthPropName)) {
                long bw = Long.parseLong(propValue);
                return new Bandwidth(bw);
            } else if (propName.equalsIgnoreCase(ForwardingMode.name)) {
                int mode = Integer.parseInt(propValue);
                return new ForwardingMode(mode);
            } else if (propName.equalsIgnoreCase(MacAddress.name)){
                return new MacAddress(propValue);
            }
            else {
                log.debug("Not able to create {} property", propName);
            }
        } catch (Exception e) {
            log.debug("createProperty caught exception {}", e.getMessage());
        }

        return null;
    }


    @SuppressWarnings("deprecation")
    @Override
    public String getNodeDescription(Node node) {
        // Check first if user configured a name
        SwitchConfig config = getSwitchConfig(node.toString());
        if (config != null) {
            String configuredDesc = config.getNodeDescription();
            if (configuredDesc != null && !configuredDesc.isEmpty()) {
                return configuredDesc;
            }
        }

        // No name configured by user, get the node advertised name
        Description desc = (Description) getNodeProp(node,
                Description.propertyName);
        return (desc == null /* || desc.getValue().equalsIgnoreCase("none") */) ? ""
                : desc.getValue();
    }

    @Override
    public Set<Switch> getConfiguredNotConnectedSwitches() {
        Set<Switch> configuredNotConnectedSwitches = new HashSet<Switch>();
        if (this.inventoryService == null) {
            log.trace("inventory service not available");
            return configuredNotConnectedSwitches;
        }

        Set<Node> configuredNotConnectedNodes = this.inventoryService.getConfiguredNotConnectedNodes();
        if (configuredNotConnectedNodes != null) {
            for (Node node : configuredNotConnectedNodes) {
                Switch sw = getSwitchByNode(node);
                configuredNotConnectedSwitches.add(sw);
            }
        }
        return configuredNotConnectedSwitches;
    }

}
