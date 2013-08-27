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

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.address.DataLinkAddress;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;

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
    @XmlElement
    private boolean staticHost;
    @XmlElement
    private String networkAddress;

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

    protected boolean isStaticHost() {
        return staticHost;
    }

    protected String getNetworkAddress() {
        return networkAddress;
    }

    public static HostConfig convert(HostNodeConnector hnc) {
        if(hnc == null) {
            return null;
        }
        HostConfig hc = new HostConfig();
        DataLinkAddress dl = hnc.getDataLayerAddress();
        if(dl instanceof EthernetAddress) {
            EthernetAddress et = (EthernetAddress) dl;
            hc.dataLayerAddress = et.getMacAddress();
        } else {
            hc.dataLayerAddress = dl.getName();
        }
        NodeConnector nc = hnc.getnodeConnector();
        if(nc != null) {
            hc.nodeConnectorType = nc.getType();
            hc.nodeConnectorId = nc.getNodeConnectorIDString();
            Node n = hnc.getnodeconnectorNode();
            if(n != null) {
                hc.nodeType = n.getType();
                hc.nodeId = n.getNodeIDString();
            }
        }
        hc.vlan = String.valueOf(hnc.getVlan());
        hc.staticHost = hnc.isStaticHost();
        hc.networkAddress = hnc.getNetworkAddressAsString();
        return hc;
    }
}
