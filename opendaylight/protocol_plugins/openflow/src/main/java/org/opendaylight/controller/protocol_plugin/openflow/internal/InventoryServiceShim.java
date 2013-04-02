
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimExternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimInternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitchStateListener;
import org.opendaylight.controller.sal.core.Actions;
import org.opendaylight.controller.sal.core.Buffers;
import org.opendaylight.controller.sal.core.Capabilities;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Tables;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.OFType;
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
        IMessageListener, ISwitchStateListener {
    protected static final Logger logger = LoggerFactory
            .getLogger(InventoryServiceShim.class);
    private IController controller = null;
    private ConcurrentMap<String, IInventoryShimInternalListener> inventoryShimInternalListeners = new ConcurrentHashMap<String, IInventoryShimInternalListener>();
    private List<IInventoryShimExternalListener> inventoryShimExternalListeners = new CopyOnWriteArrayList<IInventoryShimExternalListener>();
    private ConcurrentMap<NodeConnector, List<String>> containerMap = new ConcurrentHashMap<NodeConnector, List<String>>();

    void setController(IController s) {
        this.controller = s;
    }

    void unsetController(IController s) {
        if (this.controller == s) {
            this.controller = null;
        }
    }

    void setInventoryShimInternalListener(Map<?, ?> props,
            IInventoryShimInternalListener s) {
        if (props == null) {
            logger.error("Didn't receive the service properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("containerName not supplied");
            return;
        }
        if ((this.inventoryShimInternalListeners != null)
                && !this.inventoryShimInternalListeners.containsValue(s)) {
            this.inventoryShimInternalListeners.put(containerName, s);
            logger.trace("Added inventoryShimInternalListener for container:"
                    + containerName);
        }
    }

    void unsetInventoryShimInternalListener(Map<?, ?> props,
            IInventoryShimInternalListener s) {
        if (props == null) {
            logger.error("Didn't receive the service properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("containerName not supplied");
            return;
        }
        if ((this.inventoryShimInternalListeners != null)
                && this.inventoryShimInternalListeners
                	.get(containerName) != null
                && this.inventoryShimInternalListeners
                	.get(containerName).equals(s)) {
            this.inventoryShimInternalListeners.remove(containerName);
            logger
                    .trace("Removed inventoryShimInternalListener for container: "
                            + containerName);
        }
    }

    void setInventoryShimExternalListener(IInventoryShimExternalListener s) {
        logger.trace("Set inventoryShimExternalListener");
        if ((this.inventoryShimExternalListeners != null)
                && !this.inventoryShimExternalListeners.contains(s)) {
            this.inventoryShimExternalListeners.add(s);
        }
    }

    void unsetInventoryShimExternalListener(IInventoryShimExternalListener s) {
        if ((this.inventoryShimExternalListeners != null)
                && this.inventoryShimExternalListeners.contains(s)) {
            this.inventoryShimExternalListeners.remove(s);
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
     * Function called after registering the
     * service in OSGi service registry.
     */
    void started() {
        /* Start with existing switches */
        startService();
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        this.controller.removeMessageListener(OFType.PACKET_IN, this);
        this.controller.removeSwitchStateListener(this);

        this.inventoryShimInternalListeners.clear();
        this.containerMap.clear();
        this.controller = null;
    }

    @Override
    public void receive(ISwitch sw, OFMessage msg) {
        try {
            if (msg instanceof OFPortStatus) {
                handlePortStatusMessage(sw, (OFPortStatus) msg);
            }
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        return;
    }

    protected void handlePortStatusMessage(ISwitch sw, OFPortStatus m)
            throws ConstructionException {
        Node node = new Node(NodeIDType.OPENFLOW, sw.getId());
        NodeConnector nodeConnector = PortConverter.toNodeConnector(m.getDesc()
                .getPortNumber(), node);
        UpdateType type = null;

        if (m.getReason() == (byte) OFPortReason.OFPPR_ADD.ordinal()) {
            type = UpdateType.ADDED;
        } else if (m.getReason() == (byte) OFPortReason.OFPPR_DELETE.ordinal()) {
            type = UpdateType.REMOVED;
        } else if (m.getReason() == (byte) OFPortReason.OFPPR_MODIFY.ordinal()) {
            type = UpdateType.CHANGED;
        }

        if (type != null) {
            // get node connector properties
            Set<Property> props = InventoryServiceHelper.OFPortToProps(m
                    .getDesc());
            notifyInventoryShimListener(nodeConnector, type, props);
        }
    }

    @Override
    public void switchAdded(ISwitch sw) {
        if (sw == null)
            return;

        // Add all the nodeConnectors of this switch
        Map<NodeConnector, Set<Property>> ncProps = InventoryServiceHelper
                .OFSwitchToProps(sw);
        for (Map.Entry<NodeConnector, Set<Property>> entry : ncProps.entrySet()) {
            notifyInventoryShimListener(entry.getKey(), UpdateType.ADDED, entry
                    .getValue());
        }

        // Add this node
        addNode(sw);
    }

    @Override
    public void switchDeleted(ISwitch sw) {
        if (sw == null)
            return;

        removeNode(sw);
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // do nothing
    }

    @Override
    public void tagUpdated(String containerName, Node n, short oldTag,
            short newTag, UpdateType t) {
    }

    @Override
    public void containerFlowUpdated(String containerName,
            ContainerFlow previousFlow, ContainerFlow currentFlow, UpdateType t) {
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector p,
            UpdateType t) {
        if (this.containerMap == null) {
            logger.error("containerMap is NULL");
            return;
        }
        List<String> containers = this.containerMap.get(p);
        if (containers == null) {
            containers = new CopyOnWriteArrayList<String>();
        }
        boolean updateMap = false;
        switch (t) {
        case ADDED:
            if (!containers.contains(containerName)) {
                containers.add(containerName);
                updateMap = true;
            }
            break;
        case REMOVED:
            if (containers.contains(containerName)) {
                containers.remove(containerName);
                updateMap = true;
            }
            break;
        case CHANGED:
            break;
        }
        if (updateMap) {
            if (containers.isEmpty()) {
                // Do cleanup to reduce memory footprint if no
                // elements to be tracked
                this.containerMap.remove(p);
            } else {
                this.containerMap.put(p, containers);
            }
        }

        // notify InventoryService
        notifyInventoryShimInternalListener(containerName, p, t, null);
    }

    private void notifyInventoryShimExternalListener(Node node,
            UpdateType type, Set<Property> props) {
        for (IInventoryShimExternalListener s : this.inventoryShimExternalListeners) {
            s.updateNode(node, type, props);
        }
    }

    private void notifyInventoryShimExternalListener(
            NodeConnector nodeConnector, UpdateType type, Set<Property> props) {
        for (IInventoryShimExternalListener s : this.inventoryShimExternalListeners) {
            s.updateNodeConnector(nodeConnector, type, props);
        }
    }

    private void notifyInventoryShimInternalListener(String container,
            NodeConnector nodeConnector, UpdateType type, Set<Property> props) {
        IInventoryShimInternalListener inventoryShimInternalListener = inventoryShimInternalListeners
                .get(container);
        if (inventoryShimInternalListener != null) {
            inventoryShimInternalListener.updateNodeConnector(nodeConnector,
                    type, props);
            logger.trace(type + " " + nodeConnector + " on container "
                    + container);
        }
    }

    /*
     *  Notify all internal and external listeners
     */
    private void notifyInventoryShimListener(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        // Always notify default InventoryService. Store properties in default one.
        notifyInventoryShimInternalListener(GlobalConstants.DEFAULT.toString(),
                nodeConnector, type, props);

        // Now notify other containers
        List<String> containers = containerMap.get(nodeConnector);
        if (containers != null) {
            for (String container : containers) {
                // no property stored in container components.
                notifyInventoryShimInternalListener(container, nodeConnector,
                        type, null);
            }
        }

        // Notify DiscoveryService
        notifyInventoryShimExternalListener(nodeConnector, type, props);
    }

    /*
     *  Notify all internal and external listeners
     */
    private void notifyInventoryShimListener(Node node, UpdateType type,
            Set<Property> props) {
        switch (type) {
        case ADDED:
            // Notify only the default Inventory Service
            IInventoryShimInternalListener inventoryShimDefaultListener = inventoryShimInternalListeners
                    .get(GlobalConstants.DEFAULT.toString());
            if (inventoryShimDefaultListener != null) {
                inventoryShimDefaultListener.updateNode(node, type, props);
            }
            break;
        case REMOVED:
            // Notify all Inventory Service containers
            for (IInventoryShimInternalListener inventoryShimInternalListener : inventoryShimInternalListeners
                    .values()) {
                inventoryShimInternalListener.updateNode(node, type, null);
            }
            break;
        default:
            break;
        }

        // Notify external listener
        notifyInventoryShimExternalListener(node, type, props);
    }

    private void addNode(ISwitch sw) {
        Node node;
        try {
            node = new Node(NodeIDType.OPENFLOW, sw.getId());
        } catch (ConstructionException e) {
            logger.error("{}", e.getMessage());
            return;
        }

        UpdateType type = UpdateType.ADDED;

        Set<Property> props = new HashSet<Property>();
        Long sid = (Long) node.getID();

        Date connectedSince = controller.getSwitches().get(sid)
                .getConnectedDate();
        Long connectedSinceTime = (connectedSince == null) ? 0 : connectedSince
                .getTime();
        props.add(new TimeStamp(connectedSinceTime, "connectedSince"));

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
        Actions a = new Actions(act);
        if (a != null) {
        	props.add(a);
        }
        int buffers = sw.getBuffers();
        Buffers b = new Buffers(buffers);
        if (b != null) {
        	props.add(b);
        }
        // Notify all internal and external listeners
        notifyInventoryShimListener(node, type, props);
    }

    private void removeNode(ISwitch sw) {
        Node node;
        try {
            node = new Node(NodeIDType.OPENFLOW, sw.getId());
        } catch (ConstructionException e) {
            logger.error("{}", e.getMessage());
            return;
        }

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
}
