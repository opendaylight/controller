package org.opendaylight.controller.sal.match.extensible;

import org.opendaylight.controller.sal.utils.NetUtils;

public class TpSrc extends MatchField<Short> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "TP_SRC";
    private short port;

    /**
     * Creates a Match field for the Transport source port
     *
     * @param port
     *            the transport port
     */
    public TpSrc(short port) {
        super(TYPE);
        this.port = port;
    }

    @Override
    public Short getValue() {
        return port;
    }

    @Override
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
    public boolean hasReverse() {
        return true;
    }

    @Override
    public TpDst getReverse() {
        return new TpDst(port);
    }

    @Override
    public TpSrc clone() {
        return new TpSrc(port);
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
        if (!(obj instanceof TpSrc)) {
            return false;
        }
        TpSrc other = (TpSrc) obj;
        if (port != other.port) {
            return false;
        }
        return true;
    }
}