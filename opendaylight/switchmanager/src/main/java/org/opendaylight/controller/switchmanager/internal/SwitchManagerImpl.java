
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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
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
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.Description;
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
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
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
public class SwitchManagerImpl implements ISwitchManager,
        IConfigurationContainerAware, IObjectReader,
        ICacheUpdateAware<Long, String>, IListenInventoryUpdates,
        CommandProvider {
    private static Logger log = LoggerFactory
            .getLogger(SwitchManagerImpl.class);
    private static String ROOT = GlobalConstants.STARTUPHOME.toString();
    private static final String SAVE = "Save";
    private String subnetFileName = null, spanFileName = null,
            switchConfigFileName = null;
    private List<NodeConnector> spanNodeConnectors = new CopyOnWriteArrayList<NodeConnector>();
    private ConcurrentMap<InetAddress, Subnet> subnets; // set of Subnets keyed by the InetAddress
    private ConcurrentMap<String, SubnetConfig> subnetsConfigList;
    private ConcurrentMap<Integer, SpanConfig> spanConfigList;
    private ConcurrentMap<String, SwitchConfig> nodeConfigList; // manually configured parameters for the node, like name and tier
    private ConcurrentMap<Long, String> configSaveEvent;
    private ConcurrentMap<Node, Map<String, Property>> nodeProps; // properties are maintained in default container only
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps; // properties are maintained in default container only
    private ConcurrentMap<Node, Map<String, NodeConnector>> nodeConnectorNames;
    private IInventoryService inventoryService;
    private Set<ISwitchManagerAware> switchManagerAware = Collections
            .synchronizedSet(new HashSet<ISwitchManagerAware>());
    private Set<IInventoryListener> inventoryListeners = Collections
            .synchronizedSet(new HashSet<IInventoryListener>());
    private Set<ISpanAware> spanAware = Collections
            .synchronizedSet(new HashSet<ISpanAware>());
    private byte[] MAC;
    private static boolean hostRefresh = true;
    private int hostRetryCount = 5;
    private IClusterContainerServices clusterContainerService = null;
    private String containerName = null;
    private boolean isDefaultContainer = true;
    
    public enum ReasonCode {
        SUCCESS("Success"), FAILURE("Failure"), INVALID_CONF(
                "Invalid Configuration"), EXIST("Entry Already Exist"), CONFLICT(
                "Configuration Conflict with Existing Entry");

        private String name;

        private ReasonCode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public void notifySubnetChange(Subnet sub, boolean add) {
        synchronized (switchManagerAware) {
            for (Object subAware : switchManagerAware) {
                try {
                    ((ISwitchManagerAware) subAware).subnetNotify(sub, add);
                } catch (Exception e) {
                    log.error("Failed to notify Subnet change", e);
                }
            }
        }
    }

    public void notifySpanPortChange(Node node, List<NodeConnector> ports,
            boolean add) {
        synchronized (spanAware) {
            for (Object sa : spanAware) {
                try {
                    ((ISpanAware) sa).spanUpdate(node, ports, add);
                } catch (Exception e) {
                    log.error("Failed to notify Span Interface change", e);
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
                    log.error("Failed to notify Subnet change", e);
                }
            }
        }
    }

    public void startUp() {
        // Initialize configuration file names
        subnetFileName = ROOT + "subnets" + this.getContainerName() + ".conf";
        spanFileName = ROOT + "spanPorts_" + this.getContainerName() + ".conf";
        switchConfigFileName = ROOT + "switchConfig_" + this.getContainerName()
                + ".conf";

        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();

        /*
         * Read startup and build database if we have not already gotten the
         * configurations synced from another node
         */
        if (subnetsConfigList.isEmpty())
            loadSubnetConfiguration();
        if (spanConfigList.isEmpty())
            loadSpanConfiguration();
        if (nodeConfigList.isEmpty())
            loadSwitchConfiguration();

        MAC = getHardwareMAC();
    }

    public void shutDown() {
        destroyCaches(this.getContainerName());
    }

    @SuppressWarnings("deprecation")
	private void allocateCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't create cache");
            return;
        }

        try {
            clusterContainerService.createCache(
                    "switchmanager.subnetsConfigList", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.spanConfigList",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.nodeConfigList",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.subnets",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache(
                    "switchmanager.configSaveEvent", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache("switchmanager.nodeProps",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache(
                    "switchmanager.nodeConnectorProps", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache(
                    "switchmanager.nodeConnectorNames", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            log.error("\nCache already exits - destroy and recreate if needed");
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCaches() {
        if (this.clusterContainerService == null) {
            log
                    .info("un-initialized clusterContainerService, can't create cache");
            return;
        }

        subnetsConfigList = (ConcurrentMap<String, SubnetConfig>) clusterContainerService
                .getCache("switchmanager.subnetsConfigList");
        if (subnetsConfigList == null) {
            log.error("\nFailed to get cache for subnetsConfigList");
        }

        spanConfigList = (ConcurrentMap<Integer, SpanConfig>) clusterContainerService
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

        configSaveEvent = (ConcurrentMap<Long, String>) clusterContainerService
                .getCache("switchmanager.configSaveEvent");
        if (configSaveEvent == null) {
            log.error("\nFailed to get cache for configSaveEvent");
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
    }

    void nonClusterObjectCreate() {
        subnetsConfigList = new ConcurrentHashMap<String, SubnetConfig>();
        spanConfigList = new ConcurrentHashMap<Integer, SpanConfig>();
        nodeConfigList = new ConcurrentHashMap<String, SwitchConfig>();
        subnets = new ConcurrentHashMap<InetAddress, Subnet>();
        configSaveEvent = new ConcurrentHashMap<Long, String>();
        nodeProps = new ConcurrentHashMap<Node, Map<String, Property>>();
        nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
        nodeConnectorNames = new ConcurrentHashMap<Node, Map<String, NodeConnector>>();
    }

    @SuppressWarnings("deprecation")
	private void destroyCaches(String container) {
        if (this.clusterContainerService == null) {
            log
                    .info("un-initialized clusterContainerService, can't create cache");
            return;
        }

        clusterContainerService.destroyCache("switchmanager.subnetsConfigList");
        clusterContainerService.destroyCache("switchmanager.spanConfigList");
        clusterContainerService.destroyCache("switchmanager.nodeConfigList");
        clusterContainerService.destroyCache("switchmanager.subnets");
        clusterContainerService.destroyCache("switchmanager.configSaveEvent");
        clusterContainerService.destroyCache("switchmanager.nodeProps");
        clusterContainerService
                .destroyCache("switchmanager.nodeConnectorProps");
        clusterContainerService
                .destroyCache("switchmanager.nodeConnectorNames");
        nonClusterObjectCreate();
    }

    public List<SubnetConfig> getSubnetsConfigList() {
        return new ArrayList<SubnetConfig>(subnetsConfigList.values());
    }

    @Override
    public SubnetConfig getSubnetConfig(String subnet) {
        return subnetsConfigList.get(subnet);
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

    public SwitchConfig getSwitchConfig(String switchId) {
        return nodeConfigList.get(switchId);
    }

    public Switch getSwitchByNode(Node node) {
        Switch sw = new Switch(node);
        sw.setNode(node);

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

    public List<Switch> getNetworkDevices() {
        Set<Node> nodeSet = getNodes();
        List<Switch> swList = new ArrayList<Switch>();
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                swList.add(getSwitchByNode(node));
            }
        }

        return swList;
    }

    private void updateConfig(SubnetConfig conf, boolean add) {
        if (add) {
            subnetsConfigList.put(conf.getName(), conf);
        } else {
            subnetsConfigList.remove(conf.getName());
        }
    }

    private void updateDatabase(SubnetConfig conf, boolean add) {
        Subnet subnet = subnets.get(conf.getIPnum());
        if (add) {
            if (subnet == null) {
                subnet = new Subnet(conf);
            }
            // In case of API3 call we may receive the ports along with the
            // subnet creation
            if (!conf.isGlobal()) {
                Set<NodeConnector> sp = conf.getSubnetNodeConnectors();
                subnet.addNodeConnectors(sp);
            }
            subnets.put(conf.getIPnum(), subnet);
        } else { // This is the deletion of the whole subnet
            if (subnet == null)
                return;
            subnets.remove(conf.getIPnum());
        }
    }

    private Status semanticCheck(SubnetConfig conf) {
        Subnet newSubnet = new Subnet(conf);
        Set<InetAddress> IPs = subnets.keySet();
        if (IPs == null) {
        	return new Status(StatusCode.SUCCESS, null);
        }
        for (InetAddress i : IPs) {
            Subnet existingSubnet = subnets.get(i);
            if ((existingSubnet != null)
                    && !existingSubnet.isMutualExclusive(newSubnet)) {
                return new Status(StatusCode.CONFLICT, null);
            }
        }
        return new Status(StatusCode.SUCCESS, null);
    }

    private Status addRemoveSubnet(SubnetConfig conf, boolean add) {
        // Valid config check
        if (!conf.isValidConfig()) {
        	String msg = "Invalid Subnet configuration";
            log.warn(msg);
            return new Status(StatusCode.BADREQUEST, msg);
        }

        if (add) {
            // Presence check
            if (subnetsConfigList.containsKey(conf.getName())) {
            	return new Status(StatusCode.CONFLICT, 
            			"Same subnet config already exists");
            }
            // Semantyc check
            Status rc = semanticCheck(conf);
            if (!rc.isSuccess()) {
                return rc;
            }
        }
        // Update Configuration
        updateConfig(conf, add);

        // Update Database
        updateDatabase(conf, add);

        return new Status(StatusCode.SUCCESS, null);
    }

    /**
     * Adds Subnet configured in GUI or API3
     */
    public Status addSubnet(SubnetConfig conf) {
        return this.addRemoveSubnet(conf, true);
    }

    @Override
    public Status removeSubnet(SubnetConfig conf) {
        return this.addRemoveSubnet(conf, false);
    }

    @Override
    public Status removeSubnet(String name) {
        SubnetConfig conf = subnetsConfigList.get(name);
        if (conf == null) {
            return new Status(StatusCode.SUCCESS, "Subnet not present");
        }
        return this.addRemoveSubnet(conf, false);
    }

    @Override
    public Status addPortsToSubnet(String name, String switchPorts) {
        // Update Configuration
        SubnetConfig conf = subnetsConfigList.get(name);
        if (conf == null) {
            return new Status(StatusCode.NOTFOUND, "Subnet does not exist");
        }
        if (!conf.isValidSwitchPort(switchPorts)) {
        	return new Status(StatusCode.BADREQUEST, "Invalid switchports");
        }

        conf.addNodeConnectors(switchPorts);

        // Update Database
        Subnet sub = subnets.get(conf.getIPnum());
        Set<NodeConnector> sp = conf.getNodeConnectors(switchPorts);
        sub.addNodeConnectors(sp);
        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public Status removePortsFromSubnet(String name, String switchPorts) {
        // Update Configuration
        SubnetConfig conf = subnetsConfigList.get(name);
        if (conf == null) {
        	return new Status(StatusCode.NOTFOUND, "Subnet does not exist");
        }
        conf.removeNodeConnectors(switchPorts);

        // Update Database
        Subnet sub = subnets.get(conf.getIPnum());
        Set<NodeConnector> sp = conf.getNodeConnectors(switchPorts);
        sub.deleteNodeConnectors(sp);
        return new Status(StatusCode.SUCCESS, null);
    }

    public String getContainerName() {
        if (containerName == null) {
            return GlobalConstants.DEFAULT.toString();
        }
        return containerName;
    }

    @Override
    public Subnet getSubnetByNetworkAddress(InetAddress networkAddress) {
        Subnet sub;
        Set<InetAddress> indices = subnets.keySet();
        for (InetAddress i : indices) {
            sub = subnets.get(i);
            if (sub.isSubnetOf(networkAddress)) {
                return sub;
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

    @SuppressWarnings("unchecked")
    private void loadSubnetConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<Integer, SubnetConfig> confList = (ConcurrentMap<Integer, SubnetConfig>) objReader
                .read(this, subnetFileName);

        if (confList == null) {
            return;
        }

        for (SubnetConfig conf : confList.values()) {
            addSubnet(conf);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSpanConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<Integer, SpanConfig> confList = (ConcurrentMap<Integer, SpanConfig>) objReader
                .read(this, spanFileName);

        if (confList == null) {
            return;
        }

        for (SpanConfig conf : confList.values()) {
            addSpanConfig(conf);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSwitchConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, SwitchConfig> confList = (ConcurrentMap<String, SwitchConfig>) objReader
                .read(this, switchConfigFileName);

        if (confList == null) {
            return;
        }

        for (SwitchConfig conf : confList.values()) {
            updateSwitchConfig(conf);
        }
    }

    @Override
    public void updateSwitchConfig(SwitchConfig cfgObject) {
        boolean modeChange = false;

        SwitchConfig sc = nodeConfigList.get(cfgObject.getNodeId());
        if ((sc == null) || !cfgObject.getMode().equals(sc.getMode())) {
            modeChange = true;
        }

        nodeConfigList.put(cfgObject.getNodeId(), cfgObject);
        try {
            // update default container only
            if (isDefaultContainer) {
                String nodeId = cfgObject.getNodeId();
                Node node = Node.fromString(nodeId);
                Map<String, Property> propMap;
                if (nodeProps.get(node) != null) {
                    propMap = nodeProps.get(node);
                } else {
                    propMap = new HashMap<String, Property>();
                }
                Property desc = new Description(cfgObject.getNodeDescription());
                propMap.put(desc.getName(), desc);
                Property tier = new Tier(Integer.parseInt(cfgObject.getTier()));
                propMap.put(tier.getName(), tier);
                addNodeProps(node, propMap);

                log.info("Set Node {}'s Mode to {}", nodeId, cfgObject
                        .getMode());

                if (modeChange) {
                    notifyModeChange(node, cfgObject.isProactive());
                }
            }
        } catch (Exception e) {
            log.debug("updateSwitchConfig: {}", e);
        }
    }

    @Override
    public Status saveSwitchConfig() {
        // Publish the save config event to the cluster nodes
        configSaveEvent.put(new Date().getTime(), SAVE);
        return saveSwitchConfigInternal();
    }

    public Status saveSwitchConfigInternal() {
        Status retS = null, retP = null;
        ObjectWriter objWriter = new ObjectWriter();

        retS = objWriter.write(new ConcurrentHashMap<String, SubnetConfig>(
                subnetsConfigList), subnetFileName);
        retP = objWriter.write(new ConcurrentHashMap<Integer, SpanConfig>(
                spanConfigList), spanFileName);
        retS = objWriter.write(new ConcurrentHashMap<String, SwitchConfig>(
                nodeConfigList), switchConfigFileName);

        if (retS.equals(retP)) {
            if (retS.isSuccess()) {
                return retS;
            } else {
                return new Status(StatusCode.INTERNALERROR, "Save failed");
            }
        } else {
            return new Status(StatusCode.INTERNALERROR, "Partial save failure");
        }
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
        if (spanConfigList.containsKey(conf.hashCode())) {
        	return new Status(StatusCode.CONFLICT, "Same span config exists");
        }

        // Update database and notify clients
        addSpanPorts(conf.getNode(), conf.getPortArrayList());

        // Update configuration
        spanConfigList.put(conf.hashCode(), conf);

        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public Status removeSpanConfig(SpanConfig conf) {
        removeSpanPorts(conf.getNode(), conf.getPortArrayList());

        // Update configuration
        spanConfigList.remove(conf.hashCode());

        return new Status(StatusCode.SUCCESS, null);
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

    @Override
    public void entryCreated(Long key, String cacheName, boolean local) {
    }

    @Override
    public void entryUpdated(Long key, String new_value, String cacheName,
            boolean originLocal) {
        saveSwitchConfigInternal();
    }

    @Override
    public void entryDeleted(Long key, String cacheName, boolean originLocal) {
    }

    private void addNode(Node node, Set<Property> props) {
        log.trace("{} added", node);
        if (nodeProps == null) {
            return;
        }

        Map<String, Property> propMap;
        if (nodeProps.get(node) != null) {
            propMap = nodeProps.get(node);
        } else {
            propMap = new HashMap<String, Property>();
        }

        // copy node properties from plugin
        if (props != null) {
            for (Property prop : props) {
                propMap.put(prop.getName(), prop);
            }
        }

        // copy node properties from config
        boolean proactiveForwarding = false;
        if (nodeConfigList != null) {
            String nodeId = node.toString();
            for (SwitchConfig conf : nodeConfigList.values()) {
                if (conf.getNodeId().equals(nodeId)) {
                    Property description = new Description(conf.getNodeDescription());
                    propMap.put(description.getName(), description);
                    Property tier = new Tier(Integer.parseInt(conf.getTier()));
                    propMap.put(tier.getName(), tier);
                    proactiveForwarding = conf.isProactive();
                    break;
                }
            }
        }
        addNodeProps(node, propMap);

        // check if span ports are configed
        addSpanPorts(node);

        // notify node listeners
        notifyNode(node, UpdateType.ADDED, propMap);
        
        // notify proactive mode forwarding
        if (proactiveForwarding) {
        	notifyModeChange(node, true);
        }
    }

    private void removeNode(Node node) {
        log.trace("{} removed", node);
        if (nodeProps == null)
            return;
        nodeProps.remove(node);

        // check if span ports need to be cleaned up
        removeSpanPorts(node);

        /* notify node listeners */
        notifyNode(node, UpdateType.REMOVED, null);
    }

    private void updateNode(Node node, Set<Property> props) {
        log.trace("{} updated", node);
        if (nodeProps == null) {
            return;
        }

        Map<String, Property> propMap;
        if (nodeProps.get(node) != null) {
            propMap = nodeProps.get(node);
        } else {
            propMap = new HashMap<String, Property>();
        }

        // copy node properties from plugin
        if (props != null) {
            for (Property prop : props) {
                propMap.put(prop.getName(), prop);
            }
        }
        addNodeProps(node, propMap);

        /* notify node listeners */
        notifyNode(node, UpdateType.CHANGED, propMap);
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
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
        Node node = nodeConnector.getNode();
        Map<String, Property> propMap = new HashMap<String, Property>();

        log.trace("{} {}", nodeConnector, type);

        if (nodeConnectorProps == null) {
            return;
        }

        switch (type) {
        case ADDED:
        case CHANGED:
            if (props != null) {
                for (Property prop : props) {
                    addNodeConnectorProp(nodeConnector, prop);
                    propMap.put(prop.getName(), prop);
                }
            } else {
                addNodeConnectorProp(nodeConnector, null);
                addNodeProps(node, null);
            }

            // check if span is configed
            addSpanPort(nodeConnector);
            break;
        case REMOVED:
            removeNodeConnectorAllProps(nodeConnector);
            removeNodeProps(node);

            // clean up span config
            removeSpanPort(nodeConnector);
            break;
        default:
            break;
        }

        notifyNodeConnector(nodeConnector, type, propMap);
    }

    @Override
    public Set<Node> getNodes() {
        return (nodeProps != null) ? new HashSet<Node>(nodeProps.keySet())
                : null;
    }

    /*
     * test utility function which assumes all nodes are OF nodes
     */
    private Node getNode(Long id) {
        Set<Node> nodes = getNodes();
        if (nodes != null) {
            for (Node node : nodes) {
                if (id.equals((Long)node.getID())) {
                    return node;
                }
            }
        }
        return null;
    }

    /*
     * Returns a copy of a list of properties for a given node
     *
     * (non-Javadoc)
     * @see org.opendaylight.controller.switchmanager.ISwitchManager#getNodeProps(org.opendaylight.controller.sal.core.Node)
     */
    @Override
    public Map<String, Property> getNodeProps(Node node) {
        if (isDefaultContainer) {
            Map<String, Property> rv = null;
            if (this.nodeProps != null) {
                rv = this.nodeProps.get(node);
                if (rv != null) {
                    /* make a copy of it */
                    rv = new HashMap<String, Property>(rv);
                }
            }
            return rv;
        } else {
            // get it from default container
            ISwitchManager defaultSwitchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, GlobalConstants.DEFAULT
                            .toString(), this);
            return defaultSwitchManager.getNodeProps(node);
        }
    }

    @Override
    public Property getNodeProp(Node node, String propName) {
        Map<String, Property> propMap = getNodeProps(node);
        return (propMap != null) ? propMap.get(propName) : null;
    }

    @Override
    public void setNodeProp(Node node, Property prop) {
        /* Get a copy of the property map */
        Map<String, Property> propMap = getNodeProps(node);
        if (propMap == null)
            return;

        propMap.put(prop.getName(), prop);
        this.nodeProps.put(node, propMap);
    }

    @Override
    public Status removeNodeProp(Node node, String propName) {
        Map<String, Property> propMap = getNodeProps(node);
        if (propMap != null) {
        	propMap.remove(propName);
        	this.nodeProps.put(node, propMap);
        }
        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public Status removeNodeAllProps(Node node) {
        this.nodeProps.remove(node);
        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public Set<NodeConnector> getUpNodeConnectors(Node node) {
        if (nodeConnectorProps == null)
            return null;

        Set<NodeConnector> nodeConnectorSet = new HashSet<NodeConnector>();
        for (NodeConnector nodeConnector : nodeConnectorProps.keySet()) {
            if (((Long) nodeConnector.getNode().getID())
                    .longValue() != (Long) node.getID())
                continue;
            if (isNodeConnectorEnabled(nodeConnector))
                nodeConnectorSet.add(nodeConnector);
        }

        return nodeConnectorSet;
    }

    @Override
    public Set<NodeConnector> getNodeConnectors(Node node) {
        if (nodeConnectorProps == null)
            return null;

        Set<NodeConnector> nodeConnectorSet = new HashSet<NodeConnector>();
        for (NodeConnector nodeConnector : nodeConnectorProps.keySet()) {
            if (((Long) nodeConnector.getNode().getID())
                    .longValue() != (Long) node.getID())
                continue;
            nodeConnectorSet.add(nodeConnector);
        }

        return nodeConnectorSet;
    }

    @Override
    public Set<NodeConnector> getPhysicalNodeConnectors(Node node) {
        if (nodeConnectorProps == null)
            return null;

        Set<NodeConnector> nodeConnectorSet = new HashSet<NodeConnector>();
        for (NodeConnector nodeConnector : nodeConnectorProps.keySet()) {
            if (!nodeConnector.getNode().equals(node)
                    || isSpecial(nodeConnector)) {
                continue;
            }
            nodeConnectorSet.add(nodeConnector);
        }

        return nodeConnectorSet;
    }

    /*
     * testing utility function which assumes we are dealing with OF Node nodeconnectors only
     */
    @SuppressWarnings("unused")
    private Set<Long> getEnabledNodeConnectorIds(Node node) {
        Set<Long> ids = new HashSet<Long>();
        Set<NodeConnector> nodeConnectors = getUpNodeConnectors(node);

        if (nodeConnectors != null) {
            for (NodeConnector nodeConnector : nodeConnectors) {
                ids.add((Long) nodeConnector.getID());
            }
        }

        return ids;
    }

    @Override
    public Map<String, Property> getNodeConnectorProps(
            NodeConnector nodeConnector) {
        if (isDefaultContainer) {
            Map<String, Property> rv = null;
            if (this.nodeConnectorProps != null) {
                rv = this.nodeConnectorProps.get(nodeConnector);
                if (rv != null) {
                    rv = new HashMap<String, Property>(rv);
                }
            }
            return rv;
        } else {
            // get it from default container
            ISwitchManager defaultSwitchManager = (ISwitchManager) ServiceHelper
                    .getInstance(ISwitchManager.class, GlobalConstants.DEFAULT
                            .toString(), this);
            return defaultSwitchManager.getNodeConnectorProps(nodeConnector);
        }
    }

    @Override
    public Property getNodeConnectorProp(NodeConnector nodeConnector,
            String propName) {
        Map<String, Property> propMap = getNodeConnectorProps(nodeConnector);
        return (propMap != null) ? propMap.get(propName) : null;
    }

    private byte[] getHardwareMAC() {
        Enumeration<NetworkInterface> nis;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e1) {
            e1.printStackTrace();
            return null;
        }
        byte[] MAC = null;
        for (; nis.hasMoreElements();) {
            NetworkInterface ni = nis.nextElement();
            try {
                MAC = ni.getHardwareAddress();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            if (MAC != null) {
                return MAC;
            }
        }
        return null;
    }

    @Override
    public byte[] getControllerMAC() {
        return MAC;
    }

    @Override
    public boolean isHostRefreshEnabled() {
        return hostRefresh;
    }

    @Override
    public int getHostRetryCount() {
        return hostRetryCount;
    }

    @Override
    public NodeConnector getNodeConnector(Node node, String nodeConnectorName) {
        if (nodeConnectorNames == null)
            return null;

        Map<String, NodeConnector> map = nodeConnectorNames.get(node);
        if (map == null)
            return null;

        return map.get(nodeConnectorName);
    }

    /**
     * Adds a node connector and its property if any
     * 
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propName 	 	name of {@link org.opendaylight.controller.sal.core.Property}
     * @return success or failed reason
     */
    @Override
    public Status addNodeConnectorProp(NodeConnector nodeConnector, Property prop) {
        Map<String, Property> propMap = getNodeConnectorProps(nodeConnector);

        if (propMap == null) {
            propMap = new HashMap<String, Property>();
        }

        // Just add the nodeConnector if prop is not available (in a non-default container)
        if (prop == null) {
            nodeConnectorProps.put(nodeConnector, propMap);
            return new Status(StatusCode.SUCCESS, null);
        }

        propMap.put(prop.getName(), prop);
        nodeConnectorProps.put(nodeConnector, propMap);

        if (prop.getName().equals(Name.NamePropName)) {
            if (nodeConnectorNames != null) {
                Node node = nodeConnector.getNode();
                Map<String, NodeConnector> map = nodeConnectorNames.get(node);
                if (map == null) {
                    map = new HashMap<String, NodeConnector>();
                }

                map.put(((Name) prop).getValue(), nodeConnector);
                nodeConnectorNames.put(node, map);
            }
        }

        return new Status(StatusCode.SUCCESS, null);
    }

    /**
     * Removes one property of a node connector
     * 
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propName 	 	name of {@link org.opendaylight.controller.sal.core.Property}
     * @return success or failed reason
     */
    @Override
    public Status removeNodeConnectorProp(NodeConnector nodeConnector, String propName) {
        Map<String, Property> propMap = getNodeConnectorProps(nodeConnector);

        if (propMap == null) {
        	/* Nothing to remove */
            return new Status(StatusCode.SUCCESS, null);
        }

        propMap.remove(propName);
        nodeConnectorProps.put(nodeConnector, propMap);

        if (nodeConnectorNames != null) {
            Name name = ((Name) getNodeConnectorProp(nodeConnector,
                    Name.NamePropName));
            if (name != null) {
                Node node = nodeConnector.getNode();
                Map<String, NodeConnector> map = nodeConnectorNames.get(node);
                if (map != null) {
                    map.remove(name.getValue());
                    nodeConnectorNames.put(node, map);
                }
            }
        }

        return new Status(StatusCode.SUCCESS, null);
    }

    /**
     * Removes all the properties of a node connector
     * 
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return success or failed reason
     */
    @Override
    public Status removeNodeConnectorAllProps(NodeConnector nodeConnector) {
        if (nodeConnectorNames != null) {
            Name name = ((Name) getNodeConnectorProp(nodeConnector,
                    Name.NamePropName));
            if (name != null) {
                Node node = nodeConnector.getNode();
                Map<String, NodeConnector> map = nodeConnectorNames.get(node);
                if (map != null) {
                    map.remove(name.getValue());
                    nodeConnectorNames.put(node, map);
                }
            }
        }
        nodeConnectorProps.remove(nodeConnector);

        return new Status(StatusCode.SUCCESS, null);
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
            log.trace("Running containerName:" + this.containerName);
        } else {
            // In the Global instance case the containerName is empty
            this.containerName = "";
        }
        isDefaultContainer = containerName.equals(GlobalConstants.DEFAULT
                .toString());

        startUp();
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        shutDown();
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        // OSGI console
        registerWithOSGIConsole();
    }

    /**
     * Function called after registered the
     * service in OSGi service registry.
     */
    void started() {
        // solicit for existing inventories
        getInventories();
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
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

    private void getInventories() {
        if (inventoryService == null) {
            log.trace("inventory service not avaiable");
            return;
        }

        nodeProps = this.inventoryService.getNodeProps();
        Set<Node> nodeSet = nodeProps.keySet();
        if (nodeSet != null) {
            for (Node node : nodeSet) {
                addNode(node, null);
            }
        }

        nodeConnectorProps = inventoryService.getNodeConnectorProps();
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
                service.modeChangeNotify(node, sc.isProactive());
            }
        }
    }

    private void bulkUpdateService(IInventoryListener service) {
        for (Node node : getNodes()) {
            service.notifyNode(node, UpdateType.ADDED, null);
        }

        Map<String, Property> propMap = new HashMap<String, Property>();
        propMap.put(State.StatePropName, new State(State.EDGE_UP));
        for (NodeConnector nodeConnector : nodeConnectorProps.keySet()) {
            if (isNodeConnectorEnabled(nodeConnector)) {
                service.notifyNodeConnector(nodeConnector, UpdateType.ADDED,
                        propMap);
            }
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
        if (nodeConnector == null)
            return false;

        Config config = (Config) getNodeConnectorProp(nodeConnector,
                Config.ConfigPropName);
        State state = (State) getNodeConnectorProp(nodeConnector,
                State.StatePropName);
        return ((config != null) && (config.getValue() == Config.ADMIN_UP)
                && (state != null) && (state.getValue() == State.EDGE_UP));
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---Switch Manager---\n");
        help.append("\t pns                    - Print connected nodes\n");
        help.append("\t pncs <node id>         - Print node connectors for a given node\n");
        help.append("\t pencs <node id>        - Print enabled node connectors for a given node\n");
        help.append("\t pdm <node id>          - Print switch ports in device map\n");
        help.append("\t snt <node id> <tier>   - Set node tier number\n");
        help.append("\t hostRefresh <on/off/?> - Enable/Disable/Query host refresh\n");
        help.append("\t hostRetry <count>      - Set host retry count\n");
        return help.toString();
    }

    public void _pns(CommandInterpreter ci) {
        ci.println("           Node                       Type             Name             Tier");
        if (nodeProps == null) {
            return;
        }
        Set<Node> nodeSet = nodeProps.keySet();
        if (nodeSet == null) {
            return;
        }
        for (Node node : nodeSet) {
        	Description desc = ((Description) getNodeProp(node, Description.propertyName));
            Tier tier = ((Tier) getNodeProp(node, Tier.TierPropName));
            String nodeName = (desc == null) ? "" : desc.getValue();
            int tierNum = (tier == null) ? 0 : tier.getValue();
            ci.println(node + "            " + node.getType()
                    + "            " + nodeName + "            " + tierNum);
        }
        ci.println("Total number of Nodes: " + nodeSet.size());
    }

    public void _pencs(CommandInterpreter ci) {
        String st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter node id");
            return;
        }
        Long id = Long.decode(st);

        Node node = NodeCreator.createOFNode(id);
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

    public void _pncs(CommandInterpreter ci) {
        String st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter node id");
            return;
        }
        Long id = Long.decode(st);

        ci.println("          NodeConnector               BandWidth(Gbps)     Admin     State");
        Node node = NodeCreator.createOFNode(id);
        Set<NodeConnector> nodeConnectorSet = getNodeConnectors(node);
        if (nodeConnectorSet == null) {
            return;
        }
        for (NodeConnector nodeConnector : nodeConnectorSet) {
            if (nodeConnector == null) {
                continue;
            }
            Map<String, Property> propMap = getNodeConnectorProps(nodeConnector);
            Bandwidth bw = (Bandwidth) propMap.get(Bandwidth.BandwidthPropName);
            Config config = (Config) propMap.get(Config.ConfigPropName);
            State state = (State) propMap.get(State.StatePropName);
            String out = nodeConnector + "           ";
            out += (bw != null) ? bw.getValue() / Math.pow(10, 9) : "    ";
            out += "             ";
            out += (config != null) ? config.getValue() : " ";
            out += "          ";
            out += (state != null) ? state.getValue() : " ";
            ci.println(out);
        }
        ci.println("Total number of NodeConnectors: " + nodeConnectorSet.size());
    }

    public void _pdm(CommandInterpreter ci) {
        String st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter node id");
            return;
        }
        Object id = Long.decode(st);
        Switch sw = getSwitchByNode(NodeCreator.createOFNode((Long) id));

        ci.println("          NodeConnector                        Name");
        if (sw == null) {
            return;
        }
        Set<NodeConnector> nodeConnectorSet = sw.getNodeConnectors();
        String nodeConnectorName;
        if (nodeConnectorSet != null && nodeConnectorSet.size() > 0) {
            for (NodeConnector nodeConnector : nodeConnectorSet) {
                Map<String, Property> propMap = getNodeConnectorProps(nodeConnector);
                nodeConnectorName = (propMap == null) ? null : ((Name) propMap
                        .get(Name.NamePropName)).getValue();
                if (nodeConnectorName != null) {
                    Node node = nodeConnector.getNode();
                    if (!node.equals(getNode((Long) id))) {
                        log.debug("node not match {} {}", node,
                                getNode((Long) id));
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
                		+ ((nodeConnectorName == null) ? ""
                				: nodeConnectorName) + "("
                        + nodeConnector.getID() + ")");
            }
            ci.println("Total number of NodeConnectors: " + nodeConnectorSet.size());
        }
    }

    public void _snt(CommandInterpreter ci) {
        String st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter node id");
            return;
        }
        Long id = Long.decode(st);

        Node node = NodeCreator.createOFNode(id);

        st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter tier number");
            return;
        }
        Integer tid = Integer.decode(st);
        Tier tier = new Tier(tid);
        setNodeProp(node, tier);
    }

    public void _hostRefresh(CommandInterpreter ci) {
        String mode = ci.nextArgument();
        if (mode == null) {
            ci.println("expecting on/off/?");
            return;
        }
        if (mode.toLowerCase().equals("on"))
            hostRefresh = true;
        else if (mode.toLowerCase().equals("off"))
            hostRefresh = false;
        else if (mode.equals("?")) {
            if (hostRefresh)
                ci.println("host refresh is ON");
            else
                ci.println("host refresh is OFF");
        } else
            ci.println("expecting on/off/?");
        return;
    }

    public void _hostRetry(CommandInterpreter ci) {
        String retry = ci.nextArgument();
        if (retry == null) {
            ci.println("Please enter a valid number. Current retry count is "
                    + hostRetryCount);
            return;
        }
        try {
            hostRetryCount = Integer.parseInt(retry);
        } catch (Exception e) {
            ci.println("Please enter a valid number");
        }
        return;
    }

    @Override
    public byte[] getNodeMAC(Node node) {
        if (node.getType().equals(Node.NodeIDType.OPENFLOW)) {
            byte[] gmac = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
            long dpid = (Long) node.getID();

            for (short i = 0; i < 6; i++) {
                gmac[5 - i] = (byte) dpid;
                dpid >>= 8;
            }
            return gmac;
        }
        return null;
    }

    @Override
    public boolean isSpecial(NodeConnector p) {
        if (p.getType().equals(NodeConnectorIDType.CONTROLLER) ||
            p.getType().equals(NodeConnectorIDType.ALL) ||
            p.getType().equals(NodeConnectorIDType.SWSTACK) ||
            p.getType().equals(NodeConnectorIDType.HWPATH)) {
            return true;
        }
        return false;
    }

    /*
     * Add span configuration to local cache and notify clients
     */
    private void addSpanPorts(Node node, List<NodeConnector> nodeConncetors) {
        List<NodeConnector> ncLists = new ArrayList<NodeConnector>();

        for (NodeConnector nodeConnector : nodeConncetors) {
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

    private void addSpanPort(NodeConnector nodeConncetor) {
        List<NodeConnector> ncLists = new ArrayList<NodeConnector>();
        ncLists.add(nodeConncetor);
        addSpanPorts(nodeConncetor.getNode(), ncLists);
    }

    /*
     * Remove span configuration to local cache and notify clients
     */
    private void removeSpanPorts(Node node, List<NodeConnector> nodeConncetors) {
        List<NodeConnector> ncLists = new ArrayList<NodeConnector>();

        for (NodeConnector nodeConnector : nodeConncetors) {
            if (!spanNodeConnectors.contains(nodeConnector)) {
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

    private void removeSpanPort(NodeConnector nodeConncetor) {
        List<NodeConnector> ncLists = new ArrayList<NodeConnector>();
        ncLists.add(nodeConncetor);
        removeSpanPorts(nodeConncetor.getNode(), ncLists);
    }

    private void addNodeProps(Node node, Map<String, Property> propMap) {
        if (propMap == null) {
            propMap = new HashMap<String, Property>();
        }
        nodeProps.put(node, propMap);
    }

    private void removeNodeProps(Node node) {
        if (getUpNodeConnectors(node).size() == 0) {
            nodeProps.remove(node);
        }
    }

    @Override
    public Status saveConfiguration() {
        return saveSwitchConfig();
    }

	/**
	 * Creates a Name/Tier/Bandwidth Property object based on given property
	 * name and value. Other property types are not supported yet.
	 * 
	 * @param propName  Name of the Property
	 * @param propValue Value of the Property
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
			} else {
				log.debug("Not able to create {} property", propName);
			}
		} catch (Exception e) {
			log.debug(e.getMessage());
		}

		return null;
    }
	
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
        Description desc = (Description) getNodeProp(node, Description.propertyName);
        return (desc == null /*|| desc.getValue().equalsIgnoreCase("none")*/) ?	
        				"" : desc.getValue();
    }
}
