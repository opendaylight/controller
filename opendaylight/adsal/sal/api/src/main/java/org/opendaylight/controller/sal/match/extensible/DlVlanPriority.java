package org.opendaylight.controller.sal.match.extensible;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class DlVlanPriority extends MatchField<Byte> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_VLAN_PR";
    private static final byte MAX = 7;
    private byte vlanPriority;

    /**
     * Creates a Match field for the data layer type
     *
     * @param address
     *            the data layer type
     */
    public DlVlanPriority(byte vlanPriority) {
        super(TYPE);
        this.vlanPriority = vlanPriority;
    }

    // To satisfy JAXB
    private DlVlanPriority() {
        super(TYPE);
    }

    @Override
    public Byte getValue() {
        return vlanPriority;
    }

    @Override
    @XmlElement(name = "mask")
    protected String getValueString() {
        return String.format("0X%s", Integer.toHexString(NetUtils.getUnsignedByte(vlanPriority)));
    }

    @Override
    public Byte getMask() {
        return null;
    }

    @Override
    protected String getMaskString() {
        return null;
    }

    @Override
    public boolean isValid() {
        return vlanPriority >= 0 && vlanPriority <= MAX;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public DlVlanPriority getReverse() {
        return this.clone();
    }

    @Override
    public DlVlanPriority clone() {
        return new DlVlanPriority(vlanPriority);
    }

    @Override
    public boolean isV6() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + vlanPriority;
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
        if (!(obj instanceof DlVlanPriority)) {
            return false;
        }
        DlVlanPriority other = (DlVlanPriority) obj;
        if (vlanPriority != other.vlanPriority) {
            return false;
        }
        return true;
    }
}