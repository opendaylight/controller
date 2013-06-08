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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.protocol_plugin.openflow.IFlowProgrammerNotifier;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimExternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class FlowProgrammerService implements IPluginInFlowProgrammerService,
        IMessageListener, IContainerListener, IInventoryShimExternalListener,
        CommandProvider {
    private static final Logger log = LoggerFactory
            .getLogger(FlowProgrammerService.class);
    private IController controller;
    private ConcurrentMap<String, IFlowProgrammerNotifier> flowProgrammerNotifiers;
    private Map<String, Set<NodeConnector>> containerToNc;
    private ConcurrentMap<Long, Map<Integer, Long>> xid2rid;
    private int barrierMessagePriorCount = getBarrierMessagePriorCount();

    public FlowProgrammerService() {
        controller = null;
        flowProgrammerNotifiers = new ConcurrentHashMap<String, IFlowProgrammerNotifier>();
        containerToNc = new HashMap<String, Set<NodeConnector>>();
        xid2rid = new ConcurrentHashMap<Long, Map<Integer, Long>>();
    }

    public void setController(IController core) {
        this.controller = core;
    }

    public void unsetController(IController core) {
        if (this.controller == core) {
            this.controller = null;
        }
    }

    public void setFlowProgrammerNotifier(Map<String, ?> props,
            IFlowProgrammerNotifier s) {
        if (props == null || props.get("containerName") == null) {
            log.error("Didn't receive the service correct properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        this.flowProgrammerNotifiers.put(containerName, s);
    }

    public void unsetFlowProgrammerNotifier(Map<String, ?> props,
            IFlowProgrammerNotifier s) {
        if (props == null || props.get("containerName") == null) {
            log.error("Didn't receive the service correct properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (this.flowProgrammerNotifiers != null
                && this.flowProgrammerNotifiers.containsKey(containerName)
                && this.flowProgrammerNotifiers.get(containerName) == s) {
            this.flowProgrammerNotifiers.remove(containerName);
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     * 
     */
    void init() {
        this.controller.addMessageListener(OFType.FLOW_REMOVED, this);
        this.controller.addMessageListener(OFType.ERROR, this);
        registerWithOSGIConsole();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     * 
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     * 
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     * 
     */
    void stop() {
    }

    @Override
    public Status addFlow(Node node, Flow flow) {
        return addFlowInternal(node, flow, 0);
    }

    @Override
    public Status modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        return modifyFlowInternal(node, oldFlow, newFlow, 0);
    }

    @Override
    public Status removeFlow(Node node, Flow flow) {
        return removeFlowInternal(node, flow, 0);
    }

    @Override
    public Status addFlowAsync(Node node, Flow flow, long rid) {
        return addFlowInternal(node, flow, rid);
    }

    @Override
    public Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow,
            long rid) {
        return modifyFlowInternal(node, oldFlow, newFlow, rid);
    }

    @Override
    public Status removeFlowAsync(Node node, Flow flow, long rid) {
        return removeFlowInternal(node, flow, rid);
    }

    private Status addFlowInternal(Node node, Flow flow, long rid) {
        String action = "add";
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE, errorString("send",
                    action, "Invalid node type"));
        }

        if (controller != null) {
            ISwitch sw = controller.getSwitch((Long) node.getID());
            if (sw != null) {
                FlowConverter x = new FlowConverter(flow);
                OFMessage msg = x.getOFFlowMod(OFFlowMod.OFPFC_ADD, null);

                Object result;
                if (rid == 0) {
                    /*
                     * Synchronous message send. Each message is followed by a
                     * Barrier message.
                     */
                    result = sw.syncSend(msg);
                } else {
                    /*
                     * Message will be sent asynchronously. A Barrier message
                     * will be inserted automatically to synchronize the
                     * progression.
                     */
                    result = asyncMsgSend(node, sw, msg, rid);  
                }
                return getStatusInternal(result, action, rid);
            } else {
                return new Status(StatusCode.GONE, errorString("send", action,
                        "Switch is not available"));
            }
        }
        return new Status(StatusCode.INTERNALERROR, errorString("send", action,
                "Internal plugin error"));
    }

    private Status modifyFlowInternal(Node node, Flow oldFlow, Flow newFlow, long rid) {
        String action = "modify";
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE, errorString("send",
                    action, "Invalid node type"));
        }
        if (controller != null) {
            ISwitch sw = controller.getSwitch((Long) node.getID());
            if (sw != null) {
                OFMessage msg1 = null, msg2 = null;

                // If priority and match portion are the same, send a
                // modification message
                if (oldFlow.getPriority() != newFlow.getPriority()
                        || !oldFlow.getMatch().equals(newFlow.getMatch())) {
                    msg1 = new FlowConverter(oldFlow).getOFFlowMod(
                            OFFlowMod.OFPFC_DELETE_STRICT, OFPort.OFPP_NONE);
                    msg2 = new FlowConverter(newFlow).getOFFlowMod(
                            OFFlowMod.OFPFC_ADD, null);
                } else {
                    msg1 = new FlowConverter(newFlow).getOFFlowMod(
                            OFFlowMod.OFPFC_MODIFY_STRICT, null);
                }
                /*
                 * Synchronous message send
                 */
                action = (msg2 == null) ? "modify" : "delete";
                Object result;
                if (rid == 0) {
                    /*
                     * Synchronous message send. Each message is followed by a
                     * Barrier message.
                     */
                    result = sw.syncSend(msg1);
                } else {
                    /*
                     * Message will be sent asynchronously. A Barrier message
                     * will be inserted automatically to synchronize the
                     * progression.
                     */
                    result = asyncMsgSend(node, sw, msg1, rid);
                }

                Status rv = getStatusInternal(result, action, rid);
                if ((msg2 == null) || !rv.isSuccess()) {
                    return rv;
                }

                action = "add";
                if (rid == 0) {
                    /*
                     * Synchronous message send. Each message is followed by a
                     * Barrier message.
                     */
                    result = sw.syncSend(msg2);
                } else {
                    /*
                     * Message will be sent asynchronously. A Barrier message
                     * will be inserted automatically to synchronize the
                     * progression.
                     */
                    result = asyncMsgSend(node, sw, msg2, rid);
                }
                return getStatusInternal(result, action, rid);
            } else {
                return new Status(StatusCode.GONE, errorString("send", action,
                        "Switch is not available"));
            }
        }
        return new Status(StatusCode.INTERNALERROR, errorString("send", action,
                "Internal plugin error"));
    }

    private Status removeFlowInternal(Node node, Flow flow, long rid) {
        String action = "remove";
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE, errorString("send",
                    action, "Invalid node type"));
        }
        if (controller != null) {
            ISwitch sw = controller.getSwitch((Long) node.getID());
            if (sw != null) {
                OFMessage msg = new FlowConverter(flow).getOFFlowMod(
                        OFFlowMod.OFPFC_DELETE_STRICT, OFPort.OFPP_NONE);
                Object result;
                if (rid == 0) {
                    /*
                     * Synchronous message send. Each message is followed by a
                     * Barrier message.
                     */
                    result = sw.syncSend(msg);
                } else {
                    /*
                     * Message will be sent asynchronously. A Barrier message
                     * will be inserted automatically to synchronize the
                     * progression.
                     */
                    result = asyncMsgSend(node, sw, msg, rid);
                }
                return getStatusInternal(result, action, rid);
            } else {
                return new Status(StatusCode.GONE, errorString("send", action,
                        "Switch is not available"));
            }
        }
        return new Status(StatusCode.INTERNALERROR, errorString("send", action,
                "Internal plugin error"));
    }

    @Override
    public Status removeAllFlows(Node node) {
        return new Status(StatusCode.SUCCESS);
    }

    private String errorString(String phase, String action, String cause) {
        return "Failed to "
                + ((phase != null) ? phase + " the " + action
                        + " flow message: " : action + " the flow: ") + cause;
    }

    @Override
    public void receive(ISwitch sw, OFMessage msg) {
        if (msg instanceof OFFlowRemoved) {
            handleFlowRemovedMessage(sw, (OFFlowRemoved) msg);
        } else if (msg instanceof OFError) {
            handleErrorMessage(sw, (OFError) msg);
        }
    }

    private void handleFlowRemovedMessage(ISwitch sw, OFFlowRemoved msg) {
        Node node = NodeCreator.createOFNode(sw.getId());
        Flow flow = new FlowConverter(msg.getMatch(),
                new ArrayList<OFAction>(0)).getFlow(node);
        flow.setPriority(msg.getPriority());
        flow.setIdleTimeout(msg.getIdleTimeout());
        flow.setId(msg.getCookie());

        Match match = flow.getMatch();
        NodeConnector inPort = match.isPresent(MatchType.IN_PORT) ? (NodeConnector) match
                .getField(MatchType.IN_PORT).getValue() : null;

        for (Map.Entry<String, IFlowProgrammerNotifier> containerNotifier : flowProgrammerNotifiers
                .entrySet()) {
            String container = containerNotifier.getKey();
            IFlowProgrammerNotifier notifier = containerNotifier.getValue();
            /*
             * Switch only provide us with the match information. For now let's
             * try to identify the container membership only from the input port
             * match field. In any case, upper layer consumers can derive
             * whether the notification was not for them. More sophisticated
             * filtering can be added later on.
             */
            if (inPort == null
                    || container.equals(GlobalConstants.DEFAULT.toString())
                    || this.containerToNc.get(container).contains(inPort)) {
                notifier.flowRemoved(node, flow);
            }
        }
    }

    private void handleErrorMessage(ISwitch sw, OFError errorMsg) {
        Node node = NodeCreator.createOFNode(sw.getId());
        OFMessage offendingMsg = errorMsg.getOffendingMsg();
        Integer xid;
        if (offendingMsg != null) {
            xid = offendingMsg.getXid();
        } else {
            xid = errorMsg.getXid();
        }

        Long rid = getMessageRid(sw.getId(), xid);
        /*
         * Null or zero requestId indicates that the error message is meant for
         * a sync message. It will be handled by the sync message worker thread.
         * Hence we are done here.
         */
        if ((rid == null) || (rid == 0)) {
            return;
        }
        
        /*
         * Notifies the caller that error has been reported for a previous flow
         * programming request
         */
        for (Map.Entry<String, IFlowProgrammerNotifier> containerNotifier : flowProgrammerNotifiers
                .entrySet()) {
            IFlowProgrammerNotifier notifier = containerNotifier.getValue();
            notifier.flowErrorReported(node, rid, errorMsg);
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
            UpdateType type) {
        Set<NodeConnector> target = null;

        switch (type) {
        case ADDED:
            if (!containerToNc.containsKey(containerName)) {
                containerToNc.put(containerName, new HashSet<NodeConnector>());
            }
            containerToNc.get(containerName).add(p);
            break;
        case CHANGED:
            break;
        case REMOVED:
            target = containerToNc.get(containerName);
            if (target != null) {
                target.remove(p);
            }
            break;
        default:
        }
    }

    @Override
    public void containerModeUpdated(UpdateType t) {

    }

    @Override
    public Status syncSendBarrierMessage(Node node) {
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE,
                    "The node does not support Barrier message.");
        }

        if (controller != null) {
            long swid = (Long) node.getID();
            ISwitch sw = controller.getSwitch(swid);
            if (sw != null) {
                sw.syncSendBarrierMessage();
                clearXid2Rid(swid);
                return (new Status(StatusCode.SUCCESS));
            } else {
                return new Status(StatusCode.GONE,
                        "The node does not have a valid Switch reference.");
            }
        }
        return new Status(StatusCode.INTERNALERROR,
                "Failed to send Barrier message.");
    }
    
    @Override
    public Status asyncSendBarrierMessage(Node node) {
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE,
                    "The node does not support Barrier message.");
        }

        if (controller != null) {
            long swid = (Long) node.getID();
            ISwitch sw = controller.getSwitch(swid);
            if (sw != null) {
                sw.asyncSendBarrierMessage();
                clearXid2Rid(swid);
                return (new Status(StatusCode.SUCCESS));
            } else {
                return new Status(StatusCode.GONE,
                        "The node does not have a valid Switch reference.");
            }
        }
        return new Status(StatusCode.INTERNALERROR,
                "Failed to send Barrier message.");
    }
    
    /**
     * This method sends the message asynchronously until the number of messages
     * sent reaches a threshold. Then a Barrier message is sent automatically
     * for sync purpose. An unique Request ID associated with the message is
     * passed down by the caller. The Request ID will be returned to the caller
     * when an error message is received from the switch.
     * 
     * @param node
     *            The node
     * @param msg
     *            The switch
     * @param msg
     *            The OF message to be sent
     * @param rid
     *            The Request Id
     * @return result
     */
    private Object asyncMsgSend(Node node, ISwitch sw, OFMessage msg, long rid) {
        Object result = Boolean.TRUE;
        long swid = (Long) node.getID();
        int xid;

        xid = sw.asyncSend(msg);
        addXid2Rid(swid, xid, rid);
        
        Map<Integer, Long> swxid2rid = this.xid2rid.get(swid);
        if (swxid2rid == null) {
            return result;
        }
        
        int size = swxid2rid.size();
        if (size % barrierMessagePriorCount == 0) {
            result = asyncSendBarrierMessage(node);
        }
        
        return result;
    }
    
    /**
     * A number of async messages are sent followed by a synchronous Barrier
     * message. This method returns the maximum async messages that can be sent
     * before the Barrier message.
     * 
     * @return The max count of async messages sent prior to Barrier message
     */
    private int getBarrierMessagePriorCount() {
        String count = System.getProperty("of.barrierMessagePriorCount");
        int rv = 100;

        if (count != null) {
            try {
                rv = Integer.parseInt(count);
            } catch (Exception e) {
            }
        }

        return rv;
    }
    
    /**
     * This method returns the message Request ID previously assigned by the
     * caller for a given OF message xid
     * 
     * @param swid
     *            The switch id
     * @param xid
     *            The OF message xid
     * @return The Request ID
     */
    private Long getMessageRid(long swid, Integer xid) {
        Long rid = null;

        if (xid == null) {
            return rid;
        }

        Map<Integer, Long> swxid2rid = this.xid2rid.get(swid);
        if (swxid2rid != null) {
            rid = swxid2rid.get(xid);
        }
        return rid;
    }

    /**
     * This method returns a copy of outstanding xid to rid mappings.for a given
     * switch
     * 
     * @param swid
     *            The switch id
     * @return a copy of xid2rid mappings
     */
    public Map<Integer, Long> getSwXid2Rid(long swid) {
        Map<Integer, Long> swxid2rid = this.xid2rid.get(swid);
        
        if (swxid2rid != null) {
            return new HashMap<Integer, Long>(swxid2rid);
        } else {
            return new HashMap<Integer, Long>();
        }
    }

    /**
     * Adds xid to rid mapping to the local DB
     * 
     * @param swid
     *            The switch id
     * @param xid
     *            The OF message xid
     * @param rid
     *            The message Request ID
     */
    private void addXid2Rid(long swid, int xid, long rid) {
        Map<Integer, Long> swxid2rid = this.xid2rid.get(swid);
        if (swxid2rid != null) {
            swxid2rid.put(xid, rid);
        }
    }

    /**
     * When an Error message is received, this method will be invoked to remove
     * the offending xid from the local DB.
     * 
     * @param swid
     *            The switch id
     * @param xid
     *            The OF message xid
     */
    private void removeXid2Rid(long swid, int xid) {
        Map<Integer, Long> swxid2rid = this.xid2rid.get(swid);
        if (swxid2rid != null) {
            swxid2rid.remove(xid);
        }
    }

    /**
     * Convert various result into Status
     * 
     * @param result
     *            The returned result from previous action
     * @param action
     *            add/modify/delete flow action
     * @param rid
     *            The Request ID associated with the flow message
     * @return Status
     */
    private Status getStatusInternal(Object result, String action, long rid) {
        if (result instanceof Boolean) {
            return ((Boolean) result == Boolean.TRUE) ? new Status(
                    StatusCode.SUCCESS, rid) : new Status(
                    StatusCode.TIMEOUT, errorString(null, action,
                            "Request Timed Out"));
        } else if (result instanceof Status) {
            return (Status) result;
        } else if (result instanceof OFError) {
            OFError res = (OFError) result;
            return new Status(StatusCode.INTERNALERROR, errorString(
                    "program", action, Utils.getOFErrorString(res)));
        } else {
            return new Status(StatusCode.INTERNALERROR, errorString(
                    "send", action, "Internal Error"));
        }
    }
    
    /**
     * When a Barrier reply is received, this method will be invoked to clear
     * the local DB
     * 
     * @param swid
     *            The switch id
     */
    private void clearXid2Rid(long swid) {
        Map<Integer, Long> swxid2rid = this.xid2rid.get(swid);
        if (swxid2rid != null) {
            swxid2rid.clear();
        }
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        long swid = (Long)node.getID();
        
        switch (type) {
        case ADDED:
            Map<Integer, Long> swxid2rid = new HashMap<Integer, Long>();
            this.xid2rid.put(swid, swxid2rid);
            break;
        case CHANGED:
            break;
        case REMOVED:
            this.xid2rid.remove(swid);
            break;
        default:
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
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
        help.append("-- Flow Programmer Service --\n");
        help.append("\t px2r <node id>          - Print outstanding xid2rid mappings for a given node id\n");
        help.append("\t px2rc                   - Print max num of async msgs prior to the Barrier\n");
        return help.toString();
    }

    public void _px2r(CommandInterpreter ci) {
        String st = ci.nextArgument();
        if (st == null) {
            ci.println("Please enter a valid node id");
            return;
        }
        
        long sid;
        try {
            sid = HexEncode.stringToLong(st);
        } catch (NumberFormatException e) {
            ci.println("Please enter a valid node id");
            return;
        }
        
        Map<Integer, Long> swxid2rid = this.xid2rid.get(sid);
        if (swxid2rid == null) {
            ci.println("The node id entered does not exist");
            return;
        }

        ci.println("xid             rid");
        
        Set<Integer> xidSet = swxid2rid.keySet();
        if (xidSet == null) {
            return;
        }

        for (Integer xid : xidSet) {
            ci.println(xid + "       " + swxid2rid.get(xid));
        }
    }

    public void _px2rc(CommandInterpreter ci) {
        ci.println("Max num of async messages sent prior to the Barrier message is "
                + barrierMessagePriorCount);
    }
}
