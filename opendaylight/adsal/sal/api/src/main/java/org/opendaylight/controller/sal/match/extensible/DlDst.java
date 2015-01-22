package org.opendaylight.controller.sal.match.extensible;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class DlDst extends MatchField<byte[]> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_DST";
    private byte[] address;

    /**
     * Creates a Match field for the destination data layer address
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

    // To satisfy JAXB
    public DlDst() {
        super(TYPE);
    }

    @Override
    public byte[] getValue() {
        return Arrays.copyOf(address, address.length);
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return HexEncode.bytesToHexStringFormat(address);
    }

    @Override
    public byte[] getMask() {
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
    public DlSrc getReverse() {
        return new DlSrc(address);
    }

    @Override
    public DlDst clone() {
        return new DlDst(address);
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
        if (!(obj instanceof DlDst)) {
            return false;
        }
        DlDst other = (DlDst) obj;
        return Arrays.equals(address, other.address);
    }
}
