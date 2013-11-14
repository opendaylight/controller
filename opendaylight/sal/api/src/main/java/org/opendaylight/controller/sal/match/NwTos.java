package org.opendaylight.controller.sal.match;

import org.opendaylight.controller.sal.utils.HexEncode;

public class NwTos extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "NW_TOS";
    private static final short MAX = 63;
    private byte tos;

    /**
     * Creates a Match2 field for the network TOS
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

    @Override
    public Object getValue() {
        return tos;
    }

    @Override
    protected String getValueString() {
        return HexEncode.longToHexString(tos);
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
        return tos >= 0 && tos <= MAX;
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
        return new NwTos(tos);
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