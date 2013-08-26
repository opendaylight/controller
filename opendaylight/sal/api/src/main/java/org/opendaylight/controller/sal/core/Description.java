package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The class represents the Name property of an element.
 */
@XmlRootElement
@SuppressWarnings("serial")
public class Description extends Property {
    @XmlElement(name="value")
    private String descriptionValue;
    public static final String propertyName = "description";

    /*
     * Private constructor used for JAXB mapping
     */
    private Description() {
        super(propertyName);
        this.descriptionValue = null;
    }

    public Description(String description) {
        super(propertyName);
        this.descriptionValue = description;
    }

    @Override
    public Description clone() {
        return new Description(this.descriptionValue);
    }

    public String getValue() {
        return this.descriptionValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((descriptionValue == null) ? 0 : descriptionValue.hashCode());
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
        if (descriptionValue == null) {
            if (other.descriptionValue != null)
                return false;
        } else if (!descriptionValue.equals(other.descriptionValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Description[" + descriptionValue + "]";
    }
}
