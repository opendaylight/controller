
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
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
import org.opendaylight.controller.sal.topology.ITopologyService;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
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
public class TopologyManagerImpl implements ITopologyManager,
        IConfigurationContainerAware, IListenTopoUpdates, IObjectReader,
        CommandProvider {
    private static final Logger log = LoggerFactory
            .getLogger(TopologyManagerImpl.class);
    private ITopologyService topoService = null;
    private IClusterContainerServices clusterContainerService = null;
    // DB of all the Edges with properties which constitute our topology
    private ConcurrentMap<Edge, Set<Property>> edgesDB = null;
    // DB of all NodeConnector which are part of Edges, meaning they
    // are connected to another NodeConnector on the other side
    private ConcurrentMap<NodeConnector, Set<Property>> nodeConnectorsDB = null;
    // DB of all the NodeConnectors with an Host attached to it
    private ConcurrentMap<NodeConnector, ImmutablePair<Host, Set<Property>>> hostsDB = null;
    // Topology Manager Aware listeners
    private Set<ITopologyManagerAware> topologyManagerAware = Collections
            .synchronizedSet(new HashSet<ITopologyManagerAware>());

    private static String ROOT = GlobalConstants.STARTUPHOME.toString();
    private String userLinksFileName = null;
    private ConcurrentMap<String, TopologyUserLinkConfig> userLinks;
    
    void nonClusterObjectCreate() {
    	edgesDB = new ConcurrentHashMap<Edge, Set<Property>>();
    	hostsDB = new ConcurrentHashMap<NodeConnector, ImmutablePair<Host, Set<Property>>>();
    	userLinks = new ConcurrentHashMap<String, TopologyUserLinkConfig>();
    	nodeConnectorsDB = new ConcurrentHashMap<NodeConnector, Set<Property>>();
    }
    

    void setTopologyManagerAware(ITopologyManagerAware s) {
        if (this.topologyManagerAware != null) {
        	log.debug("Adding ITopologyManagerAware: " + s);
            this.topologyManagerAware.add(s);
        }
    }

    void unsetTopologyManagerAware(ITopologyManagerAware s) {
        if (this.topologyManagerAware != null) {
            this.topologyManagerAware.remove(s);
        }
    }

    void setTopoService(ITopologyService s) {
        this.topoService = s;
    }

    void unsetTopoService(ITopologyService s) {
        if (this.topoService == s) {
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

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        String containerName = null;
        Dictionary props = c.getServiceProperties();
        if (props != null) {
            containerName = (String) props.get("containerName");
        } else {
            // In the Global instance case the containerName is empty
            containerName = "UNKNOWN";
        }

        if (this.clusterContainerService == null) {
            log.error("Cluster Services is null, not expected!");
            return;
        }

        if (this.topoService == null) {
            log.error("Topology Services is null, not expected!");
            return;
        }

        try {
            this.edgesDB = (ConcurrentMap<Edge, Set<Property>>) this.clusterContainerService
                    .createCache("topologymanager.edgesDB", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("topologymanager.edgesDB Cache already exists - "
                    + "destroy and recreate if needed");
        } catch (CacheConfigException cce) {
            log.error("topologymanager.edgesDB Cache configuration invalid - "
                    + "check cache mode");
        }

        try {
            this.hostsDB = (ConcurrentMap<NodeConnector, ImmutablePair<Host, Set<Property>>>) this.clusterContainerService
                    .createCache("topologymanager.hostsDB", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("topologymanager.hostsDB Cache already exists - "
                    + "destroy and recreate if needed");
        } catch (CacheConfigException cce) {
            log.error("topologymanager.hostsDB Cache configuration invalid - "
                    + "check cache mode");
        }

        try {
            this.nodeConnectorsDB = (ConcurrentMap<NodeConnector, Set<Property>>) this.clusterContainerService
                    .createCache("topologymanager.nodeConnectorDB", EnumSet
                            .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("topologymanager.nodeConnectorDB Cache already exists"
                    + " - destroy and recreate if needed");
        } catch (CacheConfigException cce) {
            log.error("topologymanager.nodeConnectorDB Cache configuration "
                    + "invalid - check cache mode");
        }

        userLinks = new ConcurrentHashMap<String, TopologyUserLinkConfig>();

        userLinksFileName = ROOT + "userTopology_" + containerName + ".conf";
        registerWithOSGIConsole();
        loadConfiguration();
    }

    /**
     * Function called after the topology manager has registered the
     * service in OSGi service registry.
     *
     */
    void started() {
        // SollicitRefresh MUST be called here else if called at init
        // time it may sollicit refresh too soon.
        log.debug("Sollicit topology refresh");
        topoService.sollicitRefresh();
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        if (this.clusterContainerService == null) {
            log.error("Cluster Services is null, not expected!");
            this.edgesDB = null;
            this.hostsDB = null;
            this.nodeConnectorsDB = null;
            return;
        }
        this.clusterContainerService.destroyCache("topologymanager.edgesDB");
        this.edgesDB = null;
        this.clusterContainerService.destroyCache("topologymanager.hostsDB");
        this.hostsDB = null;
        this.clusterContainerService
                .destroyCache("topologymanager.nodeConnectorDB");
        this.nodeConnectorsDB = null;
        log.debug("Topology Manager DB DE-allocated");
    }

    @SuppressWarnings("unchecked")
    private void loadConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, TopologyUserLinkConfig> confList = (ConcurrentMap<String, TopologyUserLinkConfig>) objReader
                .read(this, userLinksFileName);

        if (confList == null) {
            return;
        }

        for (TopologyUserLinkConfig conf : confList.values()) {
            addUserLink(conf);
        }
    }

    @Override
    public Status saveConfig() {
        // Publish the save config event to the cluster nodes
        /**
         * Get the CLUSTERING SERVICES WORKING BEFORE TRYING THIS

        configSaveEvent.put(new Date().getTime(), SAVE);
         */
        return saveConfigInternal();
    }

    public Status saveConfigInternal() {
    	Status retS;
        ObjectWriter objWriter = new ObjectWriter();

        retS = objWriter.write(
                new ConcurrentHashMap<String, TopologyUserLinkConfig>(
                        userLinks), userLinksFileName);

        if (retS.isSuccess()) {
            return retS;
        } else {
            return new Status(StatusCode.INTERNALERROR, "Save failed");
        }
    }

    @Override
    public Map<Node, Set<Edge>> getNodeEdges() {
        if (this.edgesDB == null) {
            return null;
        }

        HashMap<Node, Set<Edge>> res = new HashMap<Node, Set<Edge>>();
        for (Edge key : this.edgesDB.keySet()) {
            // Lets analyze the tail
            Node node = key.getTailNodeConnector().getNode();
            Set<Edge> nodeEdges = res.get(node);
            if (nodeEdges == null) {
                nodeEdges = new HashSet<Edge>();
            }
            nodeEdges.add(key);
            // We need to re-add to the MAP even if the element was
            // already there so in case of clustered services the map
            // gets updated in the cluster
            res.put(node, nodeEdges);

            // Lets analyze the head
            node = key.getHeadNodeConnector().getNode();
            nodeEdges = res.get(node);
            if (nodeEdges == null) {
                nodeEdges = new HashSet<Edge>();
            }
            nodeEdges.add(key);
            // We need to re-add to the MAP even if the element was
            // already there so in case of clustered services the map
            // gets updated in the cluster
            res.put(node, nodeEdges);
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
     * The Map returned is a copy of the current topology hence if the
     * topology changes the copy doesn't
     *
     * @return A Map representing the current topology expressed as
     * edges of the network
     */
    @Override
    public Map<Edge, Set<Property>> getEdges() {
        if (this.edgesDB == null) {
            return null;
        }

        HashMap<Edge, Set<Property>> res = new HashMap<Edge, Set<Property>>();
        for (Edge key : this.edgesDB.keySet()) {
            // Sets of props are copied because the composition of
            // those properties could change with time
            HashSet<Property> prop = new HashSet<Property>(this.edgesDB
                    .get(key));
            // We can simply reuse the key because the object is
            // immutable so doesn't really matter that we are
            // referencing the only owned by a different table, the
            // meaning is the same because doesn't change with time.
            res.put(key, prop);
        }

        return res;
    }

    // TODO remove with spring-dm removal
    /**
     * @param set the topologyAware to set
     */
    public void setTopologyAware(Set<Object> set) {
        for (Object s : set) {
            setTopologyManagerAware((ITopologyManagerAware) s);
        }
    }

    @Override
    public Set<NodeConnector> getNodeConnectorWithHost() {
        if (this.hostsDB == null) {
            return null;
        }

        return (this.hostsDB.keySet());
    }

    @Override
    public Map<Node, Set<NodeConnector>> getNodesWithNodeConnectorHost() {
        if (this.hostsDB == null) {
            return null;
        }
        HashMap<Node, Set<NodeConnector>> res = new HashMap<Node, Set<NodeConnector>>();

        for (NodeConnector p : this.hostsDB.keySet()) {
            Node n = p.getNode();
            Set<NodeConnector> pSet = res.get(n);
            if (pSet == null) {
                // Create the HashSet if null
                pSet = new HashSet<NodeConnector>();
                res.put(n, pSet);
            }

            // Keep updating the HashSet, given this is not a
            // clustered map we can just update the set without
            // worrying to update the hashmap.
            pSet.add(p);
        }

        return (res);
    }

    @Override
    public Host getHostAttachedToNodeConnector(NodeConnector p) {
        if (this.hostsDB == null) {
            return null;
        }

        return (this.hostsDB.get(p).getLeft());
    }

    @Override
    public void updateHostLink(NodeConnector p, Host h, UpdateType t,
            Set<Property> props) {
        if (this.hostsDB == null) {
            return;
        }

        switch (t) {
        case ADDED:
        case CHANGED:
            // Clone the property set in case non null else just
            // create an empty one. Caches allocated via infinispan
            // don't allow null values
            if (props == null) {
                props = new HashSet<Property>();
            } else {
                props = new HashSet<Property>(props);
            }

            this.hostsDB.put(p, new ImmutablePair(h, props));
            break;
        case REMOVED:
            this.hostsDB.remove(p);
            break;
        }
    }

    @Override
    public void edgeUpdate(Edge e, UpdateType type, Set<Property> props) {
        switch (type) {
        case ADDED:
            // Make sure the props are non-null
            if (props == null) {
                props = (Set<Property>) new HashSet();
            } else {
                // Copy the set so noone is going to change the content
                props = (Set<Property>) new HashSet(props);
            }

            // Now make sure thre is the creation timestamp for the
            // edge, if not there timestamp with the first update
            boolean found_create = false;
            for (Property prop : props) {
                if (prop instanceof TimeStamp) {
                    TimeStamp t = (TimeStamp) prop;
                    if (t.getTimeStampName().equals("creation")) {
                        found_create = true;
                    }
                }
            }

            if (!found_create) {
                TimeStamp t = new TimeStamp(System.currentTimeMillis(),
                        "creation");
                props.add(t);
            }

            // Now add this in the database eventually overriding
            // something that may have been already existing
            this.edgesDB.put(e, props);

            // Now populate the DB of NodeConnectors
            // NOTE WELL: properties are empy sets, not really needed
            // for now.
            this.nodeConnectorsDB.put(e.getHeadNodeConnector(),
                    new HashSet<Property>());
            this.nodeConnectorsDB.put(e.getTailNodeConnector(),
                    new HashSet<Property>());
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
            break;
        case CHANGED:
            Set<Property> old_props = this.edgesDB.get(e);

            // When property changes lets make sure we can change it
            // all except the creation time stamp because that should
            // be changed only when the edge is destroyed and created
            // again
            TimeStamp tc = null;
            for (Property prop : old_props) {
                if (prop instanceof TimeStamp) {
                    TimeStamp t = (TimeStamp) prop;
                    if (t.getTimeStampName().equals("creation")) {
                        tc = t;
                    }
                }
            }

            // Now lest make sure new properties are non-null
            // Make sure the props are non-null
            if (props == null) {
                props = (Set<Property>) new HashSet();
            } else {
                // Copy the set so noone is going to change the content
                props = (Set<Property>) new HashSet(props);
            }

            // Now lets remove the creation property if exist in the
            // new props
            for (Iterator<Property> i = props.iterator(); i.hasNext();) {
                Property prop = i.next();
                if (prop instanceof TimeStamp) {
                    TimeStamp t = (TimeStamp) prop;
                    if (t.getTimeStampName().equals("creation")) {
                        i.remove();
                    }
                }
            }

            // Now lets add the creation timestamp in it
            if (tc != null) {
                props.add(tc);
            }

            // Finally update
            this.edgesDB.put(e, props);
            break;
        }

        // Now update the listeners
        for (ITopologyManagerAware s : this.topologyManagerAware) {
            try {
                s.edgeUpdate(e, type, props);
            } catch (Exception exc) {
                log.error("Exception on callback", exc);
            }
        }
    }

    private Edge getReverseLinkTuple(TopologyUserLinkConfig link) {
		TopologyUserLinkConfig rLink = new TopologyUserLinkConfig(
				link.getName(), link.getDstNodeIDType(), link.getDstSwitchId(),
				link.getDstNodeConnectorIDType(), link.getDstPort(),
				link.getSrcNodeIDType(), link.getSrcSwitchId(),
				link.getSrcNodeConnectorIDType(), link.getSrcPort());
        return getLinkTuple(rLink);
    }

    private Edge getLinkTuple(TopologyUserLinkConfig link) {
        Edge linkTuple = null;

        // if atleast 1 link exists for the srcPort and atleast 1 link exists for the dstPort
        // that makes it ineligible for the Manual link addition
        // This is just an extra protection to avoid mis-programming.
        boolean srcLinkExists = false;
        boolean dstLinkExists = false;
        //TODO check a way to validate the port with inventory services
        //if (srcSw.getPorts().contains(srcPort) &&
        //dstSw.getPorts().contains(srcPort) &&
        if (!srcLinkExists && !dstLinkExists) {
            Node sNode = null;
            Node dNode = null;
            NodeConnector sPort = null;
            NodeConnector dPort = null;
            linkTuple = null;
            String srcNodeIDType = link.getSrcNodeIDType();
            String srcNodeConnectorIDType = link.getSrcNodeConnectorIDType();
            String dstNodeIDType = link.getDstNodeIDType();
            String dstNodeConnectorIDType = link.getDstNodeConnectorIDType();
            try {
            	if (srcNodeIDType.equals(NodeIDType.OPENFLOW)) {
                    sNode = new Node(srcNodeIDType, link.getSrcSwitchIDLong());
            	} else {
                    sNode = new Node(srcNodeIDType, link.getSrcSwitchId());            		
            	}

            	if (dstNodeIDType.equals(NodeIDType.OPENFLOW)) {
                    dNode = new Node(dstNodeIDType, link.getDstSwitchIDLong());
            	} else {
                    dNode = new Node(dstNodeIDType, link.getDstSwitchId());            		
            	}
    	
            	if (srcNodeConnectorIDType.equals(NodeConnectorIDType.OPENFLOW)) {
                    Short srcPort = Short.valueOf((short) 0);
	        		if (!link.isSrcPortByName()) {
	        			srcPort = Short.parseShort(link.getSrcPort());
	        		}
					sPort = new NodeConnector(srcNodeConnectorIDType, 
							srcPort, sNode);
            	} else {
    				sPort = new NodeConnector(srcNodeConnectorIDType,
    						link.getSrcPort(), sNode);            		
            	}

            	if (dstNodeConnectorIDType.equals(NodeConnectorIDType.OPENFLOW)) {
                    Short dstPort = Short.valueOf((short) 0);
	        		if (!link.isDstPortByName()) {
	                    dstPort = Short.parseShort(link.getDstPort());
	                }
					dPort = new NodeConnector(dstNodeConnectorIDType,
							dstPort, dNode);
            	} else {
					dPort = new NodeConnector(dstNodeConnectorIDType,
							link.getDstPort(), dNode);
            	}
                linkTuple = new Edge(sPort, dPort);
            } catch (ConstructionException cex) {
            	log.warn("Caught exception ", cex);
            }
            return linkTuple;
        }

        if (srcLinkExists && dstLinkExists) {
            link.setStatus(TopologyUserLinkConfig.STATUS.INCORRECT);
        }
        return null;
    }

    @Override
    public ConcurrentMap<String, TopologyUserLinkConfig> getUserLinks() {
        return userLinks;
    }

    @Override
    public Status addUserLink(TopologyUserLinkConfig link) {
        if (!link.isValid()) {
            return new Status(StatusCode.BADREQUEST, 
            		"Configuration Invalid. Please check the parameters");
        }
        if (userLinks.get(link.getName()) != null) {
            return new Status(StatusCode.CONFLICT, 
            		"Link with name : " + link.getName()
                    + " already exists. Please use another name");
        }
        if (userLinks.containsValue(link)) {
            return new Status(StatusCode.CONFLICT, "Link configuration exists");
        }

        link.setStatus(TopologyUserLinkConfig.STATUS.LINKDOWN);
        userLinks.put(link.getName(), link);

        Edge linkTuple = getLinkTuple(link);
        if (linkTuple != null) {
            try {
                linkTuple = getReverseLinkTuple(link);
                link.setStatus(TopologyUserLinkConfig.STATUS.SUCCESS);
            } catch (Exception e) {
                return new Status(StatusCode.INTERNALERROR,
                		"Exception while adding custom link : " + 
                				e.getMessage());
            }
        }
        return new Status(StatusCode.SUCCESS, null);
    }

    @Override
    public Status deleteUserLink(String linkName) {
        if (linkName == null) {
            return new Status(StatusCode.BADREQUEST, 
            		"A valid linkName is required to Delete a link");
        }

        TopologyUserLinkConfig link = userLinks.get(linkName);

        Edge linkTuple = getLinkTuple(link);
        userLinks.remove(linkName);
        if (linkTuple != null) {
            try {
                //oneTopology.deleteUserConfiguredLink(linkTuple);
            } catch (Exception e) {
                log
                        .warn("Harmless : Exception while Deleting User Configured link "
                                + link + " " + e.toString());
            }
            linkTuple = getReverseLinkTuple(link);
            try {
                //oneTopology.deleteUserConfiguredLink(linkTuple);
            } catch (Exception e) {
                log
                        .error("Harmless : Exception while Deleting User Configured Reverse link "
                                + link + " " + e.toString());
            }
        }
        return new Status(StatusCode.SUCCESS, null);
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
		help.append("\t addTopo name <NodeIDType> <src-sw-id> <NodeConnectorIDType> <port-number> <NodeIDType> <dst-sw-id> <NodeConnectorIDType> <port-number>\n");
        help.append("\t delTopo name\n");
        help.append("\t printTopo\n");
        help.append("\t printNodeEdges\n");
        return help.toString();
    }

    public void _printTopo(CommandInterpreter ci) {
        for (String name : this.userLinks.keySet()) {
        	TopologyUserLinkConfig linkConfig = userLinks.get(name);
            ci.println("Name : " + name);
            ci.println(linkConfig);
            ci.println("Edge " +  getLinkTuple(linkConfig));
            ci.println("Reverse Edge " +  getReverseLinkTuple(linkConfig));
        }
    }

    public void _addTopo(CommandInterpreter ci) {
        String name = ci.nextArgument();
        if ((name == null)) {
            ci.println("Please enter a valid Name");
            return;
        }

        String srcNodeIDType = ci.nextArgument();
        if (srcNodeIDType == null) {
            ci.println("Null source node ID Type. Example: OF or PR");
            return;
        }

        String dpid = ci.nextArgument();
        if (dpid == null) {
            ci.println("Null source node id");
            return;
        }

        String srcNodeConnectorIDType = ci.nextArgument();
        if (srcNodeConnectorIDType == null) {
            ci.println("Null source node connector ID Type. Example: OF or PR");
            return;
        }

        String port = ci.nextArgument();
        if (port == null) {
            ci.println("Null source port number");
            return;
        }

        String dstNodeIDType = ci.nextArgument();
        if (dstNodeIDType == null) {
            ci.println("Null destination node ID Type. Example: OF or PR");
            return;
        }

        String ddpid = ci.nextArgument();
        if (ddpid == null) {
            ci.println("Null destination node ID");
            return;
        }

        String dstNodeConnectorIDType = ci.nextArgument();
        if (dstNodeConnectorIDType == null) {
            ci.println("Null destination node connector ID Type. Example: OF or PR");
            return;
        }

        String dport = ci.nextArgument();
        if (dport == null) {
            ci.println("Null destination port number");
            return;
        }
		TopologyUserLinkConfig config = new TopologyUserLinkConfig(name,
				srcNodeIDType, dpid, srcNodeConnectorIDType, port,
				dstNodeIDType, ddpid, dstNodeConnectorIDType, dport);
        ci.println(this.addUserLink(config));
    }

    public void _delTopo(CommandInterpreter ci) {
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
        // TODO Auto-generated method stub
        return ois.readObject();
    }

    @Override
    public Status saveConfiguration() {
        return saveConfig();
    }

    @Override
    public void edgeOverUtilized(Edge edge) {
        log.warn("Link Utilization above normal: " + edge);
    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {
        log.warn("Link Utilization back to normal: " + edge);
    }

}
