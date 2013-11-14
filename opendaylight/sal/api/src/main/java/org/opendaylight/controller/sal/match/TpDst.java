package org.opendaylight.controller.sal.match;

import org.opendaylight.controller.sal.utils.NetUtils;

public class TpDst extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "TP_DST";
    private short port;

    /**
     * Creates a Match2 field for the transport destination port
     *
     * @param port
     *            the transport port
     */
    public TpDst(short port) {
        super(TYPE);
        this.port = port;
    }

    @Override
    public Object getValue() {
        return port;
    }

    @Override
    protected String getValueString() {
        return String.valueOf(NetUtils.getUnsignedShort(port));
    }

    @Override
    public Object getMask() {
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
    public MatchField2 clone() {
        return new TpDst(port);
    }
}