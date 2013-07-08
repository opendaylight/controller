
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.statisticsmanager.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.IReadServiceListener;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class caches latest network nodes statistics as notified by reader
 * services and provides API to retrieve them.
 */
public class StatisticsManager implements IStatisticsManager, IReadServiceListener, IListenInventoryUpdates {
    private static final Logger log = LoggerFactory.getLogger(StatisticsManager.class);
    private IContainer container;
    private IClusterContainerServices clusterContainerService;
    private IReadService reader;
    //statistics caches
    private ConcurrentMap<Node, List<FlowOnNode>> flowStatistics;
    private ConcurrentMap<Node, List<NodeConnectorStatistics>> nodeConnectorStatistics;
    private ConcurrentMap<Node, List<NodeTableStatistics>> tableStatistics;
    private ConcurrentMap<Node, NodeDescription> descriptionStatistics;

    private void nonClusterObjectCreate() {
        flowStatistics = new ConcurrentHashMap<Node, List<FlowOnNode>>();
        nodeConnectorStatistics = new ConcurrentHashMap<Node, List<NodeConnectorStatistics>>();
        tableStatistics = new ConcurrentHashMap<Node, List<NodeTableStatistics>>();
        descriptionStatistics = new ConcurrentHashMap<Node, NodeDescription>();
    }

