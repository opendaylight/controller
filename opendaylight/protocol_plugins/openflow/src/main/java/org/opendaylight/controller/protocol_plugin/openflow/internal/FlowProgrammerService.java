/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.protocol_plugin.openflow.IFlowProgrammerNotifier;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6Error;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;

import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class FlowProgrammerService implements IPluginInFlowProgrammerService,
        IMessageListener, IContainerListener {
    private static final Logger log = LoggerFactory
            .getLogger(FlowProgrammerService.class);
    private IController controller;
    private ConcurrentMap<String, IFlowProgrammerNotifier> flowProgrammerNotifiers;
    private Map<String, Set<NodeConnector>> containerToNc;

    public FlowProgrammerService() {
        controller = null;
        flowProgrammerNotifiers = new ConcurrentHashMap<String, IFlowProgrammerNotifier>();
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

                /*
                 * Synchronous message send
                 */
                Object result = sw.syncSend(msg);
                if (result instanceof Boolean) {
                    return ((Boolean) result == Boolean.TRUE) ? new Status(
                            StatusCode.SUCCESS, null) : new Status(
                            StatusCode.TIMEOUT, errorString(null, action,
                                    "Request Timed Out"));
                } else if (result instanceof OFError) {
                    OFError res = (OFError) result;
                    if (res.getErrorType() == V6Error.NICIRA_VENDOR_ERRORTYPE) {
                        V6Error er = new V6Error(res);
                        byte[] b = res.getError();
                        ByteBuffer bb = ByteBuffer.allocate(b.length);
                        bb.put(b);
                        bb.rewind();
                        er.readFrom(bb);
                        return new Status(StatusCode.INTERNALERROR,
                                errorString("program", action,
                                        "Vendor Extension Internal Error"));
                    }
                    return new Status(StatusCode.INTERNALERROR, errorString(
                            "program", action, Utils.getOFErrorString(res)));
                } else {
                    return new Status(StatusCode.INTERNALERROR, errorString(
                            "send", action, "Internal Error"));
                }
            } else {
                return new Status(StatusCode.GONE, errorString("send", action,
                        "Switch is not available"));
            }
        }
        return new Status(StatusCode.INTERNALERROR, errorString("send", action,
                "Internal plugin error"));
    }

    @Override
    public Status modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
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
                Object result = sw.syncSend(msg1);
                if (result instanceof Boolean) {
                    if ((Boolean) result == Boolean.FALSE) {
                        return new Status(StatusCode.TIMEOUT, errorString(null,
                                action, "Request Timed Out"));
                    } else if (msg2 == null) {
                        return new Status(StatusCode.SUCCESS, null);
                    }
                } else if (result instanceof OFError) {
                    return new Status(StatusCode.INTERNALERROR, errorString(
                            "program", action,
                            Utils.getOFErrorString((OFError) result)));
                } else {
                    return new Status(StatusCode.INTERNALERROR, errorString(
                            "send", action, "Internal Error"));
                }

                if (msg2 != null) {
                    action = "add";
                    result = sw.syncSend(msg2);
                    if (result instanceof Boolean) {
                        return ((Boolean) result == Boolean.TRUE) ? new Status(
                                StatusCode.SUCCESS, null) : new Status(
                                StatusCode.TIMEOUT, errorString(null, action,
                                        "Request Timed Out"));
                    } else if (result instanceof OFError) {
                        return new Status(StatusCode.INTERNALERROR,
                                errorString("program", action, Utils
                                        .getOFErrorString((OFError) result)));
                    } else {
                        return new Status(StatusCode.INTERNALERROR,
                                errorString("send", action, "Internal Error"));
                    }
                }
            } else {
                return new Status(StatusCode.GONE, errorString("send", action,
                        "Switch is not available"));
            }
        }
        return new Status(StatusCode.INTERNALERROR, errorString("send", action,
                "Internal plugin error"));
    }

    @Override
    public Status removeFlow(Node node, Flow flow) {
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
                Object result = sw.syncSend(msg);
                if (result instanceof Boolean) {
                    return ((Boolean) result == Boolean.TRUE) ? new Status(
                            StatusCode.SUCCESS, null) : new Status(
                            StatusCode.TIMEOUT, errorString(null, action,
                                    "Request Timed Out"));
                } else if (result instanceof OFError) {
                    return new Status(StatusCode.INTERNALERROR, errorString(
                            "program", action,
                            Utils.getOFErrorString((OFError) result)));
                } else {
                    return new Status(StatusCode.INTERNALERROR, errorString(
                            "send", action, "Internal Error"));
                }
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
        return new Status(StatusCode.SUCCESS, null);
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
}
