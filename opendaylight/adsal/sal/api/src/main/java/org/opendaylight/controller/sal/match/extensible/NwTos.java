package org.opendaylight.controller.sal.match.extensible;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class NwTos extends MatchField<Byte> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "NW_TOS";
    private static final short MAX = 63;
    private byte tos;

    /**
     * Creates a Match field for the network TOS
     *
     * @param address
     *            the network TOS
     */
    public NwTos(byte tos) {
        super(TYPE);
        this.tos = tos;
    }

    public NwTos(int tos) {
        super(TYPE);
        this.tos = (byte) tos;
    }

    public NwTos(short tos) {
        super(TYPE);
        this.tos = (byte) tos;
    }

    // To satisfy JAXB
    private NwTos() {
        super(TYPE);
    }

    @Override
    public Byte getValue() {
        return tos;
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return String.format("0X%s", Integer.toHexString(NetUtils.getUnsignedByte(tos)));
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
        return tos >= 0 && tos <= MAX;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public NwTos getReverse() {
        return this.clone();
    }

    @Override
    public NwTos clone() {
        return new NwTos(tos);
    }

    @Override
    public boolean isV6() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + tos;
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
        if (!(obj instanceof NwTos)) {
            return false;
        }
        NwTos other = (NwTos) obj;
        if (tos != other.tos) {
            return false;
        }
        return true;
    }
}