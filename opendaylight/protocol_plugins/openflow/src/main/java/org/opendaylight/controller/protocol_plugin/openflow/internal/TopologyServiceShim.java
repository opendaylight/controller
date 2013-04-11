
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.protocol_plugin.openflow.IOFStatisticsManager;
import org.opendaylight.controller.protocol_plugin.openflow.IRefreshInternalProvider;
import org.opendaylight.controller.protocol_plugin.openflow.ITopologyServiceShimListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.discovery.IDiscoveryService;
import org.opendaylight.controller.sal.utils.GlobalConstants;

/**
 * The class describes a shim layer that relays the topology events from OpenFlow
 * core to various listeners. The notifications are filtered based on container
 * configurations.
 */
public class TopologyServiceShim implements IDiscoveryService,
        IContainerListener, CommandProvider, IRefreshInternalProvider {
    protected static final Logger logger = LoggerFactory
            .getLogger(TopologyServiceShim.class);
    private ConcurrentMap<String, ITopologyServiceShimListener> topologyServiceShimListeners = new ConcurrentHashMap<String, ITopologyServiceShimListener>();
    private ConcurrentMap<NodeConnector, List<String>> containerMap = new ConcurrentHashMap<NodeConnector, List<String>>();
    private ConcurrentMap<String, Map<NodeConnector, Pair<Edge, Set<Property>>>> edgeMap = new ConcurrentHashMap<String, Map<NodeConnector, Pair<Edge, Set<Property>>>>();

    private BlockingQueue<NotifyEntry> notifyQ;
    private Thread notifyThread;
    private BlockingQueue<String> bulkNotifyQ;
    private Thread ofPluginTopoBulkUpdate;
    private volatile Boolean shuttingDown = false;
    private IOFStatisticsManager statsMgr;
    private Timer pollTimer;
    private TimerTask txRatePoller;
    private Thread bwUtilNotifyThread;
    private BlockingQueue<UtilizationUpdate> bwUtilNotifyQ;
    private List<NodeConnector> connectorsOverUtilized;
    private float bwThresholdFactor = (float) 0.8; // Threshold = 80% of link bandwidth

    class NotifyEntry {
        String container;
        Pair<Edge, Set<Property>> edgeProps;
        UpdateType type;

        NotifyEntry(String container, Pair<Edge, Set<Property>> edgeProps,
                UpdateType type) {
            this.container = container;
            this.edgeProps = edgeProps;
            this.type = type;
        }
    }

    class TopologyNotify implements Runnable {
        private final BlockingQueue<NotifyEntry> notifyQ;

        TopologyNotify(BlockingQueue<NotifyEntry> notifyQ) {
            this.notifyQ = notifyQ;
        }

        public void run() {
            while (true) {
                try {
                    NotifyEntry entry = notifyQ.take();

                    ITopologyServiceShimListener topologServiceShimListener = topologyServiceShimListeners
                            .get(entry.container);
                    topologServiceShimListener.edgeUpdate(entry.edgeProps
                            .getLeft(), entry.type, entry.edgeProps.getRight());

                    entry = null;
                } catch (InterruptedException e1) {
                    logger.warn("TopologyNotify interrupted", e1.getMessage());
                    if (shuttingDown) {
                        return;
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    class UtilizationUpdate {
        NodeConnector connector;
        UpdateType type;

        UtilizationUpdate(NodeConnector connector, UpdateType type) {
            this.connector = connector;
            this.type = type;
        }
    }

    class BwUtilizationNotify implements Runnable {
        private final BlockingQueue<UtilizationUpdate> notifyQ;

        BwUtilizationNotify(BlockingQueue<UtilizationUpdate> notifyQ) {
            this.notifyQ = notifyQ;
        }

        public void run() {
            while (true) {
                try {
                    UtilizationUpdate update = notifyQ.take();
                    NodeConnector connector = update.connector;
                    Set<String> containerList = edgeMap.keySet();//.get(connector);
                    for (String container : containerList) {
                        Map<NodeConnector, Pair<Edge, Set<Property>>> edgePropsMap = edgeMap
                                .get(container);
                        Edge edge = edgePropsMap.get(connector).getLeft();
                        if (edge.getTailNodeConnector().equals(connector)) {
                            ITopologyServiceShimListener topologServiceShimListener = topologyServiceShimListeners
                                    .get(container);
                            if (update.type == UpdateType.ADDED) {
                                topologServiceShimListener
                                        .edgeOverUtilized(edge);
                            } else {
                                topologServiceShimListener
                                        .edgeUtilBackToNormal(edge);
                            }
                        }
                    }
                } catch (InterruptedException e1) {
                    logger
                            .warn(
                                    "Edge Bandwidth Utilization Notify Thread interrupted",
                                    e1.getMessage());
                    if (shuttingDown) {
                        return;
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.trace("Init called");
        connectorsOverUtilized = new ArrayList<NodeConnector>();
        notifyQ = new LinkedBlockingQueue<NotifyEntry>();
        notifyThread = new Thread(new TopologyNotify(notifyQ));
        bwUtilNotifyQ = new LinkedBlockingQueue<UtilizationUpdate>();
        bwUtilNotifyThread = new Thread(new BwUtilizationNotify(bwUtilNotifyQ));
        bulkNotifyQ = new LinkedBlockingQueue<String>();
        ofPluginTopoBulkUpdate = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String containerName = bulkNotifyQ.take();
                        logger.debug("Bulk Notify container:{}", containerName);
                        TopologyBulkUpdate(containerName);
                    } catch (InterruptedException e) {
                        logger.warn("Topology Bulk update thread interrupted");
                        if (shuttingDown) {
                            return;
                        }
                    }
                }
            }
        }, "Topology Bulk Update");

        // Initialize node connector tx bit rate poller timer
        pollTimer = new Timer();
        txRatePoller = new TimerTask() {
            @Override
            public void run() {
                pollTxBitRates();
            }
        };

        registerWithOSGIConsole();
    }

    /**
     * Continuously polls the transmit bit rate for all the node connectors
     * from statistics manager and trigger the warning notification upward
     * when the transmit rate is above a threshold which is a percentage of
     * the edge bandwidth
     */
    protected void pollTxBitRates() {
        Map<NodeConnector, Pair<Edge, Set<Property>>> globalContainerEdges = edgeMap
                .get(GlobalConstants.DEFAULT.toString());
        if (globalContainerEdges == null) {
            return;
        }

        for (NodeConnector connector : globalContainerEdges.keySet()) {
            // Skip if node connector belongs to production switch
            if (connector.getType().equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                continue;
            }

            // Get edge for which this node connector is head
            Pair<Edge, Set<Property>> props = this.edgeMap.get(
                    GlobalConstants.DEFAULT.toString()).get(connector);
            // On switch mgr restart the props get reset
            if (props == null) {
                continue;
            }
            Set<Property> propSet = props.getRight();
            if (propSet == null) {
                continue;
            }

            float bw = 0;
            for (Property prop : propSet) {
                if (prop instanceof Bandwidth) {
                    bw = ((Bandwidth) prop).getValue();
                    break;
                }
            }

            // Skip if agent did not provide a bandwidth info for the edge
            if (bw == 0) {
                continue;
            }

            // Compare bandwidth usage
            Long switchId = (Long) connector.getNode().getID();
            Short port = (Short) connector.getID();
            float rate = statsMgr.getTransmitRate(switchId, port);
            if (rate > bwThresholdFactor * bw) {
                if (!connectorsOverUtilized.contains(connector)) {
                    connectorsOverUtilized.add(connector);
                    this.bwUtilNotifyQ.add(new UtilizationUpdate(connector,
                            UpdateType.ADDED));
                }
            } else {
                if (connectorsOverUtilized.contains(connector)) {
                    connectorsOverUtilized.remove(connector);
                    this.bwUtilNotifyQ.add(new UtilizationUpdate(connector,
                            UpdateType.REMOVED));
                }
            }
        }

    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        logger.trace("DESTROY called!");
        notifyQ = null;
        notifyThread = null;
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        logger.trace("START called!");
        notifyThread.start();
        bwUtilNotifyThread.start();
        ofPluginTopoBulkUpdate.start();
        pollTimer.scheduleAtFixedRate(txRatePoller, 10000, 5000);
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.trace("STOP called!");
        shuttingDown = true;
        notifyThread.interrupt();
    }

    void setTopologyServiceShimListener(Map<?, ?> props,
            ITopologyServiceShimListener s) {
        if (props == null) {
            logger.error("Didn't receive the service properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("containerName not supplied");
            return;
        }
        if ((this.topologyServiceShimListeners != null)
                && !this.topologyServiceShimListeners
                	.containsKey(containerName)) {
            this.topologyServiceShimListeners.put(containerName, s);
            logger.trace("Added topologyServiceShimListener for container:"
                    + containerName);
        }
    }

    void unsetTopologyServiceShimListener(Map<?, ?> props,
            ITopologyServiceShimListener s) {
        if (props == null) {
            logger.error("Didn't receive the service properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("containerName not supplied");
            return;
        }
        if ((this.topologyServiceShimListeners != null)
                && this.topologyServiceShimListeners
                .containsKey(containerName)
                && this.topologyServiceShimListeners
                .get(containerName).equals(s)
                ) {
            this.topologyServiceShimListeners.remove(containerName);
            logger.trace("Removed topologyServiceShimListener for container: "
                    + containerName);
        }
    }

    void setStatisticsManager(IOFStatisticsManager s) {
        this.statsMgr = s;
    }

    void unsetStatisticsManager(IOFStatisticsManager s) {
        if (this.statsMgr == s) {
            this.statsMgr = null;
        }
    }

    private void removeNodeConnector(String container,
            NodeConnector nodeConnector) {
        Map<NodeConnector, Pair<Edge, Set<Property>>> edgePropsMap = edgeMap
                .get(container);
        if (edgePropsMap == null) {
            return;
        }

        // Remove edge in one direction
        Pair<Edge, Set<Property>> edgeProps = edgePropsMap.get(nodeConnector);
        if (edgeProps == null) {
            return;
        }
        notifyEdge(container, edgeProps.getLeft(), UpdateType.REMOVED, null);

        // Remove edge in another direction
        edgeProps = edgePropsMap
                .get(edgeProps.getLeft().getHeadNodeConnector());
        if (edgeProps == null) {
            return;
        }
        notifyEdge(container, edgeProps.getLeft(), UpdateType.REMOVED, null);
    }

    private void notifyEdge(String container, Edge edge, UpdateType type,
            Set<Property> props) {
        Map<NodeConnector, Pair<Edge, Set<Property>>> edgePropsMap = edgeMap
                .get(container);
        NodeConnector src = edge.getTailNodeConnector();
        Pair<Edge, Set<Property>> edgeProps = new ImmutablePair<Edge, Set<Property>>(
                edge, props);

        switch (type) {
        case ADDED:
        case CHANGED:
            if (edgePropsMap == null) {
                edgePropsMap = new HashMap<NodeConnector, Pair<Edge, Set<Property>>>();
            } else {
                if (edgePropsMap.containsKey(src)
                        && edgePropsMap.get(src).equals(edgeProps)) {
                    // Entry already exists. Return here.
                    return;
                }
            }
            edgePropsMap.put(src, edgeProps);
            edgeMap.put(container, edgePropsMap);
            break;
        case REMOVED:
            if (edgePropsMap != null) {
                edgePropsMap.remove(src);
                if (edgePropsMap.isEmpty()) {
                    edgeMap.remove(container);
                } else {
                    edgeMap.put(container, edgePropsMap);
                }
            }
            break;
        default:
            logger.debug("Invalid " + type + " Edge " + edge
                    + " in container {}", container);
            return;
        }

        notifyQ.add(new NotifyEntry(container, edgeProps, type));

        logger.trace(type + " Edge " + edge + " in container {}", container);
    }

    @Override
    public void notifyEdge(Edge edge, UpdateType type, Set<Property> props) {
        if ((edge == null) || (type == null)) {
            return;
        }

        // Notify default container
        notifyEdge(GlobalConstants.DEFAULT.toString(), edge, type, props);

        // Notify the corresponding containers
        List<String> containers = getEdgeContainers(edge);
        if (containers != null) {
            for (String container : containers) {
                notifyEdge(container, edge, type, props);
            }
        }
    }

    /*
     * Return a list of containers the edge associated with
     */
    private List<String> getEdgeContainers(Edge edge) {
        NodeConnector src = edge.getTailNodeConnector(), dst = edge
                .getHeadNodeConnector();

        if (!src.getType().equals(
                NodeConnector.NodeConnectorIDType.PRODUCTION)) {
            /* Find the common containers for both ends */
            List<String> srcContainers = this.containerMap.get(src), dstContainers = this.containerMap
                    .get(dst), cmnContainers = null;
            if ((srcContainers != null) && (dstContainers != null)) {
                cmnContainers = new ArrayList<String>(srcContainers);
                cmnContainers.retainAll(dstContainers);
            }
            return cmnContainers;
        } else {
            /*
             * If the neighbor is part of a monitored production network, get
             * the containers that the edge port belongs to
             */
            return this.containerMap.get(dst);
        }
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
                removeNodeConnector(containerName, p);
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
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // do nothing
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---Topology Service Shim---\n");
        help
                .append("\t pem [container]               - Print edgeMap entries for a given container\n");
        return help.toString();
    }

    public void _pem(CommandInterpreter ci) {
        String container = ci.nextArgument();
        if (container == null) {
            container = GlobalConstants.DEFAULT.toString();
        }

        ci.println("Container: " + container);
        ci.println("                             Edge                                          Bandwidth");

        Map<NodeConnector, Pair<Edge, Set<Property>>> edgePropsMap = edgeMap
                .get(container);
        if (edgePropsMap == null) {
            return;
        }
        int count = 0;
        for (Pair<Edge, Set<Property>> edgeProps : edgePropsMap.values()) {
            if (edgeProps == null) {
                continue;
            }

            long bw = 0;
            for (Property prop : edgeProps.getRight()) {
                if (prop.getName().equals(Bandwidth.BandwidthPropName)) {
                    bw = ((Bandwidth) prop).getValue();
                }
            }
            count++;
            ci.println(edgeProps.getLeft() + "          " + bw);
        }
        ci.println("Total number of Edges: " + count);
    }

    public void _bwfactor(CommandInterpreter ci) {
        String factorString = ci.nextArgument();
        if (factorString == null) {
            ci.println("Bw threshold: " + this.bwThresholdFactor);
            ci.println("Insert a non null bw threshold");
            return;
        }
        bwThresholdFactor = Float.parseFloat(factorString);
        ci.println("New Bw threshold: " + this.bwThresholdFactor);
    }

    /**
     * This method will trigger topology updates to be sent
     * toward SAL.  SAL then pushes the updates to ALL the applications
     * that have registered as listeners for this service.  SAL has no
     * way of knowing which application requested for the refresh.
     *
     * As an example of this case, is stopping and starting the
     * Topology Manager.  When the topology Manager is stopped,
     * and restarted, it will no longer have the latest topology.
     * Hence, a request is sent here.
     *
     * @param containerName
     * @return void
     */
    @Override
    public void requestRefresh(String containerName) {
        // wake up a bulk update thread and exit
        // the thread will execute the bulkUpdate()
        bulkNotifyQ.add(containerName);
    }

    /**
     * Reading the current topology database, the method will replay
     * all the edge updates for the ITopologyServiceShimListener instance
     * in the given container, which will in turn publish them toward SAL.
     * @param containerName
     */
    private void TopologyBulkUpdate(String containerName) {
        Map<NodeConnector, Pair<Edge, Set<Property>>> edgePropMap = null;

        logger.debug("Try bulk update for container:{}", containerName);
        edgePropMap = edgeMap.get(containerName);
        if (edgePropMap == null) {
            logger.debug("No edges known for container:{}", containerName);
            return;
        }
        ITopologyServiceShimListener topologServiceShimListener = topologyServiceShimListeners
                .get(containerName);
        if (topologServiceShimListener == null) {
            logger.debug("No topology service shim listener for container:{}",
                    containerName);
            return;
        }
        int i = 0;
        for (Pair<Edge, Set<Property>> edgeProps : edgePropMap.values()) {
            if (edgeProps != null) {
                i++;
                logger.trace("Add edge {}", edgeProps.getLeft());
                topologServiceShimListener.edgeUpdate(edgeProps.getLeft(),
                        UpdateType.ADDED, edgeProps.getRight());
            }
        }
        logger.debug("Sent {} updates", i);
    }

}
