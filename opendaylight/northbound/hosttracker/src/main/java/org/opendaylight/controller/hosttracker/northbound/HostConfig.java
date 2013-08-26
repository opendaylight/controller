/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.northbound;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Configuration Java Object which represents a Host configuration information
 * for HostTracker.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class HostConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement
    public String dataLayerAddress;
    @XmlElement
    private String nodeType;
    @XmlElement
    private String nodeId;
    @XmlElement
    private String nodeConnectorType;
    @XmlElement
    private String nodeConnectorId;
    @XmlElement
    private String vlan;

    public HostConfig() {

    }

    protected String getDataLayerAddress() {
        return this.dataLayerAddress;
    }

    protected String getNodeType() {
        return this.nodeType;
    }

    protected String getNodeId() {
        return this.nodeId;
    }

    protected String getNodeConnectorType() {
        return this.nodeConnectorType;
    }

    protected String getNodeConnectorId() {
        return this.nodeConnectorId;
    }

    protected String getVlan() {
        return this.vlan;
    }
}
