
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topology.web;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.usermanager.IUserManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;

@Controller
@RequestMapping("/")
public class Topology {

    protected Map<String, Map<String, Object>> cache = new HashMap<String, Map<String, Object>>();
    protected Map<String, Map<String, Object>> stagedNodes;
    protected Map<String, Map<String, Object>> newNodes;
    protected Integer nodeHash = null;
    protected Integer hostHash = null;
    protected Integer nodeSingleHash = null;
    protected Integer nodeConfigurationHash = null;
    
    /**
     * Topology of nodes and hosts in the network in JSON format.
     * 
     * Mainly intended for consumption by the visual topology.
     * 
     * @return - JSON output for visual topology
     */
    @RequestMapping(value = "/visual.json", method = RequestMethod.GET)
    @ResponseBody
    public Collection<Map<String, Object>> getLinkData() {
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
                .getInstance(ITopologyManager.class, "default", this);
        if (topologyManager == null) return null;
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, "default", this);
        if (switchManager == null) return null;
        
        Map<Node, Set<Edge>> nodeEdges = topologyManager.getNodeEdges();
        Map<Node, Set<NodeConnector>> hostEdges = topologyManager
                .getNodesWithNodeConnectorHost();
        List<Switch> nodes = switchManager.getNetworkDevices();
        
        List<SwitchConfig> switchConfigurations = new ArrayList<SwitchConfig>();
        for(Switch sw : nodes) {
        	Node n = sw.getNode();
        	SwitchConfig config = switchManager.getSwitchConfig(n.toString());
        	switchConfigurations.add(config);
        }
        
        // return cache if topology hasn't changed
        if (
        	(nodeHash != null && hostHash != null && nodeSingleHash != null && nodeConfigurationHash != null) &&
        	nodeHash == nodeEdges.hashCode() && hostHash == hostEdges.hashCode() && nodeSingleHash == nodes.hashCode() && nodeConfigurationHash == switchConfigurations.hashCode()
        ) {
        	return cache.values();
        }
        
        // cache has changed, we must assign the new values
        nodeHash = nodeEdges.hashCode();
        hostHash = hostEdges.hashCode();
        nodeSingleHash = nodes.hashCode();
        nodeConfigurationHash = switchConfigurations.hashCode();
        
        stagedNodes = new HashMap<String, Map<String, Object>>();
        newNodes = new HashMap<String, Map<String, Object>>();

        // nodeEdges addition
        addNodes(nodeEdges, topologyManager, switchManager);

        // single nodes addition
        addSingleNodes(nodes, switchManager);
        
        // hostNodes addition
        addHostNodes(hostEdges, topologyManager);
        
        repositionTopology();
        
