package org.opendaylight.controller.sal.match;

import java.util.Arrays;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

public class DlSrc extends MatchField2<byte[]> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_SRC";
    private byte[] address;

    /**
     * Creates a Match2 field for the source datalayer address
     *
     * @param address
     *            the datalayer address. The constructor makes a copy of it
     */
    public DlSrc(byte[] address) {
        super(TYPE);
        if (address != null) {
            this.address = Arrays.copyOf(address, address.length);
        }
    }

    @Override
    public byte[] getValue() {
        return Arrays.copyOf(address, address.length);
    }

    @Override
    protected String getValueString() {
        return HexEncode.bytesToHexStringFormat(address);
    }

    @Override
    public byte[] getMask() {
        return new byte[0];
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
    public DlDst getReverse() {
        return new DlDst(address);
    }

    @Override
    public DlSrc clone() {
        return new DlSrc(address);
    }

    @Override
    public boolean isV6() {
        return true;
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
        if (!(obj instanceof DlSrc)) {
            return false;
        }
        DlSrc other = (DlSrc) obj;
        return Arrays.equals(address, other.address);
    }
}