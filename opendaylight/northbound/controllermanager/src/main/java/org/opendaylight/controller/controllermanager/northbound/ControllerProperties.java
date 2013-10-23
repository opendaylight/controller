package org.opendaylight.controller.controllermanager.northbound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.opendaylight.controller.sal.core.Property;

/**
 * The class describes set of properties attached to a controller
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class ControllerProperties {

    @XmlElementRef
    @XmlElementWrapper
    @JsonIgnore
    /**
     * Set to store the controller properties
     */
    private Set<Property> properties;

    // JAXB required constructor
    private ControllerProperties() {
        this.properties = null;
    }

    public ControllerProperties(Set<Property> properties) {
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
}
