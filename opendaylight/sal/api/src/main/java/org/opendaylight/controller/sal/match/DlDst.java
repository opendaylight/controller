package org.opendaylight.controller.sal.match;

import java.util.Arrays;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

public class DlDst extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_DST";
    private byte[] address;

    /**
     * Creates a Match2 field for the destination data layer address
     *
     * @param address
     *            the data layer address. The constructor makes a copy of it
     */
    public DlDst(byte[] address) {
        super(TYPE);
        if (address != null) {
            this.address = Arrays.copyOf(address, address.length);
        }
    }

    @Override
    public Object getValue() {
        return Arrays.copyOf(address, address.length);
    }

    @Override
    protected String getValueString() {
        return HexEncode.bytesToHexStringFormat(address);
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
        return address != null && address.length == NetUtils.MACAddrLengthInBytes;
    }

    @Override
    public boolean hasReverse() {
        return true;
    }

    @Override
    public MatchField2 getReverse() {
        return new DlSrc(address);
    }

    @Override
    public MatchField2 clone() {
        return new DlDst(address);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(address);
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
        if (!(obj instanceof DlDst)) {
            return false;
        }
        DlDst other = (DlDst) obj;
        if (!Arrays.equals(address, other.address)) {
            return false;
        }
        return true;
    }
}
