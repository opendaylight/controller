package org.opendaylight.controller.sal.match.extensible;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class DlType extends MatchField<Short> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_TYPE";
    private short ethertype;

    /**
     * Creates a Match field for the data layer type
     *
     * @param address
     *            the data layer type
     */
    public DlType(short ethertype) {
        super(TYPE);
        this.ethertype = ethertype;
    }

    // To satisfy JAXB
    private DlType() {
        super(TYPE);
    }

    @Override
    public Short getValue() {
        return ethertype;
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return String.format("0X%s", Integer.toHexString(NetUtils.getUnsignedShort(ethertype)));
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
        return true;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public DlType getReverse() {
        return this.clone();
    }

    @Override
    public DlType clone() {
        return new DlType(ethertype);
    }

    @Override
    public boolean isV6() {
        return this.ethertype == EtherTypes.IPv6.shortValue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ethertype;
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
        if (!(obj instanceof DlType)) {
            return false;
        }
        DlType other = (DlType) obj;
        if (ethertype != other.ethertype) {
            return false;
        }
        return true;
    }
}