        return cache.values();
    }

    /**
     * Add regular nodes to main topology
     *
     * @param nodeEdges - node-edges mapping derived from topology
     * @param topology - the topology instance
     */
    private void addNodes(Map<Node, Set<Edge>> nodeEdges,
            ITopologyManager topology, ISwitchManager switchManager) {
        Bandwidth bandwidth = new Bandwidth(0);
        Map<Edge, Set<Property>> properties = topology.getEdges();
        
        for (Map.Entry<Node, Set<Edge>> e : nodeEdges.entrySet()) {
            Node n = e.getKey();
            String description = switchManager.getNodeDescription(n);
            NodeBean node = createNodeBean(description, n);
            
            List<Map<String, Object>> adjacencies = new LinkedList<Map<String, Object>>();
            Set<Edge> links = e.getValue();
            for (Edge link : links) {
                for (Property p : properties.get(link)) {
                    if (p instanceof Bandwidth) {
                    	bandwidth = (Bandwidth) p;
                        break;
                    }
                }
                EdgeBean edge = new EdgeBean(link, bandwidth);
                adjacencies.add(edge.out());
            }
            
            node.setLinks(adjacencies);
            if (cache.containsKey(node.id())) {
            	// retrieve node from cache
            	Map<String, Object> nodeEntry = cache.get(node.id());

        		Map<String, String> data = (Map<String, String>) nodeEntry.get("data");
        		data.put("$desc", description);
        		nodeEntry.put("data", data);
        		
            	// always update adjacencies
            	nodeEntry.put("adjacencies", adjacencies);
            	// stage this cached node (with position)
            	stagedNodes.put(node.id(), nodeEntry);
            } else {
            	newNodes.put(node.id(), node.out());
            }
        }
    }
    
    protected NodeBean createNodeBean(String description, Node node) {
    	String name = (description == null || 
    			description.trim().isEmpty() ||
    			description.equalsIgnoreCase("none"))?
    					node.toString() : description;
   		return  new NodeBean(node.toString(), name, NodeType.NODE);
    }
    
    @SuppressWarnings("unchecked")
	private void addSingleNodes(List<Switch> nodes, ISwitchManager switchManager) {
    	if (nodes == null) return;
    	for (Switch sw : nodes) {
    		Node n = sw.getNode();

    		String description = switchManager.getNodeDescription(n);
    		
    		if ((stagedNodes.containsKey(n.toString()) && cache.containsKey(n.toString())) || newNodes.containsKey(n.toString())) {
    			continue;
    		}
    		NodeBean node = createNodeBean(description, n);
    		
    		// FIXME still doesn't display standalone node when last remaining link is removed
    		if (cache.containsKey(node.id()) && !stagedNodes.containsKey(node.id())) {
    			Map<String, Object> nodeEntry = cache.get(node.id());
				Map<String, String> data = (Map<String, String>) nodeEntry.get("data");
        		data.put("$desc", description);
        		nodeEntry.put("data", data);
            	stagedNodes.put(node.id(), nodeEntry);
            } else {
            	newNodes.put(node.id(), node.out());
            }
    	}
    }

    /**
     * Add regular hosts to main topology
     *
     * @param hostEdges - node-nodeconnectors host-specific mapping from topology
     * @param topology - topology instance
     */
    private void addHostNodes(Map<Node, Set<NodeConnector>> hostEdges,
            ITopologyManager topology) {
        for (Map.Entry<Node, Set<NodeConnector>> e : hostEdges.entrySet()) {
            for (NodeConnector connector : e.getValue()) {
                Host host = topology.getHostAttachedToNodeConnector(connector);
                EthernetAddress dmac = (EthernetAddress) host.getDataLayerAddress();

                ByteBuffer addressByteBuffer = ByteBuffer.allocate(8);
                addressByteBuffer.putShort((short) 0);
                addressByteBuffer.put(dmac.getValue());
                addressByteBuffer.rewind();
                
                long hid = addressByteBuffer.getLong();
                String hostId = String.valueOf(hid);
                
                NodeBean hostBean = new NodeBean(hostId, host.getNetworkAddressAsString(), NodeType.HOST);
                List<Map<String, Object>> adjacencies = new LinkedList<Map<String, Object>>();
                EdgeBean edge = new EdgeBean(connector, hid);
                adjacencies.add(edge.out());
                hostBean.setLinks(adjacencies);
                
                if (cache.containsKey(hostId)) {
                	Map<String, Object> hostEntry = cache.get(hostId);
                	hostEntry.put("adjacencies", adjacencies);
                	stagedNodes.put(hostId, hostEntry);
                } else {
                	newNodes.put(String.valueOf(hid), hostBean.out());
                }
            }
        }
    }

    /**
     * Re-position nodes in circular layout
     */
    private void repositionTopology() {
        Graph<String, String> graph = new SparseMultigraph<String, String>();
        cache.clear();
        cache.putAll(stagedNodes);
        cache.putAll(newNodes);
        for (Map<String, Object> on : cache.values()) {
            graph.addVertex(on.toString());

            List<Map<String, Object>> adjacencies = (List<Map<String, Object>>) on.get("adjacencies");
            
            for (Map<String, Object> adj : adjacencies) {
                graph.addEdge(
                	adj.toString(), adj.get("nodeFrom").toString(),
                	adj.get("nodeTo").toString()
                );
            }
        }
        
        CircleLayout layout = new CircleLayout(graph);
        layout.setSize(new Dimension(1200, 365));
        for (Map.Entry<String, Map<String, Object>> v : newNodes.entrySet()) {
            Double x = layout.transform(v.getKey()).getX();
            Double y = layout.transform(v.getKey()).getY();

            Map<String, String> nodeData = (HashMap<String, String>) v.getValue().get("data");
            nodeData.put("$x", (x - 600) + "");
            nodeData.put("$y", (y - 225) + "");

            newNodes.get(v.getKey()).put("data", nodeData);
        }
    }

    /**
     * Update node position
     * 
     * This method is mainly used by the visual topology
     *
     * @param nodeId - The node to update
     * @return The node object
     */
    @RequestMapping(value = "/node/{nodeId}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> post(@PathVariable String nodeId, @RequestParam(required = true) String x,
    		@RequestParam(required = true) String y, HttpServletRequest request) {
    	if (!authorize(UserLevel.NETWORKADMIN, request)) {
    		return new HashMap<String, Object>(); // silently disregard new node position
    	}
    	
        String id = new String(nodeId);
        
        if (!cache.containsKey(id))
            return null;

        Map<String, Object> node = cache.get(id);
        Map<String, String> data = (Map<String, String>) node.get("data");

        data.put("$x", x);
        data.put("$y", y);

        node.put("data", data);
        
        return node;
    }
    
    /**
     * Node object for visual topology
     */
    protected class NodeBean {
    	protected String id;
    	protected String name;
    	protected Map<String, String> data;
    	protected List<Map<String, Object>> links;
    	
    	public NodeBean() {
    		data = new HashMap<String, String>();
    		links = new ArrayList<Map<String, Object>>();
    	}
    	
    	public NodeBean(String id, String name, String type) {
    		this();
    		this.id = id;
    		this.name = name;
    		data.put("$desc", name);
    		data.put("$type", type);
    	}
    	
    	public void setLinks(List<Map<String, Object>> links) {
    		this.links = links;
    	}
    	
    	public Map<String, Object> out() {
    		Map<String, Object> node = new HashMap<String, Object>();
    		node.put("id", this.id);
    		node.put("name", this.name);
    		node.put("data", this.data);
    		node.put("adjacencies", this.links);
    		
    		return node;
    	}
    	
    	public String name() {
    		return this.name;
    	}
    	
    	public String id() {
    		return this.id;
    	}
    }
    
    /**
     * Edge object for visual topology
     */
    protected class EdgeBean {
    	protected NodeConnector source;
    	protected NodeConnector destination;
    	protected Map<String, String> data;
    	protected Long hostId;
    	
    	public EdgeBean() {
    		data = new HashMap<String, String>();
    	}
    	
    	public EdgeBean(Edge link, Bandwidth bandwidth) {
    		this();
    		this.source = link.getHeadNodeConnector();
    		this.destination = link.getTailNodeConnector();
    		
    		// data
    		data.put("$bandwidth", bandwidth.toString());
    		data.put("$color", bandwidthColor(bandwidth));
    		data.put("$nodeToPort", destination.getID().toString());
    		data.put("$nodeFromPort", source.getID().toString());
    		data.put("$descFrom", source.getNode().toString());
    		data.put("$descTo", destination.getNode().toString());
    		data.put("$nodeFromPortName", source.toString());
    		data.put("$nodeToPortName", destination.toString());
    	}
    	
    	public EdgeBean(NodeConnector connector, Long hostId) {
    		this();
    		this.source = null;
    		this.destination = connector;
    		this.hostId = hostId;
    		
    		data.put("$bandwidth", "N/A");
    		data.put("$color", bandwidthColor(new Bandwidth(0)));
    		data.put("$nodeToPort", connector.getNodeConnectorIDString());
    		data.put("$nodeFromPort", connector.getNodeConnectorIDString());
    		data.put("$descTo", "");
    		data.put("$descFrom", "");
    		data.put("$nodeToPortName", "");
    		data.put("$nodeFromPortName", "");
    	}
    	
    	public Map<String, Object> out() {
    		Map<String, Object> edge = new HashMap<String, Object>();
    		
    		edge.put("data", data);
    		if (source == null) {
    			edge.put("nodeFrom", String.valueOf(this.hostId));
    		} else {
    			edge.put("nodeFrom", source.getNode().toString());
    		}
    		edge.put("nodeTo", destination.getNode().toString());
    		
    		
    		return edge;
    	}
    	
    	private String bandwidthColor(Bandwidth bandwidth) {
        	String color = null;
        	long bandwidthValue = bandwidth.getValue();
        	
        	if (bandwidthValue == 0) {
                color = "#000";
            } else if (bandwidthValue < Bandwidth.BW1Kbps) {
            	color = "#148AC6";
            } else if (bandwidthValue < Bandwidth.BW1Mbps) {
                color = "#2858A0";
            } else if (bandwidthValue < Bandwidth.BW1Gbps) {
                color = "#009393";
            } else if (bandwidthValue < Bandwidth.BW1Tbps) {
                color = "#C6C014";
            } else if (bandwidthValue < Bandwidth.BW1Pbps) {
                color = "#F9F464";
            }
        	
        	return color;
        }
    }
    
    protected class NodeType {
    	public static final String NODE = "swtch";
    	public static final String HOST = "host";
    }
    
    private boolean authorize(UserLevel level, HttpServletRequest request) {
    	IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
        	return false;
        }
        
        String username = request.getUserPrincipal().getName();
        UserLevel userLevel = userManager.getUserLevel(username);
        if (userLevel.toNumber() <= level.toNumber()) {
        	return true;
        }
        return false;
    }
}