    @SuppressWarnings("deprecation")
    private void allocateCaches() {
        if (clusterContainerService == null) {
            nonClusterObjectCreate();
            log.error("Clustering service unavailable. Allocated non-cluster statistics manager cache.");
            return;
        }

        try {
            clusterContainerService.createCache("statisticsmanager.flowStatistics",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache("statisticsmanager.nodeConnectorStatistics",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache("statisticsmanager.tableStatistics",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache("statisticsmanager.descriptionStatistics",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));

        } catch (CacheConfigException cce) {
            log.error("Statistics cache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            log.debug("Skipping statistics cache creation - already present");
        }
    }
    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCaches() {
        ConcurrentMap<?, ?> map;

        if (this.clusterContainerService == null) {
            log.warn("Can't retrieve statistics manager cache, Clustering service unavailable.");
            return;
        }

        log.debug("Statistics Manager - retrieveCaches for Container {}", container);

        map = clusterContainerService.getCache("statisticsmanager.flowStatistics");
        if (map != null) {
            this.flowStatistics = (ConcurrentMap<Node, List<FlowOnNode>>) map;
        } else {
            log.error("Cache allocation failed for statisticsmanager.flowStatistics in container {}", container.getName());
        }

        map = clusterContainerService.getCache("statisticsmanager.nodeConnectorStatistics");
        if (map != null) {
            this.nodeConnectorStatistics = (ConcurrentMap<Node, List<NodeConnectorStatistics>>) map;
        } else {
            log.error("Cache allocation failed for statisticsmanager.nodeConnectorStatistics in container {}", container.getName());
        }

        map = clusterContainerService.getCache("statisticsmanager.tableStatistics");
        if (map != null) {
            this.tableStatistics = (ConcurrentMap<Node, List<NodeTableStatistics>>) map;
        } else {
            log.error("Cache allocation failed for statisticsmanager.tableStatistics in container {}", container.getName());
        }

        map = clusterContainerService.getCache("statisticsmanager.descriptionStatistics");
        if (map != null) {
            this.descriptionStatistics = (ConcurrentMap<Node, NodeDescription>) map;
        } else {
            log.error("Cache allocation failed for statisticsmanager.descriptionStatistics in container {}", container.getName());
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("INIT called!");
        allocateCaches();
        retrieveCaches();

    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        log.debug("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        log.debug("START called!");
    }

    /**
     * Function called after registering the service in OSGi service registry.
     */
    void started(){
        //retrieve current statistics so we don't have to wait for next refresh
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(
                ISwitchManager.class, container.getName(), this);
        if (reader != null && switchManager != null) {
            Set<Node> nodeSet = switchManager.getNodes();
            for (Node node : nodeSet) {
                flowStatistics.put(node, reader.readAllFlows(node));
                descriptionStatistics.put(node, reader.readDescription(node));
                tableStatistics.put(node, reader.readNodeTable(node));
                nodeConnectorStatistics.put(node, reader.readNodeConnectors(node));
            }

        } else {
            log.warn("Failed to retrieve current statistics. Statistics will not be immidiately available!");
        }
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        log.debug("STOP called!");
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set for Statistics Mgr");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed for Statistics Mgr!");
            this.clusterContainerService = null;
        }
    }
    void setIContainer(IContainer c){
        container = c;
    }
    public void unsetIContainer(IContainer s) {
        if (this.container == s) {
            this.container = null;
        }
    }

    public void setReaderService(IReadService service) {
        log.debug("Got inventory service set request {}", service);
        this.reader = service;
    }

    public void unsetReaderService(IReadService service) {
        log.debug("Got a service UNset request {}", service);
        this.reader = null;
    }

    @Override
    public List<FlowOnNode> getFlows(Node node) {
        if (node == null) {
            return null;
        }

        List<FlowOnNode> flowList = new ArrayList<FlowOnNode>();
        List<FlowOnNode> cachedList = flowStatistics.get(node);
        if (cachedList != null){
            flowList.addAll(cachedList);
        }
        return flowList;
    }

    @Override
    public Map<Node, List<FlowOnNode>> getFlowStatisticsForFlowList(List<FlowEntry> flowList) {
        Map<Node, List<FlowOnNode>> statMapOutput = new HashMap<Node, List<FlowOnNode>>();

        if (flowList == null || flowList.isEmpty()){
            return statMapOutput;
        }

        Node node;
        //index FlowEntries' flows by node so we don't traverse entire flow list for each flowEntry
        Map<Node, Set<Flow>> index = new HashMap<Node, Set<Flow>>();
        for (FlowEntry flowEntry : flowList) {
            node = flowEntry.getNode();
            Set<Flow> set = (index.containsKey(node) ? index.get(node) : new HashSet<Flow>());
            set.add(flowEntry.getFlow());
            index.put(node, set);
        }

        //iterate over flows per indexed node and add to output
        for (Entry<Node, Set<Flow>> indexEntry : index.entrySet()) {
            node = indexEntry.getKey();
            List<FlowOnNode> flowsPerNode = flowStatistics.get(node);

            if (flowsPerNode != null && !flowsPerNode.isEmpty()){
                List<FlowOnNode> filteredFlows = statMapOutput.containsKey(node) ?
                        statMapOutput.get(node) : new ArrayList<FlowOnNode>();

                for (FlowOnNode flowOnNode : flowsPerNode) {
                    if (indexEntry.getValue().contains(flowOnNode.getFlow())) {
                        filteredFlows.add(flowOnNode);
                    }
                }
                statMapOutput.put(node, filteredFlows);
            }
        }
        return statMapOutput;
    }

    @Override
    public int getFlowsNumber(Node node) {
        List<FlowOnNode> l;
        if (node == null || (l = flowStatistics.get(node)) == null){
            return -1;
        }
        return l.size();
    }

    @Override
    public NodeDescription getNodeDescription(Node node) {
        if (node == null){
            return null;
        }
        NodeDescription nd = descriptionStatistics.get(node);
        return nd != null? nd.clone() : null;
    }

    @Override
    public NodeConnectorStatistics getNodeConnectorStatistics(NodeConnector nodeConnector) {
        if (nodeConnector == null){
            return null;
        }

        List<NodeConnectorStatistics> statList = nodeConnectorStatistics.get(nodeConnector.getNode());
        if (statList != null){
            for (NodeConnectorStatistics stat : statList) {
                if (stat.getNodeConnector().equals(nodeConnector)){
                    return stat;
                }
            }
        }
        return null;
    }

    @Override
    public List<NodeConnectorStatistics> getNodeConnectorStatistics(Node node) {
        if (node == null){
            return null;
        }

        List<NodeConnectorStatistics> statList = new ArrayList<NodeConnectorStatistics>();
        List<NodeConnectorStatistics> cachedList = nodeConnectorStatistics.get(node);
        if (cachedList != null) {
            statList.addAll(cachedList);
        }
        return statList;
    }

    @Override
    public NodeTableStatistics getNodeTableStatistics(NodeTable nodeTable) {
        if (nodeTable == null){
            return null;
        }
        List<NodeTableStatistics> statList = tableStatistics.get(nodeTable.getNode());
        if (statList != null){
            for (NodeTableStatistics stat : statList) {
                if (stat.getNodeTable().getID().equals(nodeTable.getID())){
                    return stat;
                }
            }
        }
        return null;
    }

    @Override
    public List<NodeTableStatistics> getNodeTableStatistics(Node node){
        if (node == null){
            return null;
        }
        List<NodeTableStatistics> statList = new ArrayList<NodeTableStatistics>();
        List<NodeTableStatistics> cachedList = tableStatistics.get(node);
        if (cachedList != null) {
            statList.addAll(cachedList);
        }
        return statList;
    }

    @Override
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList) {
        this.flowStatistics.put(node, flowStatsList);
    }

    @Override
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList) {
        this.nodeConnectorStatistics.put(node, ncStatsList);
    }

    @Override
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList) {
        this.tableStatistics.put(node, tableStatsList);
    }

    @Override
    public void descriptionStatisticsUpdated(Node node, NodeDescription nodeDescription) {
        this.descriptionStatistics.put(node, nodeDescription);
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        //if node is removed, remove stats mappings
        if (type == UpdateType.REMOVED) {
            flowStatistics.remove(node);
            nodeConnectorStatistics.remove(node);
            tableStatistics.remove(node);
            descriptionStatistics.remove(node);
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector, UpdateType type, Set<Property> props) {
        // not interested in this update
    }
}
