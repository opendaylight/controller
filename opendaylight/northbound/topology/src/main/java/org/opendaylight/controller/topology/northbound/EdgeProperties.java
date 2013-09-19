
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topology.northbound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class EdgeProperties {
    @XmlElement
    private Edge edge;
    @XmlElementRef
    @XmlElementWrapper
    @JsonIgnore
    private Set<Property> properties;

    // JAXB required constructor
    private EdgeProperties() {
        this.edge = null;
        this.properties = null;
    }

    public EdgeProperties(Edge e, Set<Property> properties) {
        this.edge = e;
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

    public Edge getEdge() {
        return edge;
    }

    public void setEdge(Edge edge) {
        this.edge = edge;
    }
}
