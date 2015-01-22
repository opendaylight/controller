package org.opendaylight.controller.sal.match.extensible;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class TpDst extends MatchField<Short> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "TP_DST";
    private short port;

    /**
     * Creates a Match field for the transport destination port
     *
     * @param port
     *            the transport port
     */
    public TpDst(short port) {
        super(TYPE);
        this.port = port;
    }

    // To satisfy JAXB
    private TpDst() {
        super(TYPE);
    }

    @Override
    public Short getValue() {
        return port;
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return String.valueOf(NetUtils.getUnsignedShort(port));
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
    public TpSrc getReverse() {
        return new TpSrc(port);
    }

    @Override
    public boolean hasReverse() {
        return true;
    }

    @Override
    public TpDst clone() {
        return new TpDst(port);
    }

    @Override
    public boolean isV6() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof TpDst)) {
            return false;
        }
        TpDst other = (TpDst) obj;
        if (port != other.port) {
            return false;
        }
        return true;
    }
}