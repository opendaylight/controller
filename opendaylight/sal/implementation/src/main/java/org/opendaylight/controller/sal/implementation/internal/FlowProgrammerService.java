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
import java.util.concurrent.atomic.AtomicLong;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerListener;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SAL Flow Programmer Service. It dispatches the flow programming requests
 * to the proper SDN protocol plugin and it notifies about asynchronous messages
 * received from the network node related to flow programming.
 */
public class FlowProgrammerService implements IFlowProgrammerService,
        IPluginOutFlowProgrammerService, CommandProvider {

    protected static final Logger logger = LoggerFactory
            .getLogger(FlowProgrammerService.class);
    private ConcurrentHashMap<String, IPluginInFlowProgrammerService> pluginFlowProgrammer;
    private Set<IFlowProgrammerListener> listener;
    private AtomicLong seq;

    public FlowProgrammerService() {
        pluginFlowProgrammer = new ConcurrentHashMap<String, IPluginInFlowProgrammerService>();
        listener = new HashSet<IFlowProgrammerListener>();
        seq = new AtomicLong();
        /*
         * This Request ID generator starts with 1. Each aysnc message is
         * associated with an unique Request ID (!= 0).
         */
        seq.lazySet(1);
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.debug("INIT called!");
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        // Clear previous registration to avoid they are left hanging
        this.pluginFlowProgrammer.clear();
        logger.debug("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        logger.debug("START called!");
        // OSGI console
        registerWithOSGIConsole();
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
        logger.debug("STOP called!");
    }

    // Set the reference to the plugin flow programmer
    public void setService(Map<String, Object> props, IPluginInFlowProgrammerService s) {
        if (this.pluginFlowProgrammer == null) {
            logger.error("pluginFlowProgrammer store null");
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Got a service set request {}", s);
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                logger.trace("Prop key:({}) value:({})", entry.getKey(), entry.getValue());
            }
        }

        String type = null;
        Object value = props.get(GlobalConstants.PROTOCOLPLUGINTYPE.toString());
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a pluginFlowProgrammer without any "
                    + "protocolPluginType provided");
        } else {
            this.pluginFlowProgrammer.put(type, s);
            logger.debug("Stored the pluginFlowProgrammer for type: {}", type);
        }
    }

    public void unsetService(Map<String, Object> props, IPluginInFlowProgrammerService s) {
        if (this.pluginFlowProgrammer == null) {
            logger.error("pluginFlowProgrammer store null");
            return;
        }

        logger.debug("Received unsetpluginFlowProgrammer request");
        if (logger.isTraceEnabled()) {
            logger.trace("Got a service set request {}", s);
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                logger.trace("Prop key:({}) value:({})", entry.getKey(), entry.getValue());
            }
        }

        String type = null;
        Object value = props.get(GlobalConstants.PROTOCOLPLUGINTYPE.toString());
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a pluginFlowProgrammer without any "
                    + "protocolPluginType provided");
        } else if (this.pluginFlowProgrammer.get(type).equals(s)) {
            this.pluginFlowProgrammer.remove(type);
            logger.debug("Removed the pluginFlowProgrammer for type: {}", type);
        }
    }

    public void setListener(IFlowProgrammerListener s) {
        this.listener.add(s);
    }

    public void unsetListener(IFlowProgrammerListener s) {
        this.listener.remove(s);
    }

    @Override
    public Status addFlow(Node node, Flow flow) {
        if (pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType()).addFlow(
                        node, flow);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public Status removeFlow(Node node, Flow flow) {
        if (pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType())
                        .removeFlow(node, flow);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public Status removeAllFlows(Node node) {
        if (pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType())
                        .removeAllFlows(node);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public Status modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        if (pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType())
                        .modifyFlow(node, oldFlow, newFlow);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public Status addFlowAsync(Node node, Flow flow) {
        if (pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType()).addFlowAsync(
                        node, flow, getNextRid());
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public Status removeFlowAsync(Node node, Flow flow) {
        if (pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType())
                        .removeFlowAsync(node, flow, getNextRid());
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow) {
        if (pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType())
                        .modifyFlowAsync(node, oldFlow, newFlow, getNextRid());
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public void flowRemoved(Node node, Flow flow) {
        for (IFlowProgrammerListener l : listener) {
            l.flowRemoved(node, flow);
        }
    }

    @Override
    public void flowErrorReported(Node node, long rid, Object err) {
        logger.error("Got error {} for message rid {} from node {}",
                new Object[] { err, rid, node });

        for (IFlowProgrammerListener l : listener) {
            l.flowErrorReported(node, rid, err);
        }
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
        help.append("---SAL Flow Programmer testing commands---\n");
        help.append("\t addflow <sid> - Add a sample flow to the openflow switch <sid>\n");
        help.append("\t removeflow <sid> - Remove the sample flow from the openflow switch <sid>\n");
        return help.toString();
    }

    public void _addflow(CommandInterpreter ci) throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            logger.error("",e);
        } catch (ConstructionException e) {
            logger.error("",e);
        }
        ci.println(this.addFlow(node, getSampleFlow(node)));
    }

    public void _modifyflow(CommandInterpreter ci) throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            logger.error("",e);
        } catch (ConstructionException e) {
            logger.error("",e);
        }
        Flow flowA = getSampleFlow(node);
        Flow flowB = getSampleFlow(node);
        Match matchB = flowB.getMatch();
        matchB.setField(MatchType.NW_DST,
                InetAddress.getByName("190.190.190.190"));
        flowB.setMatch(matchB);
        ci.println(this.modifyFlow(node, flowA, flowB));
    }

    public void _removeflow(CommandInterpreter ci) throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            logger.error("",e);
        } catch (ConstructionException e) {
            logger.error("",e);
        }
        ci.println(this.removeFlow(node, getSampleFlow(node)));
    }

    public void _addflowv6(CommandInterpreter ci) throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            logger.error("",e);
        } catch (ConstructionException e) {
            logger.error("",e);
        }
        ci.println(this.addFlow(node, getSampleFlowV6(node)));
    }

    public void _removeflowv6(CommandInterpreter ci)
            throws UnknownHostException {
        Node node = null;
        String nodeId = ci.nextArgument();
        if (nodeId == null) {
            ci.print("Node id not specified");
            return;
        }
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            logger.error("",e);
        } catch (ConstructionException e) {
            logger.error("",e);
        }
        ci.println(this.removeFlow(node, getSampleFlowV6(node)));
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
        InetAddress newIP = InetAddress.getByName("200.200.100.1");
        InetAddress ipMask = InetAddress.getByName("255.255.255.0");
        InetAddress ipMask2 = InetAddress.getByName("255.240.0.0");
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
        actions.add(new SetNwDst(newIP));
        actions.add(new Output(oport));
        actions.add(new PopVlan());
        actions.add(new Flood());
        actions.add(new Controller());

        Flow flow = new Flow(match, actions);
        flow.setPriority((short) 100);
        flow.setHardTimeout((short) 360);

        return flow;
    }

    private Flow getSampleFlowV6(Node node) throws UnknownHostException {
        NodeConnector port = NodeConnectorCreator.createOFNodeConnector(
                (short) 24, node);
        NodeConnector oport = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        byte srcMac[] = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9a, (byte) 0xbc };
        byte dstMac[] = { (byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d,
                (byte) 0x5e, (byte) 0x6f };
        InetAddress srcIP = InetAddress
                .getByName("2001:420:281:1004:407a:57f4:4d15:c355");
        InetAddress dstIP = InetAddress
                .getByName("2001:420:281:1004:e123:e688:d655:a1b0");
        InetAddress ipMask = null; // InetAddress.getByName("ffff:ffff:ffff:ffff:0:0:0:0");
                                   // V6Match implementation assumes no mask is
                                   // specified
        InetAddress ipMask2 = null; // InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:0");
        short ethertype = EtherTypes.IPv6.shortValue();
        short vlan = (short) 27;
        byte vlanPr = (byte) 3;
        Byte tos = 4;
        byte proto = IPProtocols.UDP.byteValue();
        short src = (short) 5500;
        // short dst = 80;

        /*
         * Create a SAL Flow aFlow
         */
        Match match = new Match();
        match.setField(MatchType.IN_PORT, port);
        match.setField(MatchType.DL_SRC, srcMac);
        match.setField(MatchType.DL_DST, dstMac);
        match.setField(MatchType.DL_TYPE, ethertype);
        match.setField(MatchType.DL_VLAN, vlan);
        match.setField(MatchType.DL_VLAN_PR, vlanPr); // V6Match does not handle
                                                      // this properly...
        match.setField(MatchType.NW_SRC, srcIP, ipMask);
        match.setField(MatchType.NW_DST, dstIP, ipMask2);
        match.setField(MatchType.NW_TOS, tos);
        match.setField(MatchType.NW_PROTO, proto);
        match.setField(MatchType.TP_SRC, src); // V6Match does not handle this
                                               // properly...
        // match.setField(MatchType.TP_DST, dst); V6Match does not handle this
        // properly...

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(oport));
        actions.add(new PopVlan());
        actions.add(new Flood());

        Flow flow = new Flow(match, actions);
        flow.setPriority((short) 300);
        flow.setHardTimeout((short) 240);

        return flow;
    }

    /**
     * This Request ID generator starts with 1. Each aysnc message is
     * associated with an unique Request ID (!= 0).
     *
     * @return Request ID
     */
    private long getNextRid() {
        return seq.getAndIncrement();
    }

    @Override
    public Status syncSendBarrierMessage(Node node) {
        if (this.pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType())
                        .syncSendBarrierMessage(node);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }

    @Override
    public Status asyncSendBarrierMessage(Node node) {
        if (this.pluginFlowProgrammer != null) {
            if (this.pluginFlowProgrammer.get(node.getType()) != null) {
                return this.pluginFlowProgrammer.get(node.getType())
                        .asyncSendBarrierMessage(node);
            }
        }
        return new Status(StatusCode.NOSERVICE, "Plugin unuvailable");
    }
}
