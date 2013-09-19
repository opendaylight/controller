
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.northbound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
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
    @JsonIgnore
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

    @JsonProperty(value="properties")
    public Map<String, Property> getMapProperties() {
        Map<String, Property> map = new HashMap<String, Property>();
        for (Property p : properties) {
            map.put(p.getName(), p);
        }
        return map;
    }

    public void setMapProperties(Map<String, Property> propertiesMap) {
        this.properties = new HashSet<Property>(propertiesMap.values());
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
