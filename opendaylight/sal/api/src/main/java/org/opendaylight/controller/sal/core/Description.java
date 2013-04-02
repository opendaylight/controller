package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The class represents the Name property of an element.
 */
@XmlRootElement
@SuppressWarnings("serial")
public class Description extends Property {
    @XmlElement
    private String description;
    public static final String propertyName = "description";

    /*
     * Private constructor used for JAXB mapping
     */
    private Description() {
        super(propertyName);
        this.description = null;
    }

    public Description(String description) {
        super(propertyName);
        this.description = description;
    }

    public Description clone() {
        return new Description(this.description);
    }

    public String getValue() {
        return this.description;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "Description[" + description + "]";
    }
}
