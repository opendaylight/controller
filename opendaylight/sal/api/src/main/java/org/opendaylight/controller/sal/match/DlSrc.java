package org.opendaylight.controller.sal.match;

import java.util.Arrays;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

public class DlSrc extends MatchField2 {
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
    public DlDst getReverse() {
        return new DlDst(address);
    }

    @Override
    public MatchField2 clone() {
        return new DlSrc(address);
    }
}