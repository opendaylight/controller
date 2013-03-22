
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.northbound;

import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;

/**
 * The class describes set of properties attached to a node connector
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NodeConnectorProperties {
    @XmlElement
    private NodeConnector nodeconnector;
    @XmlElementRef
    @XmlElementWrapper
    private Set<Property> properties;

    // JAXB required constructor
    private NodeConnectorProperties() {
        this.nodeconnector = null;
        this.properties = null;
    }

    public NodeConnectorProperties(NodeConnector nodeconnector, Set<Property> properties) {
        this.nodeconnector = nodeconnector;
        this.properties = properties;
    }

    public Set<Property> getProperties() {
        return properties;
    }

    public void setProperties(Set<Property> properties) {
        this.properties = properties;
    }

    public NodeConnector getNodeConnector() {
        return nodeconnector;
    }

    public void setNodeConnector(NodeConnector nodeconnector) {
        this.nodeconnector = nodeconnector;
    }
}
