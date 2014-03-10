/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topologymanager.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.ConfigurationObject;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
import org.opendaylight.controller.sal.topology.ITopologyService;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.opendaylight.controller.topologymanager.ITopologyManagerClusterWideAware;
import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class describes TopologyManager which is the central repository of the
 * network topology. It provides service for applications to interact with
 * topology database and notifies all the listeners of topology changes.
 */
public class TopologyManagerImpl implements
        ICacheUpdateAware<Object, Object>,
        ITopologyManager,
        IConfigurationContainerAware,
        IListenTopoUpdates,
        IObjectReader,
        CommandProvider {
    protected static final String TOPOEDGESDB = "topologymanager.edgesDB";
    protected static final String TOPOHOSTSDB = "topologymanager.hostsDB";
    protected static final String TOPONODECONNECTORDB = "topologymanager.nodeConnectorDB";
    protected static final String TOPOUSERLINKSDB = "topologymanager.userLinksDB";
    private static final String USER_LINKS_FILE_NAME = "userTopology.conf";
    private static final Logger log = LoggerFactory.getLogger(TopologyManagerImpl.class);
    private ITopologyService topoService;
    private IClusterContainerServices clusterContainerService;
    private IConfigurationContainerService configurationService;
    private ISwitchManager switchManager;
    // DB of all the Edges with properties which constitute our topology
    private ConcurrentMap<Edge, Set<Property>> edgesDB;
    // DB of all NodeConnector which are part of ISL Edges, meaning they
    // are connected to another NodeConnector on the other side of an ISL link.
    // NodeConnector of a Production Edge is not part of this DB.
    private ConcurrentMap<NodeConnector, Set<Property>> nodeConnectorsDB;
    // DB of all the NodeConnectors with an Host attached to it
    private ConcurrentMap<NodeConnector, Set<ImmutablePair<Host, Set<Property>>>> hostsDB;
    // Topology Manager Aware listeners
    private Set<ITopologyManagerAware> topologyManagerAware = new CopyOnWriteArraySet<ITopologyManagerAware>();
    // Topology Manager Aware listeners - for clusterwide updates
    private Set<ITopologyManagerClusterWideAware> topologyManagerClusterWideAware =
            new CopyOnWriteArraySet<ITopologyManagerClusterWideAware>();
    private ConcurrentMap<String, TopologyUserLinkConfig> userLinksDB;
    private BlockingQueue<TopoEdgeUpdate> notifyQ = new LinkedBlockingQueue<TopoEdgeUpdate>();
    private volatile Boolean shuttingDown = false;
    private Thread notifyThread;


    void nonClusterObjectCreate() {
        edgesDB = new ConcurrentHashMap<Edge, Set<Property>>();
        hostsDB = new ConcurrentHashMap<NodeConnector, Set<ImmutablePair<Host, Set<Property>>>>();
        nodeConnectorsDB = new ConcurrentHashMap<NodeConnector, Set<Property>>();
        userLinksDB = new ConcurrentHashMap<String, TopologyUserLinkConfig>();
    }

    void setTopologyManagerAware(ITopologyManagerAware s) {
        if (this.topologyManagerAware != null) {
            log.debug("Adding ITopologyManagerAware: {}", s);
            this.topologyManagerAware.add(s);
        }
    }

    void unsetTopologyManagerAware(ITopologyManagerAware s) {
        if (this.topologyManagerAware != null) {
            log.debug("Removing ITopologyManagerAware: {}", s);
            this.topologyManagerAware.remove(s);
        }
    }

    void setTopologyManagerClusterWideAware(ITopologyManagerClusterWideAware s) {
        if (this.topologyManagerClusterWideAware != null) {
            log.debug("Adding ITopologyManagerClusterWideAware: {}", s);
            this.topologyManagerClusterWideAware.add(s);
        }
    }

    void unsetTopologyManagerClusterWideAware(ITopologyManagerClusterWideAware s) {
        if (this.topologyManagerClusterWideAware != null) {
            log.debug("Removing ITopologyManagerClusterWideAware: {}", s);
            this.topologyManagerClusterWideAware.remove(s);
        }
    }

    void setTopoService(ITopologyService s) {
        log.debug("Adding ITopologyService: {}", s);
        this.topoService = s;
    }

    void unsetTopoService(ITopologyService s) {
        if (this.topoService == s) {
            log.debug("Removing ITopologyService: {}", s);
            this.topoService = null;
        }
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    public void setConfigurationContainerService(IConfigurationContainerService service) {
        log.trace("Got configuration service set request {}", service);
        this.configurationService = service;
    }

    public void unsetConfigurationContainerService(IConfigurationContainerService service) {
        log.trace("Got configuration service UNset request");
        this.configurationService = null;
    }

    void setSwitchManager(ISwitchManager s) {
        log.debug("Adding ISwitchManager: {}", s);
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            log.debug("Removing ISwitchManager: {}", s);
            this.switchManager = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        allocateCaches();
        retrieveCaches();
        String containerName = null;
        Dictionary<?, ?> props = c.getServiceProperties();
        if (props != null) {
            containerName = (String) props.get("containerName");
        } else {
            // In the Global instance case the containerName is empty
            containerName = "UNKNOWN";
        }

        registerWithOSGIConsole();
        loadConfiguration();

        // Restore the shuttingDown status on init of the component
        shuttingDown = false;
        notifyThread = new Thread(new TopologyNotify(notifyQ));
    }

    @SuppressWarnings({ "unchecked" })
    private void allocateCaches() {
            this.edgesDB =
                    (ConcurrentMap<Edge, Set<Property>>) allocateCache(TOPOEDGESDB,EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            this.hostsDB =
                    (ConcurrentMap<NodeConnector, Set<ImmutablePair<Host, Set<Property>>>>) allocateCache(TOPOHOSTSDB, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            this.nodeConnectorsDB =
                    (ConcurrentMap<NodeConnector, Set<Property>>) allocateCache(
                            TOPONODECONNECTORDB, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            this.userLinksDB =
                    (ConcurrentMap<String, TopologyUserLinkConfig>) allocateCache(
                            TOPOUSERLINKSDB, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
    }

    private ConcurrentMap<?, ?> allocateCache(String cacheName, Set<IClusterServices.cacheMode> cacheModes) {
        ConcurrentMap<?, ?> cache = null;
        try {
            cache = this.clusterContainerService.createCache(cacheName, cacheModes);
        } catch (CacheExistException e) {
            log.debug(cacheName + " cache already exists - destroy and recreate if needed");
        } catch (CacheConfigException e) {
            log.error(cacheName + " cache configuration invalid - check cache mode");
        }
        return cache;
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        if (this.clusterContainerService == null) {
            log.error("Cluster Services is null, can't retrieve caches.");
            return;
        }

        this.edgesDB = (ConcurrentMap<Edge, Set<Property>>) this.clusterContainerService.getCache(TOPOEDGESDB);
        if (edgesDB == null) {
            log.error("Failed to get cache for " + TOPOEDGESDB);
        }

        this.hostsDB =
                (ConcurrentMap<NodeConnector, Set<ImmutablePair<Host, Set<Property>>>>) this.clusterContainerService.getCache(TOPOHOSTSDB);
        if (hostsDB == null) {
            log.error("Failed to get cache for " + TOPOHOSTSDB);
        }

        this.nodeConnectorsDB =
                (ConcurrentMap<NodeConnector, Set<Property>>) this.clusterContainerService.getCache(TOPONODECONNECTORDB);
        if (nodeConnectorsDB == null) {
            log.error("Failed to get cache for " + TOPONODECONNECTORDB);
        }

        this.userLinksDB =
                (ConcurrentMap<String, TopologyUserLinkConfig>) this.clusterContainerService.getCache(TOPOUSERLINKSDB);
        if (userLinksDB == null) {
            log.error("Failed to get cache for " + TOPOUSERLINKSDB);
        }
    }

    /**
     * Function called after the topology manager has registered the service in
     * OSGi service registry.
     *
     */
    void started() {
        // Start the batcher thread for the cluster wide topology updates
        notifyThread.start();
        // SollicitRefresh MUST be called here else if called at init
        // time it may sollicit refresh too soon.
        log.debug("Sollicit topology refresh");
        topoService.sollicitRefresh();
    }

    void stop() {
        shuttingDown = true;
        notifyThread.interrupt();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        notifyQ.clear();
        notifyThread = null;
    }

    private void loadConfiguration() {
        for (ConfigurationObject conf : configurationService.retrieveConfiguration(this, USER_LINKS_FILE_NAME)) {
            addUserLink((TopologyUserLinkConfig) conf);
        }
    }

    @Override
    public Status saveConfig() {
        return saveConfigInternal();
    }

    public Status saveConfigInternal() {
        Status saveStatus = configurationService.persistConfiguration(
                new ArrayList<ConfigurationObject>(userLinksDB.values()), USER_LINKS_FILE_NAME);

        if (!saveStatus.isSuccess()) {
            return new Status(StatusCode.INTERNALERROR, "Topology save failed: " + saveStatus.getDescription());
        }
        return saveStatus;
    }

    @Override
    public Map<Node, Set<Edge>> getNodeEdges() {
        if (this.edgesDB == null) {
            return null;
        }

        Map<Node, Set<Edge>> res = new HashMap<Node, Set<Edge>>();
        for (Edge edge : this.edgesDB.keySet()) {
            // Lets analyze the tail
            Node node = edge.getTailNodeConnector().getNode();
            Set<Edge> nodeEdges = res.get(node);
            if (nodeEdges == null) {
                nodeEdges = new HashSet<Edge>();
                res.put(node, nodeEdges);
            }
            nodeEdges.add(edge);

            // Lets analyze the head
            node = edge.getHeadNodeConnector().getNode();
            nodeEdges = res.get(node);
            if (nodeEdges == null) {
                nodeEdges = new HashSet<Edge>();
                res.put(node, nodeEdges);
            }
            nodeEdges.add(edge);
        }

        return res;
    }

    @Override
    public boolean isInternal(NodeConnector p) {
        if (this.nodeConnectorsDB == null) {
            return false;
        }

        // This is an internal NodeConnector if is connected to
        // another Node i.e it's part of the nodeConnectorsDB
        return (this.nodeConnectorsDB.get(p) != null);
    }

    /**
     * This method returns true if the edge is an ISL link.
     *
     * @param e
     *            The edge
     * @return true if it is an ISL link
     */
    public boolean isISLink(Edge e) {
        return (!isProductionLink(e));
    }

    /**
     * This method returns true if the edge is a production link.
     *
     * @param e
     *            The edge
     * @return true if it is a production link
     */
    public boolean isProductionLink(Edge e) {
        return (e.getHeadNodeConnector().getType().equals(NodeConnector.NodeConnectorIDType.PRODUCTION)
                || e.getTailNodeConnector().getType().equals(NodeConnector.NodeConnectorIDType.PRODUCTION));
    }

    /**
     * This method cross checks the determination of nodeConnector type by Discovery Service
     * against the information in SwitchManager and updates it accordingly.
     * @param e
     *          The edge
     */
    private void crossCheckNodeConnectors(Edge e) {
        NodeConnector nc;
        if (e.getHeadNodeConnector().getType().equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
            nc = updateNCTypeFromSwitchMgr(e.getHeadNodeConnector());
            if (nc != null) {
                e.setHeadNodeConnector(nc);
            }
        }
        if (e.getTailNodeConnector().getType().equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
            nc = updateNCTypeFromSwitchMgr(e.getTailNodeConnector());
            if (nc != null) {
                e.setTailNodeConnector(nc);
            }
        }
    }

    /**
     * A NodeConnector may have been categorized as of type Production by Discovery Service.
     * But at the time when this determination was made, only OF nodes were known to Discovery
     * Service. This method checks if the node of nodeConnector is known to SwitchManager. If
     * so, then it returns a new NodeConnector with correct type.
     *
     * @param nc
     *       NodeConnector as passed on in the edge
     * @return
     *       If Node of the NodeConnector is in SwitchManager, then return a new NodeConnector
     *       with correct type, null otherwise
     */

    private NodeConnector updateNCTypeFromSwitchMgr(NodeConnector nc) {

        for (Node node : switchManager.getNodes()) {
            String nodeName = node.getNodeIDString();
            log.trace("Switch Manager Node Name: {}, NodeConnector Node Name: {}", nodeName,
                    nc.getNode().getNodeIDString());
            if (nodeName.equals(nc.getNode().getNodeIDString())) {
                NodeConnector nodeConnector = NodeConnectorCreator
                        .createNodeConnector(node.getType(), nc.getID(), node);
                return nodeConnector;
            }
        }
        return null;
    }

    /**
     * The Map returned is a copy of the current topology hence if the topology
     * changes the copy doesn't
     *
     * @return A Map representing the current topology expressed as edges of the
     *         network
     */
    @Override
    public Map<Edge, Set<Property>> getEdges() {
        if (this.edgesDB == null) {
            return null;
        }

        Map<Edge, Set<Property>> edgeMap = new HashMap<Edge, Set<Property>>();
        Set<Property> props;
        for (Map.Entry<Edge, Set<Property>> edgeEntry : edgesDB.entrySet()) {
            // Sets of props are copied because the composition of
            // those properties could change with time
            props = new HashSet<Property>(edgeEntry.getValue());
            // We can simply reuse the key because the object is
            // immutable so doesn't really matter that we are
            // referencing the only owned by a different table, the
            // meaning is the same because doesn't change with time.
            edgeMap.put(edgeEntry.getKey(), props);
        }

        return edgeMap;
    }

    @Override
    public Set<NodeConnector> getNodeConnectorWithHost() {
        if (this.hostsDB == null) {
            return null;
        }

        return (new HashSet<NodeConnector>(this.hostsDB.keySet()));
    }

    @Override
    public Map<Node, Set<NodeConnector>> getNodesWithNodeConnectorHost() {
        if (this.hostsDB == null) {
            return null;
        }
        HashMap<Node, Set<NodeConnector>> res = new HashMap<Node, Set<NodeConnector>>();
        Node node;
        Set<NodeConnector> portSet;
        for (NodeConnector nc : this.hostsDB.keySet()) {
            node = nc.getNode();
            portSet = res.get(node);
            if (portSet == null) {
                // Create the HashSet if null
                portSet = new HashSet<NodeConnector>();
                res.put(node, portSet);
            }

            // Keep updating the HashSet, given this is not a
            // clustered map we can just update the set without
            // worrying to update the hashmap.
            portSet.add(nc);
        }

        return (res);
    }

    @Override
    public Host getHostAttachedToNodeConnector(NodeConnector port) {
        List<Host> hosts = getHostsAttachedToNodeConnector(port);
        if(hosts != null && !hosts.isEmpty()){
            return hosts.get(0);
        }
        return null;
    }

    @Override
    public List<Host> getHostsAttachedToNodeConnector(NodeConnector p) {
        Set<ImmutablePair<Host, Set<Property>>> hosts;
        if (this.hostsDB == null || (hosts = this.hostsDB.get(p)) == null) {
            return null;
        }
        // create a list of hosts
        List<Host> retHosts = new LinkedList<Host>();
        for(ImmutablePair<Host, Set<Property>> host : hosts) {
            retHosts.add(host.getLeft());
        }
        return retHosts;
    }

    @Override
    public synchronized void updateHostLink(NodeConnector port, Host h, UpdateType t, Set<Property> props) {

        // Clone the property set in case non null else just
        // create an empty one. Caches allocated via infinispan
        // don't allow null values
        if (props == null) {
            props = new HashSet<Property>();
        } else {
            props = new HashSet<Property>(props);
        }
        ImmutablePair<Host, Set<Property>> thisHost = new ImmutablePair<Host, Set<Property>>(h, props);

        // get the host list
        Set<ImmutablePair<Host, Set<Property>>> hostSet = this.hostsDB.get(port);
        if(hostSet == null) {
            hostSet = new HashSet<ImmutablePair<Host, Set<Property>>>();
        }
        switch (t) {
        case ADDED:
        case CHANGED:
            hostSet.add(thisHost);
            this.hostsDB.put(port, hostSet);
            break;
        case REMOVED:
            hostSet.remove(thisHost);
            if(hostSet.isEmpty()) {
                //remove only if hasn't been concurrently modified
                this.hostsDB.remove(port, hostSet);
            } else {
                this.hostsDB.put(port, hostSet);
            }
            break;
        }
    }

    private boolean headNodeConnectorExist(Edge e) {
        /*
         * Only check the head end point which is supposed to be part of a
         * network node we control (present in our inventory). If we checked the
         * tail end point as well, we would not store the edges that connect to
         * a non sdn enable port on a non sdn capable production switch. We want
         * to be able to see these switches on the topology.
         */
        NodeConnector head = e.getHeadNodeConnector();
        return (switchManager.doesNodeConnectorExist(head));
    }

    private TopoEdgeUpdate edgeUpdate(Edge e, UpdateType type, Set<Property> props) {
        switch (type) {
        case ADDED:


            if (this.edgesDB.containsKey(e)) {
                // Avoid redundant updates (e.g. cluster switch-over) as notifications trigger expensive tasks
                log.trace("Skipping redundant edge addition: {}", e);
                return null;
            }

            // Make sure the props are non-null or create a copy
            if (props == null) {
                props = new HashSet<Property>();
            } else {
                props = new HashSet<Property>(props);
            }


            // Ensure that head node connector exists
            if (!headNodeConnectorExist(e)) {
                log.warn("Ignore edge that contains invalid node connector: {}", e);
                return null;
            }

            // Check if nodeConnectors of the edge were correctly categorized
            // by protocol plugin
            crossCheckNodeConnectors(e);

            // Now make sure there is the creation timestamp for the
            // edge, if not there, stamp with the first update
            boolean found_create = false;
            for (Property prop : props) {
                if (prop instanceof TimeStamp) {
                    TimeStamp t = (TimeStamp) prop;
                    if (t.getTimeStampName().equals("creation")) {
                        found_create = true;
                        break;
                    }
                }
            }

            if (!found_create) {
                TimeStamp t = new TimeStamp(System.currentTimeMillis(), "creation");
                props.add(t);
            }

            // Now add this in the database eventually overriding
            // something that may have been already existing
            this.edgesDB.put(e, props);

            // Now populate the DB of NodeConnectors
            // NOTE WELL: properties are empty sets, not really needed
            // for now.
            // The DB only contains ISL ports
            if (isISLink(e)) {
                this.nodeConnectorsDB.put(e.getHeadNodeConnector(), new HashSet<Property>(1));
                this.nodeConnectorsDB.put(e.getTailNodeConnector(), new HashSet<Property>(1));
            }
            log.trace("Edge {}  {}", e.toString(), type.name());
            break;
        case REMOVED:
            // Now remove the edge from edgesDB
            this.edgesDB.remove(e);

            // Now lets update the NodeConnectors DB, the assumption
            // here is that two NodeConnector are exclusively
            // connected by 1 and only 1 edge, this is reasonable in
            // the same plug (virtual of phisical) we can assume two
            // cables won't be plugged. This could break only in case
            // of devices in the middle that acts as hubs, but it
            // should be safe to assume that won't happen.
            this.nodeConnectorsDB.remove(e.getHeadNodeConnector());
            this.nodeConnectorsDB.remove(e.getTailNodeConnector());
            log.trace("Edge {}  {}", e.toString(), type.name());
            break;
        case CHANGED:
            Set<Property> oldProps = this.edgesDB.get(e);

            // When property(s) changes lets make sure we can change it
            // all except the creation time stamp because that should
            // be set only when the edge is created
            TimeStamp timeStamp = null;
            for (Property prop : oldProps) {
                if (prop instanceof TimeStamp) {
                    TimeStamp tsProp = (TimeStamp) prop;
                    if (tsProp.getTimeStampName().equals("creation")) {
                        timeStamp = tsProp;
                        break;
                    }
                }
            }

            // Now lets make sure new properties are non-null
            if (props == null) {
                props = new HashSet<Property>();
            } else {
                // Copy the set so noone is going to change the content
                props = new HashSet<Property>(props);
            }

            // Now lets remove the creation property if exist in the
            // new props
            for (Iterator<Property> i = props.iterator(); i.hasNext();) {
                Property prop = i.next();
                if (prop instanceof TimeStamp) {
                    TimeStamp t = (TimeStamp) prop;
                    if (t.getTimeStampName().equals("creation")) {
                        i.remove();
                        break;
                    }
                }
            }

            // Now lets add the creation timestamp in it
            if (timeStamp != null) {
                props.add(timeStamp);
            }

            // Finally update
            this.edgesDB.put(e, props);
            log.trace("Edge {}  {}", e.toString(), type.name());
            break;
        }
        return new TopoEdgeUpdate(e, props, type);
    }

    @Override
    public void edgeUpdate(List<TopoEdgeUpdate> topoedgeupdateList) {
        List<TopoEdgeUpdate> teuList = new ArrayList<TopoEdgeUpdate>();
        for (int i = 0; i < topoedgeupdateList.size(); i++) {
            Edge e = topoedgeupdateList.get(i).getEdge();
            Set<Property> p = topoedgeupdateList.get(i).getProperty();
            UpdateType type = topoedgeupdateList.get(i).getUpdateType();
            TopoEdgeUpdate teu = edgeUpdate(e, type, p);
            if (teu != null) {
                teuList.add(teu);
            }
        }

        if (!teuList.isEmpty()) {
            // Now update the listeners
            for (ITopologyManagerAware s : this.topologyManagerAware) {
                try {
                    s.edgeUpdate(teuList);
                } catch (Exception exc) {
                    log.error("Exception on edge update:", exc);
                }
            }
        }
    }

    private Edge getReverseLinkTuple(TopologyUserLinkConfig link) {
        TopologyUserLinkConfig rLink = new TopologyUserLinkConfig(
                link.getName(), link.getDstNodeConnector(), link.getSrcNodeConnector());
        return getLinkTuple(rLink);
    }


    private Edge getLinkTuple(TopologyUserLinkConfig link) {
        NodeConnector srcNodeConnector = NodeConnector.fromString(link.getSrcNodeConnector());
        NodeConnector dstNodeConnector = NodeConnector.fromString(link.getDstNodeConnector());
        try {
            return new Edge(srcNodeConnector, dstNodeConnector);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ConcurrentMap<String, TopologyUserLinkConfig> getUserLinks() {
        return new ConcurrentHashMap<String, TopologyUserLinkConfig>(userLinksDB);
    }

    @Override
    public Status addUserLink(TopologyUserLinkConfig userLink) {
        if (!userLink.isValid()) {
            return new Status(StatusCode.BADREQUEST,
                    "User link configuration invalid.");
        }
        userLink.setStatus(TopologyUserLinkConfig.STATUS.LINKDOWN);

        //Check if this link already configured
        //NOTE: infinispan cache doesn't support Map.containsValue()
        // (which is linear time in most ConcurrentMap impl anyway)
        for (TopologyUserLinkConfig existingLink : userLinksDB.values()) {
            if (existingLink.equals(userLink)) {
                return new Status(StatusCode.CONFLICT, "Link configuration exists");
            }
        }
        //attempt put, if mapping for this key already existed return conflict
        if (userLinksDB.putIfAbsent(userLink.getName(), userLink) != null) {
            return new Status(StatusCode.CONFLICT, "Link with name : " + userLink.getName()
                    + " already exists. Please use another name");
        }

        Edge linkTuple = getLinkTuple(userLink);
        if (linkTuple != null) {
            if (!isProductionLink(linkTuple)) {
                TopoEdgeUpdate teu = edgeUpdate(linkTuple, UpdateType.ADDED,
                                                new HashSet<Property>());
                if (teu == null) {
                    userLinksDB.remove(userLink.getName());
                    return new Status(StatusCode.NOTFOUND,
                           "Link configuration contains invalid node connector: "
                           + userLink);
                }
            }

            linkTuple = getReverseLinkTuple(userLink);
            if (linkTuple != null) {
                userLink.setStatus(TopologyUserLinkConfig.STATUS.SUCCESS);
                if (!isProductionLink(linkTuple)) {
                    edgeUpdate(linkTuple, UpdateType.ADDED, new HashSet<Property>());
                }
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status deleteUserLink(String linkName) {
        if (linkName == null) {
            return new Status(StatusCode.BADREQUEST, "User link name cannot be null.");
        }

        TopologyUserLinkConfig link = userLinksDB.remove(linkName);
        Edge linkTuple;
        if ((link != null) && ((linkTuple = getLinkTuple(link)) != null)) {
            if (! isProductionLink(linkTuple)) {
                edgeUpdate(linkTuple, UpdateType.REMOVED, null);
            }

            linkTuple = getReverseLinkTuple(link);
            if (! isProductionLink(linkTuple)) {
                edgeUpdate(linkTuple, UpdateType.REMOVED, null);
            }
        }
        return new Status(StatusCode.SUCCESS);
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
        help.append("---Topology Manager---\n");
        help.append("\t addUserLink <name> <node connector string> <node connector string>\n");
        help.append("\t deleteUserLink <name>\n");
        help.append("\t printUserLink\n");
        help.append("\t printNodeEdges\n");
        return help.toString();
    }

    public void _printUserLink(CommandInterpreter ci) {
        for (String name : this.userLinksDB.keySet()) {
            TopologyUserLinkConfig linkConfig = userLinksDB.get(name);
            ci.println("Name : " + name);
            ci.println(linkConfig);
            ci.println("Edge " + getLinkTuple(linkConfig));
            ci.println("Reverse Edge " + getReverseLinkTuple(linkConfig));
        }
    }

    public void _addUserLink(CommandInterpreter ci) {
        String name = ci.nextArgument();
        if ((name == null)) {
            ci.println("Please enter a valid Name");
            return;
        }

        String ncStr1 = ci.nextArgument();
        if (ncStr1 == null) {
            ci.println("Please enter two node connector strings");
            return;
        }
        String ncStr2 = ci.nextArgument();
        if (ncStr2 == null) {
            ci.println("Please enter second node connector string");
            return;
        }

        NodeConnector nc1 = NodeConnector.fromString(ncStr1);
        if (nc1 == null) {
            ci.println("Invalid input node connector 1 string: " + ncStr1);
            return;
        }
        NodeConnector nc2 = NodeConnector.fromString(ncStr2);
        if (nc2 == null) {
            ci.println("Invalid input node connector 2 string: " + ncStr2);
            return;
        }

        TopologyUserLinkConfig config = new TopologyUserLinkConfig(name, ncStr1, ncStr2);
        ci.println(this.addUserLink(config));
    }

    public void _deleteUserLink(CommandInterpreter ci) {
        String name = ci.nextArgument();
        if ((name == null)) {
            ci.println("Please enter a valid Name");
            return;
        }
        this.deleteUserLink(name);
    }

    public void _printNodeEdges(CommandInterpreter ci) {
        Map<Node, Set<Edge>> nodeEdges = getNodeEdges();
        if (nodeEdges == null) {
            return;
        }
        Set<Node> nodeSet = nodeEdges.keySet();
        if (nodeSet == null) {
            return;
        }
        ci.println("        Node                                         Edge");
        for (Node node : nodeSet) {
            Set<Edge> edgeSet = nodeEdges.get(node);
            if (edgeSet == null) {
                continue;
            }
            for (Edge edge : edgeSet) {
                ci.println(node + "             " + edge);
            }
        }
    }

    @Override
    public Object readObject(ObjectInputStream ois)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }

    @Override
    public Status saveConfiguration() {
        return saveConfig();
    }

    @Override
    public void edgeOverUtilized(Edge edge) {
        log.warn("Link Utilization above normal: {}", edge);
    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {
        log.warn("Link Utilization back to normal: {}", edge);
    }

    private void edgeUpdateClusterWide(Edge e, UpdateType type, Set<Property> props, boolean isLocal) {
        TopoEdgeUpdate upd = new TopoEdgeUpdate(e, props, type);
        upd.setLocal(isLocal);
        notifyQ.add(upd);
    }

    @Override
    public void entryCreated(final Object key, final String cacheName, final boolean originLocal) {
        if (cacheName.equals(TOPOEDGESDB)) {
            // This is the case of an Edge being added to the topology DB
            final Edge e = (Edge) key;
            log.trace("Edge {} CREATED isLocal:{}", e, originLocal);
            edgeUpdateClusterWide(e, UpdateType.ADDED, null, originLocal);
        }
    }

    @Override
    public void entryUpdated(final Object key, final Object new_value, final String cacheName, final boolean originLocal) {
        if (cacheName.equals(TOPOEDGESDB)) {
            final Edge e = (Edge) key;
            log.trace("Edge {} UPDATED isLocal:{}", e, originLocal);
            final Set<Property> props = (Set<Property>) new_value;
            edgeUpdateClusterWide(e, UpdateType.CHANGED, props, originLocal);
        }
    }

    @Override
    public void entryDeleted(final Object key, final String cacheName, final boolean originLocal) {
        if (cacheName.equals(TOPOEDGESDB)) {
            final Edge e = (Edge) key;
            log.trace("Edge {} DELETED isLocal:{}", e, originLocal);
            edgeUpdateClusterWide(e, UpdateType.REMOVED, null, originLocal);
        }
    }

    class TopologyNotify implements Runnable {
        private final BlockingQueue<TopoEdgeUpdate> notifyQ;
        private TopoEdgeUpdate entry;
        private List<TopoEdgeUpdate> teuList = new ArrayList<TopoEdgeUpdate>();
        private boolean notifyListeners;

        TopologyNotify(BlockingQueue<TopoEdgeUpdate> notifyQ) {
            this.notifyQ = notifyQ;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    log.trace("New run of TopologyNotify");
                    notifyListeners = false;
                    // First we block waiting for an element to get in
                    entry = notifyQ.take();
                    // Then we drain the whole queue if elements are
                    // in it without getting into any blocking condition
                    for (; entry != null; entry = notifyQ.poll()) {
                        teuList.add(entry);
                        notifyListeners = true;
                    }

                    // Notify listeners only if there were updates drained else
                    // give up
                    if (notifyListeners) {
                        log.trace("Notifier thread, notified a listener");
                        // Now update the listeners
                        for (ITopologyManagerClusterWideAware s : topologyManagerClusterWideAware) {
                            try {
                                s.edgeUpdate(teuList);
                            } catch (Exception exc) {
                                log.error("Exception on edge update:", exc);
                            }
                        }
                    }
                    teuList.clear();

                    // Lets sleep for sometime to allow aggregation of event
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    if (shuttingDown) {
                        return;
                    }
                    log.warn("TopologyNotify interrupted {}", e1.getMessage());
                } catch (Exception e2) {
                    log.error("", e2);
                }
            }
        }
    }
}
