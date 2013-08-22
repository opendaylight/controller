
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Connection Manager provides south-bound connectivity services.
 * The APIs are currently focused towards Active-Active Clustering support
 * wherein the node can connect to any of the Active Controller in the Cluster.
 * This component can also host the necessary logic for south-bound connectivity
 * when partial cluster is identified during Partition scenarios.
 *
 * But this (and its corresponding implementation) component can also be used for
 * basic connectivity mechansims for various south-bound plugins.
 */

package org.opendaylight.controller.connectionmanager.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.ICoordinatorChangeAware;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.connectionmanager.scheme.AbstractScheme;
import org.opendaylight.controller.connectionmanager.scheme.SchemeFactory;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.connection.IConnectionListener;
import org.opendaylight.controller.sal.connection.IConnectionService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IInventoryService;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class ConnectionManager implements IConnectionManager, IConnectionListener,
                                          ICoordinatorChangeAware, IListenInventoryUpdates,
                                          ICacheUpdateAware<Node, Set<InetAddress>>,
                                          CommandProvider {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private ConnectionMgmtScheme activeScheme = ConnectionMgmtScheme.ANY_CONTROLLER_ONE_MASTER;
    private IClusterGlobalServices clusterServices;
    private ConcurrentMap<ConnectionMgmtScheme, AbstractScheme> schemes;
    private IConnectionService connectionService;
    private Thread connectionEventThread;
    private BlockingQueue<ConnectionMgmtEvent> connectionEvents;
    private IInventoryService inventoryService;

    public void setClusterServices(IClusterGlobalServices i) {
        this.clusterServices = i;
    }

    public void unsetClusterServices(IClusterGlobalServices i) {
        if (this.clusterServices == i) {
            this.clusterServices = null;
        }
    }

    public void setConnectionService(IConnectionService i) {
        this.connectionService = i;
    }

    public void unsetConnectionService(IConnectionService i) {
        if (this.connectionService == i) {
            this.connectionService = null;
        }
    }

    public void setInventoryService(IInventoryService service) {
        logger.trace("Got inventory service set request {}", service);
        this.inventoryService = service;
    }

    public void unsetInventoryService(IInventoryService service) {
        logger.trace("Got a service UNset request");
        this.inventoryService = null;
    }

    private void getInventories() {
        Map<Node, Map<String, Property>> nodeProp = this.inventoryService.getNodeProps();
        for (Map.Entry<Node, Map<String, Property>> entry : nodeProp.entrySet()) {
            Node node = entry.getKey();
            logger.debug("getInventories for node:{}", new Object[] { node });
            Map<String, Property> propMap = entry.getValue();
            Set<Property> props = new HashSet<Property>();
            for (Property property : propMap.values()) {
                props.add(property);
            }
            updateNode(node, UpdateType.ADDED, props);
        }

        Map<NodeConnector, Map<String, Property>> nodeConnectorProp = this.inventoryService.getNodeConnectorProps();
        for (Map.Entry<NodeConnector, Map<String, Property>> entry : nodeConnectorProp.entrySet()) {
            Map<String, Property> propMap = entry.getValue();
            Set<Property> props = new HashSet<Property>();
            for (Property property : propMap.values()) {
                props.add(property);
            }
            updateNodeConnector(entry.getKey(), UpdateType.ADDED, props);
        }
    }

    public void started() {
        connectionEventThread = new Thread(new EventHandler(), "ConnectionEvent Thread");
        connectionEventThread.start();

        registerWithOSGIConsole();
        notifyClusterViewChanged();
        // Should pull the Inventory updates in case we missed it
        getInventories();
    }

    public void init() {
        this.connectionEvents = new LinkedBlockingQueue<ConnectionMgmtEvent>();
        schemes = new ConcurrentHashMap<ConnectionMgmtScheme, AbstractScheme>();
        for (ConnectionMgmtScheme scheme : ConnectionMgmtScheme.values()) {
            AbstractScheme schemeImpl = SchemeFactory.getScheme(scheme, clusterServices);
            if (schemeImpl != null) schemes.put(scheme, schemeImpl);
        }
    }

    public void stop() {
        connectionEventThread.interrupt();
        Set<Node> localNodes = getLocalNodes();
        if (localNodes != null) {
            AbstractScheme scheme = schemes.get(activeScheme);
            for (Node localNode : localNodes) {
                connectionService.disconnect(localNode);
                if (scheme != null) scheme.removeNode(localNode);
            }
        }
    }

    @Override
    public ConnectionMgmtScheme getActiveScheme() {
        return activeScheme;
    }

    @Override
    public Set<Node> getNodes(InetAddress controller) {
        AbstractScheme scheme = schemes.get(activeScheme);
        if (scheme == null) return null;
        return scheme.getNodes(controller);
    }

    @Override
    public Set<Node> getLocalNodes() {
        AbstractScheme scheme = schemes.get(activeScheme);
        if (scheme == null) return null;
        return scheme.getNodes();
    }

    @Override
    public boolean isLocal(Node node) {
        AbstractScheme scheme = schemes.get(activeScheme);
        if (scheme == null) return false;
        return scheme.isLocal(node);
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        logger.debug("updateNode: {} type {} props {}", node, type, props);
        AbstractScheme scheme = schemes.get(activeScheme);
        if (scheme == null) return;
        switch (type) {
        case ADDED:
            scheme.addNode(node);
            break;
        case REMOVED:
            scheme.removeNode(node);
            break;
        default:
                break;
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        logger.debug("updateNodeConnector: {} type {} props {}", nodeConnector, type, props);
        AbstractScheme scheme = schemes.get(activeScheme);
        if (scheme == null) return;
        switch (type) {
        case ADDED:
            scheme.addNode(nodeConnector.getNode());
            break;
        default:
                break;
        }
    }

    @Override
    public void coordinatorChanged() {
        notifyClusterViewChanged();
    }

    @Override
    public Node connect(String connectionIdentifier, Map<ConnectionConstants, String> params) {
        if (connectionService == null) return null;
        return connectionService.connect(connectionIdentifier, params);
    }

    @Override
    public Node connect(String type, String connectionIdentifier, Map<ConnectionConstants, String> params) {
        if (connectionService == null) return null;
        return connectionService.connect(type, connectionIdentifier, params);
    }

    @Override
    public Status disconnect (Node node) {
        if (connectionService == null) return new Status(StatusCode.NOSERVICE);
        return connectionService.disconnect(node);
    }

    @Override
    public void entryCreated(Node key, String cacheName, boolean originLocal) {
        if (originLocal) return;
    }

    /*
     * Clustering Services' doesnt provide the existing states in the cache update callbacks.
     * Hence, using a scratch local cache to maintain the existing state.
     *
     */
    private ConcurrentMap<Node, Set<InetAddress>> existingConnections = new ConcurrentHashMap<Node, Set<InetAddress>>();

    @Override
    public void entryUpdated(Node node, Set<InetAddress> newControllers, String cacheName, boolean originLocal) {
        if (originLocal) return;
        Set<InetAddress> existingControllers = existingConnections.get(node);
        if (existingControllers != null) {
            logger.debug("Processing Update for : {} NewControllers : {} existingControllers : {}", node,
                    newControllers.toString(), existingControllers.toString());
            if (newControllers.size() < existingControllers.size()) {
                Set<InetAddress> removed = new HashSet<InetAddress>(existingControllers);
                if (removed.removeAll(newControllers)) {
                    logger.debug("notifyNodeDisconnectFromMaster({})", node);
                    notifyNodeDisconnectedEvent(node);
                }
            }
        } else {
            logger.debug("Ignoring the Update for : {} NewControllers : {}", node, newControllers.toString());
        }
        existingConnections.put(node, newControllers);
    }

    @Override
    public void entryDeleted(Node key, String cacheName, boolean originLocal) {
        if (originLocal) return;
        logger.debug("Deleted : {} cache : {}", key, cacheName);
        notifyNodeDisconnectedEvent(key);
    }

    private void enqueueConnectionEvent(ConnectionMgmtEvent event) {
        try {
            if (!connectionEvents.contains(event)) {
                this.connectionEvents.put(event);
            }
        } catch (InterruptedException e) {
            logger.debug("enqueueConnectionEvent caught Interrupt Exception for event {}", event);
        }
    }

    private void notifyClusterViewChanged() {
        ConnectionMgmtEvent event = new ConnectionMgmtEvent(ConnectionMgmtEventType.CLUSTER_VIEW_CHANGED, null);
        enqueueConnectionEvent(event);
    }

    private void notifyNodeDisconnectedEvent(Node node) {
        ConnectionMgmtEvent event = new ConnectionMgmtEvent(ConnectionMgmtEventType.NODE_DISCONNECTED_FROM_MASTER, node);
        enqueueConnectionEvent(event);
    }

    /*
     * this thread monitors the connectionEvent queue for new incoming events from
     */
    private class EventHandler implements Runnable {
        @Override
        public void run() {

            while (true) {
                try {
                    ConnectionMgmtEvent ev = connectionEvents.take();
                    ConnectionMgmtEventType eType = ev.getEvent();
                    switch (eType) {
                    case NODE_DISCONNECTED_FROM_MASTER:
                        Node node = (Node)ev.getData();
                        connectionService.notifyNodeDisconnectFromMaster(node);
                        break;
                    case CLUSTER_VIEW_CHANGED:
                        AbstractScheme scheme = schemes.get(activeScheme);
                        if (scheme == null) return;
                        scheme.handleClusterViewChanged();
                        connectionService.notifyClusterViewChanged();
                        break;
                    default:
                        logger.error("Unknown Connection event {}", eType.ordinal());
                    }
                } catch (InterruptedException e) {
                    connectionEvents.clear();
                    return;
                }
            }
        }
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    public void _scheme (CommandInterpreter ci) {
        String schemeStr = ci.nextArgument();
        if (schemeStr == null) {
            ci.println("Please enter valid Scheme name");
            ci.println("Current Scheme : " + activeScheme.name());
            return;
        }
        ConnectionMgmtScheme scheme = ConnectionMgmtScheme.valueOf(schemeStr);
        if (scheme == null) {
            ci.println("Please enter a valid Scheme name");
            return;
        }
        activeScheme = scheme;
    }

    public void _printNodes (CommandInterpreter ci) {
        String controller = ci.nextArgument();
        if (controller == null) {
            ci.println("Nodes connected to this controller : ");
            if (this.getLocalNodes() == null) ci.println("None");
            else ci.println(this.getLocalNodes().toString());
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(controller);
            ci.println("Nodes connected to controller "+controller);
            if (this.getNodes(address) == null) ci.println("None");
            else ci.println(this.getNodes(address).toString());
            return;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---Connection Manager---\n");
        help.append("\t scheme [<name>]                      - Print / Set scheme\n");
        help.append("\t printNodes [<controller>]            - Print connected nodes\n");
        return help.toString();
    }
}
