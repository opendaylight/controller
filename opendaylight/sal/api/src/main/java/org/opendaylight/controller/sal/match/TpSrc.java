package org.opendaylight.controller.sal.match;

import org.opendaylight.controller.sal.utils.NetUtils;

public class TpSrc extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "TP_SRC";
    private short port;

    /**
     * Creates a Match2 field for the Transport source port
     *
     * @param port
     *            the transport port
     */
    public TpSrc(short port) {
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
    public boolean hasReverse() {
        return true;
    }

    @Override
    public TpDst getReverse() {
        return new TpDst(port);
    }

    @Override
    public MatchField2 clone() {
        return new TpSrc(port);
    }
}