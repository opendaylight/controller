
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SAL Read Service. It dispatches the read request to
 * the proper SDN protocol plugin
 *
 *
 *
 */
public class ReadService implements IReadService, CommandProvider {

    protected static final Logger logger = LoggerFactory
            .getLogger(ReadService.class);
    private ConcurrentHashMap<String, IPluginInReadService>
        pluginReader =
        new ConcurrentHashMap<String, IPluginInReadService>();

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        // In case of plugin disactivating make sure we clear the
        // dependencies
        this.pluginReader.clear();
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        registerWithOSGIConsole();
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    // Set the reference to the plugin flow Reader service
    public void setService(Map props, IPluginInReadService s) {
        if (this.pluginReader == null) {
            logger.error("pluginReader store null");
            return;
        }

        logger.trace("Got a service set request {}", s);
        String type = null;
        for (Object e : props.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            logger.trace("Prop key:({}) value:({})", entry.getKey(),
            		entry.getValue());
        }

        Object value = props.get("protocolPluginType");
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a pluginReader without any "
                    + "protocolPluginType provided");
        } else {
            this.pluginReader.put(type, s);
            logger.debug("Stored the pluginReader for type: {}", type);
        }
    }

    public void unsetService(Map props, IPluginInReadService s) {
        if (this.pluginReader == null) {
            logger.error("pluginReader store null");
            return;
        }

        String type = null;
        logger.debug("Received unsetpluginReader request");
        for (Object e : props.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            logger.trace("Prop key:({}) value:({})", entry.getKey(),
                    	entry.getValue());
        }

        Object value = props.get("protocoloPluginType");
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a pluginReader without any "
                    + "protocolPluginType provided");
        } else if (this.pluginReader.get(type).equals(s)) {
            this.pluginReader.remove(type);
            logger.debug("Removed the pluginReader for type: {}", type);
        }
    }

    @Override
    public FlowOnNode readFlow(Node node, Flow flow) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readFlow(node, flow, true);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public FlowOnNode nonCachedReadFlow(Node node, Flow flow) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readFlow(node, flow, false);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public List<FlowOnNode> readAllFlows(Node node) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readAllFlow(node, true);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public List<FlowOnNode> nonCachedReadAllFlows(Node node) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readAllFlow(node, false);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public NodeDescription readDescription(Node node) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readDescription(node, true);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public NodeDescription nonCachedReadDescription(Node node) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readDescription(node, false);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public NodeConnectorStatistics readNodeConnector(NodeConnector connector) {
        Node node = connector.getNode();
        if (pluginReader != null && node != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readNodeConnector(connector, true);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public NodeConnectorStatistics nonCachedReadNodeConnector(
            NodeConnector connector) {
        Node node = connector.getNode();
        if (pluginReader != null && node != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readNodeConnector(connector, false);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public List<NodeConnectorStatistics> readNodeConnectors(Node node) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readAllNodeConnector(node, true);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public List<NodeConnectorStatistics> nonCachedReadNodeConnectors(Node node) {
        if (pluginReader != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .readAllNodeConnector(node, false);
            }
        }
        logger.warn("Plugin unavailable");
        return null;
    }

    @Override
    public long getTransmitRate(NodeConnector connector) {
        Node node = connector.getNode();
        if (pluginReader != null && node != null) {
            if (this.pluginReader.get(node.getType()) != null) {
                return this.pluginReader.get(node.getType())
                    .getTransmitRate(connector);
            }
        }
        logger.warn("Plugin unavailable");
        return 0;
    }

    // ---------------- OSGI TEST CODE ------------------------------//

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---SAL Reader testing commands---\n");
        help
                .append("\t readflows <sid> <cached>  - Read all the (cached) flows from the openflow switch <sid>\n");
        help
                .append("\t readflow  <sid> <cached>  - Read the (cached) sample flow from the openflow switch <sid>\n");
        help
                .append("\t readdesc  <sid> <cached>  - Read the (cached) description from openflow switch <sid>\n");
        help
                .append("\t           cached=true/false. If false or not specified, the protocol plugin cached info\n");
        help
                .append("\t           is returned. If true, the info is directly retrieved from the switch\n");
        return help.toString();
    }

    public void _readflows(CommandInterpreter ci) {
        String nodeId = ci.nextArgument();
        String cacheReq = ci.nextArgument();
        boolean cached;
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");
        Node node = null;
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        List<FlowOnNode> list = (cached) ? this.readAllFlows(node) : this
                .nonCachedReadAllFlows(node);
        if (list != null) {
            ci.println(list.toString());
        } else {
            ci.println("null");
        }
    }

    // Requests the hw view for the specific sample flow
    public void _readflow(CommandInterpreter ci) throws UnknownHostException {
        String nodeId = ci.nextArgument();
        String cacheReq = ci.nextArgument();
        boolean cached;
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");
        Node node = null;
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        Flow flow = getSampleFlow(node);
        FlowOnNode flowOnNode = (cached) ? this.readFlow(node, flow) : this
                .nonCachedReadFlow(node, flow);
        if (flowOnNode != null) {
            ci.println(flowOnNode.toString());
        } else {
            ci.println("null");
        }
    }

    public void _readports(CommandInterpreter ci) {
        String nodeId = ci.nextArgument();
        String cacheReq = ci.nextArgument();
        boolean cached;
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");
        Node node = null;
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        List<NodeConnectorStatistics> list = (cached) ? this
                .readNodeConnectors(node) : this
                .nonCachedReadNodeConnectors(node);
        if (list != null) {
            ci.println(list.toString());
        } else {
            ci.println("null");
        }
    }

    public void _readport(CommandInterpreter ci) {
        String nodeId = ci.nextArgument();
        String portId = ci.nextArgument();
        String cacheReq = ci.nextArgument();
        boolean cached;
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        if (portId == null) {
            ci.print("Port id not specified");
            return;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");
        NodeConnector nodeConnector = null;
        Node node = NodeCreator.createOFNode(Long.parseLong(nodeId));
        nodeConnector = NodeConnectorCreator.createNodeConnector(Short
                .valueOf(portId), node);
        NodeConnectorStatistics stats = (cached) ? this
                .readNodeConnector(nodeConnector) : this
                .nonCachedReadNodeConnector(nodeConnector);
        if (stats != null) {
            ci.println(stats.toString());
        } else {
            ci.println("null");
        }
    }

    public void _readdescr(CommandInterpreter ci) {
        String nodeId = ci.nextArgument();
        String cacheReq = ci.nextArgument();
        boolean cached;
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");

        Node node = null;
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        NodeDescription desc = (cached) ? this.readDescription(node) : this
                .nonCachedReadDescription(node);
        if (desc != null) {
            ci.println(desc.toString());
        } else {
            ci.println("null");
        }
    }

    private Flow getSampleFlow(Node node) throws UnknownHostException {
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        NodeConnector oport = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress.getByName("172.28.30.50");
        InetAddress dstIP = InetAddress.getByName("171.71.9.52");
        InetAddress ipMask = InetAddress.getByName("255.255.255.0");
        InetAddress ipMask2 = InetAddress.getByName("255.0.0.0");
        short ethertype = EtherTypes.IPv4.shortValue();
        short vlan = (short) 27;
        byte vlanPr = 3;
        Byte tos = 4;
        byte proto = IPProtocols.TCP.byteValue();
        short src = (short) 55000;
        short dst = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match = new Match();
        match.setField(MatchType.IN_PORT, port);
        match.setField(MatchType.DL_SRC, srcMac);
        match.setField(MatchType.DL_DST, dstMac);
        match.setField(MatchType.DL_TYPE, ethertype);
        match.setField(MatchType.DL_VLAN, vlan);
        match.setField(MatchType.DL_VLAN_PR, vlanPr);
        match.setField(MatchType.NW_SRC, srcIP, ipMask);
        match.setField(MatchType.NW_DST, dstIP, ipMask2);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src);
        match.setField(MatchType.TP_DST, dst);

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(oport));
        actions.add(new PopVlan());
        actions.add(new Flood());
        actions.add(new Controller());
        return new Flow(match, actions);
    }

}
