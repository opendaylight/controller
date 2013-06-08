/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.openflow.protocol.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class which provides the utilities for converting the Openflow port
 * number to the equivalent NodeConnector and vice versa
 * 
 * 
 * 
 */
public abstract class PortConverter {
    private static final Logger log = LoggerFactory
            .getLogger(PortConverter.class);
    private static final int maxOFPhysicalPort = NetUtils
            .getUnsignedShort(OFPort.OFPP_MAX.getValue());

    /**
     * Converts the Openflow port number to the equivalent NodeConnector.
     */
    public static NodeConnector toNodeConnector(short ofPort, Node node) {
        // Restore original OF unsigned 16 bits value for the comparison
        int unsignedOFPort = NetUtils.getUnsignedShort(ofPort);
        log.trace("Openflow port number signed: {} unsigned: {}", ofPort,
                unsignedOFPort);
        if (unsignedOFPort > maxOFPhysicalPort) {
            if (ofPort == OFPort.OFPP_LOCAL.getValue()) {
                return NodeConnectorCreator.createNodeConnector(
                        NodeConnectorIDType.SWSTACK,
                        NodeConnector.SPECIALNODECONNECTORID, node);
            } else if (ofPort == OFPort.OFPP_NORMAL.getValue()) {
                return NodeConnectorCreator.createNodeConnector(
                        NodeConnectorIDType.HWPATH,
                        NodeConnector.SPECIALNODECONNECTORID, node);
            } else if (ofPort == OFPort.OFPP_CONTROLLER.getValue()) {
                return NodeConnectorCreator.createNodeConnector(
                        NodeConnectorIDType.CONTROLLER,
                        NodeConnector.SPECIALNODECONNECTORID, node);
            }
        }
        return NodeConnectorCreator.createNodeConnector(ofPort, node);
    }

    /**
     * Converts the NodeConnector to the equivalent Openflow port number
     */
    public static short toOFPort(NodeConnector salPort) {
        log.trace("SAL Port", salPort);
        if (salPort.getType().equals(NodeConnectorIDType.SWSTACK)) {
            return OFPort.OFPP_LOCAL.getValue();
        } else if (salPort.getType().equals(NodeConnectorIDType.HWPATH)) {
            return OFPort.OFPP_NORMAL.getValue();
        } else if (salPort.getType().equals(NodeConnectorIDType.CONTROLLER)) {
            return OFPort.OFPP_CONTROLLER.getValue();
        }
        return (Short) salPort.getID();
    }
}
