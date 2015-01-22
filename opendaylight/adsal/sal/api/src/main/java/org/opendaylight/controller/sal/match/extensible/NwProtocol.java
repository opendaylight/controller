package org.opendaylight.controller.sal.match.extensible;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class NwProtocol extends MatchField<Byte> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "NW_PROTO";
    private static final short MAX = 255;
    private byte protocol;

    /**
     * Creates a Match field for the network protocol
     *
     * @param protocol
     *            the protocol number
     */
    public NwProtocol(byte protocol) {
        super(TYPE);
        this.protocol = protocol;
    }

    public NwProtocol(int protocol) {
        super(TYPE);
        this.protocol = (byte) protocol;
    }

    public NwProtocol(short protocol) {
        super(TYPE);
        this.protocol = (byte) protocol;
    }

    // To satisfy JAXB
    private NwProtocol() {
        super(TYPE);
    }

    @Override
    public Byte getValue() {
        return protocol;
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return String.format("0X%s", Integer.toHexString(NetUtils.getUnsignedByte(protocol)));
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
        int intProtocol = NetUtils.getUnsignedByte(protocol);
        return intProtocol >= 0 && intProtocol <= MAX;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public NwProtocol getReverse() {
        return this.clone();
    }

    @Override
    public NwProtocol clone() {
        return new NwProtocol(protocol);
    }

    @Override
    public boolean isV6() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + protocol;
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
        if (!(obj instanceof NwProtocol)) {
            return false;
        }
        NwProtocol other = (NwProtocol) obj;
        if (protocol != other.protocol) {
            return false;
        }
        return true;
    }
}