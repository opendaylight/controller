package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Description other = (Description) obj;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Description[" + description + "]";
    }
}
