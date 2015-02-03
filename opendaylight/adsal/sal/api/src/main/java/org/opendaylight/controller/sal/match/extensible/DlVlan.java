package org.opendaylight.controller.sal.match.extensible;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class DlVlan extends MatchField<Short> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_VLAN";
    private static final short MAX = 4095;
    private short vlan;

    /**
     * Creates a Match field for the data layer type
     *
     * @param address
     *            the data layer type
     */
    public DlVlan(short vlan) {
        super(TYPE);
        this.vlan = vlan;
    }

    // To satisfy JAXB
    private DlVlan() {
        super(TYPE);
    }

    @Override
    public Short getValue() {
        return vlan;
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return String.valueOf(vlan);
    }

    @Override
    public Short getMask() {
        return null;
    }

    @Override
    protected String getMaskString() {
        return null;
    }

    @Override
    public boolean isValid() {
        return vlan >= 0 && vlan <= MAX;
    }

    @Override
    public DlVlan getReverse() {
        return this.clone();
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public DlVlan clone() {
        return new DlVlan(vlan);
    }

    @Override
    public boolean isV6() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + vlan;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DlVlan)) {
            return false;
        }
        DlVlan other = (DlVlan) obj;
        if (vlan != other.vlan) {
            return false;
        }
        return true;
    }
}