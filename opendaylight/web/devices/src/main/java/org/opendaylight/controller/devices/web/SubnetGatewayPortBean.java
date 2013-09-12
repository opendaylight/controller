/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.devices.web;

public class SubnetGatewayPortBean {
    private String nodeName;
    private String nodeId;
    private PortJsonBean port;
    private String nodePortId;
    private String nodePortName;

    public String getNodeName() {
        return nodeName;
    }
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
    public String getNodeId() {
        return nodeId;
    }
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    public String getNodePortId() {
        return nodePortId;
    }
    public void setNodePortId(String nodePortId) {
        this.nodePortId = nodePortId;
    }
    public String getNodePortName() {
        return nodePortName;
    }
    public void setNodePortName(String nodePortName) {
        this.nodePortName = nodePortName;
    }
}
