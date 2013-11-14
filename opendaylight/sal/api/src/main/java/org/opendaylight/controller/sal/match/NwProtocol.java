package org.opendaylight.controller.sal.match;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

public class NwProtocol extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "NW_PROTO";
    private static final short MAX = 255;
    private byte protocol;

    /**
     * Creates a Match2 field for the network protocol
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

    @Override
    public Object getValue() {
        return protocol;
    }

    @Override
    protected String getValueString() {
        return HexEncode.longToHexString(protocol);
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
        int intProtocol = NetUtils.getUnsignedByte(protocol);
        return intProtocol >= 0 && intProtocol <= MAX;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public MatchField2 getReverse() {
        return this.clone();
    }

    @Override
    public MatchField2 clone() {
        return new NwProtocol(protocol);
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