
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPort;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Represents the openflow plugin component in charge of programming the flows
 * on the switch. It servers the install requests coming from the SAL layer.
 *
 *
 *
 */
public class FlowProgrammerService implements IPluginInFlowProgrammerService {
    private IController controller;

    public FlowProgrammerService() {
        controller = null;
    }

    public void setController(IController core) {
        this.controller = core;
    }

    public void unsetController(IController core) {
        if (this.controller == core) {
            this.controller = null;
        }
    }

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
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    @Override
    public Status addFlow(Node node, Flow flow) {
        String action = "add";
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE,
                    errorString("send", action, "Invalid node type"));
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
                    return ((Boolean) result == Boolean.TRUE) ?
                            new Status(StatusCode.SUCCESS, null)
                            : new Status(StatusCode.TIMEOUT,
                                    errorString(null, action,
                                            "Request Timed Out"));
                } else if (result instanceof OFError) {
                    return new Status(StatusCode.INTERNALERROR,
                            errorString("program", action, Utils
                            .getOFErrorString((OFError) result)));
                } else {
                    return new Status(StatusCode.INTERNALERROR,
                            errorString("send", action, "Internal Error"));
                }
            } else {
                return new Status(StatusCode.GONE, errorString("send", action,
                                "Switch is not available"));
            }
        }
        return new Status(StatusCode.INTERNALERROR,
                errorString("send", action, "Internal plugin error"));
    }

    @Override
    public Status modifyFlow(Node node, Flow oldFlow, Flow newFlow) {
        String action = "modify";
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE,
                    errorString("send", action, "Invalid node type"));
        }
        if (controller != null) {
            ISwitch sw = controller.getSwitch((Long) node.getID());
            if (sw != null) {
                OFMessage msg1 = null, msg2 = null;

                // If priority and match portion are the same, send a modification message
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
                        return new Status(StatusCode.TIMEOUT,
                                errorString(null, action,
                                        "Request Timed Out"));
                    } else if (msg2 == null) {
                        return new Status(StatusCode.SUCCESS, null);
                    }
                } else if (result instanceof OFError) {
                    return new Status(StatusCode.INTERNALERROR,
                            errorString("program", action, Utils
                            .getOFErrorString((OFError) result)));
                } else {
                    return new Status(StatusCode.INTERNALERROR,
                            errorString("send", action, "Internal Error"));
                }

                if (msg2 != null) {
                    action = "add";
                    result = sw.syncSend(msg2);
                    if (result instanceof Boolean) {
                        return ((Boolean) result == Boolean.TRUE) ?
                                new Status(StatusCode.SUCCESS, null)
                                : new Status(StatusCode.TIMEOUT,
                                        errorString(null, action,
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
        return new Status(StatusCode.INTERNALERROR,
                errorString("send", action, "Internal plugin error"));
    }

    @Override
    public Status removeFlow(Node node, Flow flow) {
        String action = "remove";
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            return new Status(StatusCode.NOTACCEPTABLE,
                    errorString("send", action, "Invalid node type"));
        }
        if (controller != null) {
            ISwitch sw = controller.getSwitch((Long) node.getID());
            if (sw != null) {
                OFMessage msg = new FlowConverter(flow).getOFFlowMod(
                        OFFlowMod.OFPFC_DELETE_STRICT, OFPort.OFPP_NONE);
                Object result = sw.syncSend(msg);
                if (result instanceof Boolean) {
                    return ((Boolean) result == Boolean.TRUE) ?
                            new Status(StatusCode.SUCCESS, null)
                            : new Status(StatusCode.TIMEOUT,
                                    errorString(null, action,
                                            "Request Timed Out"));
                } else if (result instanceof OFError) {
                    return new Status(StatusCode.INTERNALERROR,
                            errorString("program", action, Utils
                            .getOFErrorString((OFError) result)));
                } else {
                    return new Status(StatusCode.INTERNALERROR,
                            errorString("send", action, "Internal Error"));
                }
            } else {
                return new Status(StatusCode.GONE,  errorString("send", action,
                        "Switch is not available"));
            }
        }
        return new Status(StatusCode.INTERNALERROR,
                errorString("send", action, "Internal plugin error"));
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

}
