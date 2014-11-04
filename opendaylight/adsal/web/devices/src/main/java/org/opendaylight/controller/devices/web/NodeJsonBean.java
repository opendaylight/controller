/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.devices.web;

import java.util.List;

public class NodeJsonBean {
    String nodeId;
    String nodeName;
    String nodeType;
    List<PortJsonBean> nodePorts;

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public List<PortJsonBean> getNodePorts() {
        return nodePorts;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public void setNodePorts(List<PortJsonBean> port) {
        this.nodePorts = port;
    }

}
