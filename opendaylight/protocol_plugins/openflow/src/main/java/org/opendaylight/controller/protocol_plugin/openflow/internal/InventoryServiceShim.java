/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimExternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimInternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.IOFStatisticsListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitchStateListener;
import org.opendaylight.controller.sal.action.SupportedFlowActions;
import org.opendaylight.controller.sal.connection.ConnectionLocality;
import org.opendaylight.controller.sal.connection.IPluginOutConnectionService;
import org.opendaylight.controller.sal.core.Buffers;
import org.opendaylight.controller.sal.core.Capabilities;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.IContainerAware;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.MacAddress;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Tables;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.OFType;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class describes a shim layer that bridges inventory events from Openflow
 * core to various listeners. The notifications are filtered based on container
 * configurations.
 *
 *
 */
public class InventoryServiceShim implements IContainerListener,
        IMessageListener, ISwitchStateListener, IOFStatisticsListener, IContainerAware {
    protected static final Logger logger = LoggerFactory
            .getLogger(InventoryServiceShim.class);
    private IController controller = null;
    private final ConcurrentMap<String, IInventoryShimInternalListener> inventoryShimInternalListeners = new ConcurrentHashMap<String, IInventoryShimInternalListener>();
    private final Set<IInventoryShimInternalListener> globalInventoryShimInternalListeners = new HashSet<IInventoryShimInternalListener>();
    private final List<IInventoryShimExternalListener> inventoryShimExternalListeners = new CopyOnWriteArrayList<IInventoryShimExternalListener>();
    private final ConcurrentMap<NodeConnector, Set<String>> nodeConnectorContainerMap = new ConcurrentHashMap<NodeConnector, Set<String>>();
    private final ConcurrentMap<Node, Set<String>> nodeContainerMap = new ConcurrentHashMap<Node, Set<String>>();
    private final ConcurrentMap<NodeConnector, Set<Property>> nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Set<Property>>();
    private final ConcurrentMap<Node, Set<Property>> nodeProps = new ConcurrentHashMap<Node, Set<Property>>();
    private IPluginOutConnectionService connectionOutService;

    void setController(IController s) {
        this.controller = s;
    }

    void unsetController(IController s) {
        if (this.controller == s) {
            this.controller = null;
        }
    }

    void setInventoryShimGlobalInternalListener(Map<?, ?> props,
            IInventoryShimInternalListener s) {
        if ((this.globalInventoryShimInternalListeners != null)) {
            this.globalInventoryShimInternalListeners.add(s);
        }
    }

    void unsetInventoryShimGlobalInternalListener(Map<?, ?> props,
            IInventoryShimInternalListener s) {
        if ((this.globalInventoryShimInternalListeners != null)) {
            this.globalInventoryShimInternalListeners.remove(s);
        }
    }

    void setInventoryShimInternalListener(Map<?, ?> props,
            IInventoryShimInternalListener s) {
        if (props == null) {
            logger.error("setInventoryShimInternalListener property is null");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("setInventoryShimInternalListener containerName not supplied");
            return;
        }
        if ((this.inventoryShimInternalListeners != null)
                && !this.inventoryShimInternalListeners.containsValue(s)) {
            this.inventoryShimInternalListeners.put(containerName, s);
            logger.trace(
                    "Added inventoryShimInternalListener for container {}",
                    containerName);
        }
    }

    void unsetInventoryShimInternalListener(Map<?, ?> props,
            IInventoryShimInternalListener s) {
        if (props == null) {
            logger.error("unsetInventoryShimInternalListener property is null");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("setInventoryShimInternalListener containerName not supplied");
            return;
        }
        if ((this.inventoryShimInternalListeners != null)
                && this.inventoryShimInternalListeners.get(containerName) != null
                && this.inventoryShimInternalListeners.get(containerName)
                        .equals(s)) {
            this.inventoryShimInternalListeners.remove(containerName);
            logger.trace(
                    "Removed inventoryShimInternalListener for container {}",
                    containerName);
        }
    }

    void setInventoryShimExternalListener(IInventoryShimExternalListener s) {
        logger.trace("Set inventoryShimExternalListener {}", s);
        if ((this.inventoryShimExternalListeners != null)
                && !this.inventoryShimExternalListeners.contains(s)) {
            this.inventoryShimExternalListeners.add(s);
        }
    }

    void unsetInventoryShimExternalListener(IInventoryShimExternalListener s) {
        logger.trace("Unset inventoryShimExternalListener {}", s);
        if ((this.inventoryShimExternalListeners != null)
                && this.inventoryShimExternalListeners.contains(s)) {
            this.inventoryShimExternalListeners.remove(s);
        }
    }

    void setIPluginOutConnectionService(IPluginOutConnectionService s) {
        connectionOutService = s;
    }

    void unsetIPluginOutConnectionService(IPluginOutConnectionService s) {
        if (connectionOutService == s) {
            connectionOutService = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        this.controller.addMessageListener(OFType.PORT_STATUS, this);
        this.controller.addSwitchStateListener(this);
    }

    /**
     * Function called after registering the service in OSGi service registry.
     */
    void started() {
        /* Start with existing switches */
        startService();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        this.controller.removeMessageListener(OFType.PACKET_IN, this);
        this.controller.removeSwitchStateListener(this);

        this.inventoryShimInternalListeners.clear();
        this.nodeConnectorContainerMap.clear();
        this.nodeContainerMap.clear();
        this.globalInventoryShimInternalListeners.clear();
        this.controller = null;
    }

    @Override
    public void receive(ISwitch sw, OFMessage msg) {
        if (msg instanceof OFPortStatus) {
            handlePortStatusMessage(sw, (OFPortStatus) msg);
        }
        return;
    }

    protected void handlePortStatusMessage(ISwitch sw, OFPortStatus m) {
        Node node = NodeCreator.createOFNode(sw.getId());
        NodeConnector nodeConnector = PortConverter.toNodeConnector(
            m.getDesc().getPortNumber(), node);
        // get node connector properties
        Set<Property> props = InventoryServiceHelper.OFPortToProps(m.getDesc());

        UpdateType type = null;
        if (m.getReason() == (byte) OFPortReason.OFPPR_ADD.ordinal()) {
            type = UpdateType.ADDED;
            nodeConnectorProps.put(nodeConnector, props);
        } else if (m.getReason() == (byte) OFPortReason.OFPPR_DELETE.ordinal()) {
            type = UpdateType.REMOVED;
            nodeConnectorProps.remove(nodeConnector);
        } else if (m.getReason() == (byte) OFPortReason.OFPPR_MODIFY.ordinal()) {
            type = UpdateType.CHANGED;
            nodeConnectorProps.put(nodeConnector, props);
        }

        logger.trace("handlePortStatusMessage {} type {}", nodeConnector, type);

        if (type != null) {
            notifyInventoryShimListener(nodeConnector, type, props);
        }
    }

    @Override
    public void switchAdded(ISwitch sw) {
        if (sw == null) {
            return;
        }
        Node node = NodeCreator.createOFNode(sw.getId());
        if ((nodeProps.get(node) != null)  && (connectionOutService.isLocal(node))) {
            logger.debug("Ignore switchAdded {}", sw);
            return;
        }

        // Add all the nodeConnectors of this switch
        Map<NodeConnector, Set<Property>> ncProps = InventoryServiceHelper
                .OFSwitchToProps(sw);
        for (Map.Entry<NodeConnector, Set<Property>> entry : ncProps.entrySet()) {
            Set<Property> props = new HashSet<Property>();
            Set<Property> prop = entry.getValue();
            if (prop != null) {
                props.addAll(prop);
            }
            nodeConnectorProps.put(entry.getKey(), props);
            notifyInventoryShimListener(entry.getKey(), UpdateType.ADDED, entry.getValue());
        }

        // Add this node
        if (connectionOutService.getLocalityStatus(node) != ConnectionLocality.NOT_CONNECTED) {
            addNode(sw);
        } else {
            logger.debug("Skipping node addition due to Connectivity Status : {}", connectionOutService.getLocalityStatus(node).name());
        }
    }

    @Override
    public void switchDeleted(ISwitch sw) {
        if (sw == null) {
            return;
        }

        removeNode(sw);
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // do nothing
    }

    @Override
    public void tagUpdated(String containerName, Node n, short oldTag,
            short newTag, UpdateType t) {
        logger.debug("tagUpdated: {} type {} for container {}", new Object[] {
                n, t, containerName });
    }

    @Override
    public void containerFlowUpdated(String containerName,
            ContainerFlow previousFlow, ContainerFlow currentFlow, UpdateType t) {
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector nc, UpdateType t) {
        logger.debug("nodeConnectorUpdated: {} type {} for container {}", new Object[] { nc, t, containerName });
        Node node = nc.getNode();
        Set<String> ncContainers = this.nodeConnectorContainerMap.get(nc);
        Set<String> nodeContainers = this.nodeContainerMap.get(node);
        if (ncContainers == null) {
            ncContainers = new CopyOnWriteArraySet<String>();
        }
        if (nodeContainers == null) {
            nodeContainers = new CopyOnWriteArraySet<String>();
        }
        boolean notifyNodeUpdate = false;

        switch (t) {
        case ADDED:
            if (ncContainers.add(containerName)) {
                this.nodeConnectorContainerMap.put(nc, ncContainers);
            }
            if (nodeContainers.add(containerName)) {
                this.nodeContainerMap.put(node, nodeContainers);
                notifyNodeUpdate = true;
            }
            break;
        case REMOVED:
            if (ncContainers.remove(containerName)) {
                if (ncContainers.isEmpty()) {
                    // Do cleanup to reduce memory footprint if no
                    // elements to be tracked
                    this.nodeConnectorContainerMap.remove(nc);
                } else {
                    this.nodeConnectorContainerMap.put(nc, ncContainers);
                }
            }
            boolean nodeContainerUpdate = true;
            for (NodeConnector ncContainer : nodeConnectorContainerMap.keySet()) {
                if ((ncContainer.getNode().equals(node)) && (nodeConnectorContainerMap.get(ncContainer).contains(containerName))) {
                    nodeContainerUpdate = false;
                    break;
                }
            }
            if (nodeContainerUpdate) {
                nodeContainers.remove(containerName);
                notifyNodeUpdate = true;
                if (nodeContainers.isEmpty()) {
                    this.nodeContainerMap.remove(node);
                } else {
                    this.nodeContainerMap.put(node, nodeContainers);
                }
            }
            break;
        case CHANGED:
            break;
        }

        Set<Property> nodeProp = nodeProps.get(node);
        if (nodeProp == null) {
            return;
        }
        Set<Property> ncProp = nodeConnectorProps.get(nc);
        // notify InventoryService
        notifyInventoryShimInternalListener(containerName, nc, t, ncProp);

        if (notifyNodeUpdate) {
            notifyInventoryShimInternalListener(containerName, node, t, nodeProp);
        }
    }

    private void notifyInventoryShimExternalListener(Node node, UpdateType type, Set<Property> props) {
        for (IInventoryShimExternalListener s : this.inventoryShimExternalListeners) {
            s.updateNode(node, type, props);
        }
    }

    private void notifyInventoryShimExternalListener(NodeConnector nodeConnector, UpdateType type, Set<Property> props) {
        for (IInventoryShimExternalListener s : this.inventoryShimExternalListeners) {
            s.updateNodeConnector(nodeConnector, type, props);
        }
    }

    private void notifyInventoryShimInternalListener(String container,
            NodeConnector nodeConnector, UpdateType type, Set<Property> props) {
        IInventoryShimInternalListener inventoryShimInternalListener = inventoryShimInternalListeners.get(container);
        if (inventoryShimInternalListener != null) {
            inventoryShimInternalListener.updateNodeConnector(nodeConnector, type, props);
            logger.trace("notifyInventoryShimInternalListener {} type {} for container {}", new Object[] {
                    nodeConnector, type, container });
        }
    }

    /*
     * Notify all internal and external listeners
     */
    private void notifyInventoryShimListener(NodeConnector nodeConnector, UpdateType type, Set<Property> props) {

        //establish locality before notifying
        boolean isNodeLocal;
        if (type == UpdateType.REMOVED){
            //if removing get the locality first
            isNodeLocal = connectionOutService.isLocal(nodeConnector.getNode());
            notifyGlobalInventoryShimInternalListener(nodeConnector, type, props);
        } else {
            notifyGlobalInventoryShimInternalListener(nodeConnector, type, props);
            isNodeLocal = connectionOutService.isLocal(nodeConnector.getNode());
        }

        if (isNodeLocal) {
            // notify other containers
            Set<String> containers = (nodeConnectorContainerMap.get(nodeConnector) == null) ? new HashSet<String>()
                    : new HashSet<String>(nodeConnectorContainerMap.get(nodeConnector));
            containers.add(GlobalConstants.DEFAULT.toString());
            for (String container : containers) {
                notifyInventoryShimInternalListener(container, nodeConnector, type, props);
            }

            // Notify plugin listeners (Discovery, DataPacket, OFstats etc.)
            notifyInventoryShimExternalListener(nodeConnector, type, props);

            logger.debug("Connection service accepted the inventory notification for {} {}", nodeConnector, type);
        } else {
            logger.debug("Connection service dropped the inventory notification for {} {}", nodeConnector, type);
        }
    }

    /*
     * Notify all internal and external listeners
     */
    private void notifyInventoryShimListener(Node node, UpdateType type, Set<Property> props) {

        //establish locality before notifying
        boolean isNodeLocal;
        if (type == UpdateType.REMOVED){
            //if removing get the locality first
            isNodeLocal = connectionOutService.isLocal(node);
            notifyGlobalInventoryShimInternalListener(node, type, props);
        } else {
            notifyGlobalInventoryShimInternalListener(node, type, props);
            isNodeLocal = connectionOutService.isLocal(node);
        }

        if (isNodeLocal) {
            // Now notify other containers
            Set<String> containers = (nodeContainerMap.get(node) == null) ? new HashSet<String>()
                    : new HashSet<String>(nodeContainerMap.get(node));
            containers.add(GlobalConstants.DEFAULT.toString());
            for (String container : containers) {
                notifyInventoryShimInternalListener(container, node, type, props);
            }

            // Notify plugin listeners (Discovery, DataPacket, OFstats etc.)
            notifyInventoryShimExternalListener(node, type, props);

            logger.debug("Connection service accepted the inventory notification for {} {}", node, type);
        } else {
            logger.debug("Connection service dropped the inventory notification for {} {}", node, type);
        }
    }

    private void notifyGlobalInventoryShimInternalListener(Node node, UpdateType type, Set<Property> props) {
        for (IInventoryShimInternalListener globalListener : globalInventoryShimInternalListeners) {
            globalListener.updateNode(node, type, props);
            logger.trace("notifyGlobalInventoryShimInternalListener {} type {}", new Object[] { node, type });
        }
    }

    private void notifyGlobalInventoryShimInternalListener(NodeConnector nodeConnector, UpdateType type, Set<Property> props) {
        for (IInventoryShimInternalListener globalListener : globalInventoryShimInternalListeners) {
            globalListener.updateNodeConnector(nodeConnector, type, props);
            logger.trace(
                    "notifyGlobalInventoryShimInternalListener {} type {}",
                    new Object[] { nodeConnector, type });
        }
    }

    private void notifyInventoryShimInternalListener(String container,
            Node node, UpdateType type, Set<Property> props) {
        IInventoryShimInternalListener inventoryShimInternalListener = inventoryShimInternalListeners
                .get(container);
        if (inventoryShimInternalListener != null) {
            inventoryShimInternalListener.updateNode(node, type, props);
            logger.trace(
                    "notifyInventoryShimInternalListener {} type {} for container {}",
                    new Object[] { node, type, container });
        }
    }

    private void addNode(ISwitch sw) {
        Node node = NodeCreator.createOFNode(sw.getId());
        UpdateType type = UpdateType.ADDED;

        Set<Property> props = new HashSet<Property>();
        Long sid = (Long) node.getID();

        Date connectedSince = sw.getConnectedDate();
        Long connectedSinceTime = (connectedSince == null) ? 0 : connectedSince
                .getTime();
        props.add(new TimeStamp(connectedSinceTime, "connectedSince"));
        props.add(new MacAddress(deriveMacAddress(sid)));

        byte tables = sw.getTables();
        Tables t = new Tables(tables);
        if (t != null) {
            props.add(t);
        }
        int cap = sw.getCapabilities();
        Capabilities c = new Capabilities(cap);
        if (c != null) {
            props.add(c);
        }
        int act = sw.getActions();
        SupportedFlowActions a = new SupportedFlowActions(FlowConverter.getFlowActions(act));
        if (a != null) {
            props.add(a);
        }
        int buffers = sw.getBuffers();
        Buffers b = new Buffers(buffers);
        if (b != null) {
            props.add(b);
        }

        if ((nodeProps.get(node) == null) &&  (connectionOutService.isLocal(node)))  {
            // The switch is connected for the first time, flush all flows
            // that may exist on this switch
            sw.deleteAllFlows();
       }
        nodeProps.put(node, props);
        // Notify all internal and external listeners
        notifyInventoryShimListener(node, type, props);
    }

    private void removeNode(ISwitch sw) {
        Node node = NodeCreator.createOFNode(sw.getId());
        if(node == null) {
            return;
        }
        removeNodeConnectorProps(node);
        nodeProps.remove(node);
        UpdateType type = UpdateType.REMOVED;
        // Notify all internal and external listeners
        notifyInventoryShimListener(node, type, null);
    }

    private void startService() {
        // Get a snapshot of all the existing switches
        Map<Long, ISwitch> switches = this.controller.getSwitches();
        for (ISwitch sw : switches.values()) {
            switchAdded(sw);
        }
    }

    private void removeNodeConnectorProps(Node node) {
        List<NodeConnector> ncList = new ArrayList<NodeConnector>();
        for (NodeConnector nc : nodeConnectorProps.keySet()) {
            if (nc.getNode().equals(node)) {
                ncList.add(nc);
            }
        }
        for (NodeConnector nc : ncList) {
            nodeConnectorProps.remove(nc);
        }
    }

    @Override
    public void descriptionStatisticsRefreshed(Long switchId, List<OFStatistics> descriptionStats) {
        Node node = NodeCreator.createOFNode(switchId);
        Set<Property> properties = new HashSet<Property>(1);
        OFDescriptionStatistics ofDesc = (OFDescriptionStatistics) descriptionStats.get(0);
        Description desc = new Description(ofDesc.getDatapathDescription());
        properties.add(desc);

        // Notify all internal and external listeners
        notifyInventoryShimListener(node, UpdateType.CHANGED, properties);
    }

    private byte[] deriveMacAddress(long dpid) {
        byte[] mac = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        for (short i = 0; i < 6; i++) {
            mac[5 - i] = (byte) dpid;
            dpid >>= 8;
        }

        return mac;
    }

    @Override
    public void flowStatisticsRefreshed(Long switchId, List<OFStatistics> flows) {
        // Nothing to do
    }

    @Override
    public void portStatisticsRefreshed(Long switchId, List<OFStatistics> ports) {
        // Nothing to do
    }

    @Override
    public void tableStatisticsRefreshed(Long switchId, List<OFStatistics> tables) {
        // Nothing to do
    }

    @Override
    public void containerCreate(String containerName) {
        // Nothing to do
    }

    @Override
    public void containerDestroy(String containerName) {
        Set<NodeConnector> removeNodeConnectorSet = new HashSet<NodeConnector>();
        Set<Node> removeNodeSet = new HashSet<Node>();
        for (Map.Entry<NodeConnector, Set<String>> entry : nodeConnectorContainerMap.entrySet()) {
            Set<String> ncContainers = entry.getValue();
            if (ncContainers.contains(containerName)) {
                NodeConnector nodeConnector = entry.getKey();
                removeNodeConnectorSet.add(nodeConnector);
            }
        }
        for (Map.Entry<Node, Set<String>> entry : nodeContainerMap.entrySet()) {
            Set<String> nodeContainers = entry.getValue();
            if (nodeContainers.contains(containerName)) {
                Node node = entry.getKey();
                removeNodeSet.add(node);
            }
        }
        for (NodeConnector nodeConnector : removeNodeConnectorSet) {
            Set<String> ncContainers = nodeConnectorContainerMap.get(nodeConnector);
            ncContainers.remove(containerName);
            if (ncContainers.isEmpty()) {
                nodeConnectorContainerMap.remove(nodeConnector);
            }
        }
        for (Node node : removeNodeSet) {
            Set<String> nodeContainers = nodeContainerMap.get(node);
            nodeContainers.remove(containerName);
            if (nodeContainers.isEmpty()) {
                nodeContainerMap.remove(node);
            }
        }
    }
}
