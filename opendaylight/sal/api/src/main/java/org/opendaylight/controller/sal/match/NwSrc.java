package org.opendaylight.controller.sal.match;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.opendaylight.controller.sal.utils.NetUtils;

public class NwSrc extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "NW_SRC";
    protected Class<? extends MatchField2> reverseClass = NwDst.class;
    private InetAddress address;
    private InetAddress mask;

    /**
     * Creates a Match2 field for the network source address
     *
     * @param address
     *            the network address
     * @param mask
     *            the network mask
     */
    public NwSrc(InetAddress address, InetAddress mask) {
        super(TYPE);
        this.address = address;
        this.mask = mask;
    }

    @Override
    public Object getValue() {
        return address;
    }

    @Override
    protected String getValueString() {
        return address.toString();
    }

    @Override
    public Object getMask() {
        return mask;
    }

    @Override
    protected String getMaskString() {
        return mask == null ? "null" : mask.toString();
    }

    @Override
    public boolean isValid() {
        if (address != null) {
            if (mask != null) {
                return address instanceof Inet4Address && mask instanceof Inet4Address
                        || address instanceof Inet6Address && mask instanceof Inet6Address;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean hasReverse() {
        return true;
    }

    @Override
    public MatchField2 getReverse() {
        return new NwDst(address, mask);
    }

    @Override
    public MatchField2 clone() {
        return new NwSrc(address, mask);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((mask == null) ? 0 : mask.hashCode());
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
        if (!(obj instanceof NwSrc)) {
            return false;
        }
        NwSrc other = (NwSrc) obj;
        // Equality to be checked against prefix addresses
        int thisMaskLen = (this.mask == null) ? ((this.address instanceof Inet4Address) ? 32 : 128) : NetUtils
                .getSubnetMaskLength(this.mask);
        int otherMaskLen = (other.mask == null) ? ((other.address instanceof Inet4Address) ? 32 : 128) : NetUtils
                .getSubnetMaskLength(other.mask);

        return NetUtils.getSubnetPrefix(address, thisMaskLen).equals(
                NetUtils.getSubnetPrefix(other.address, otherMaskLen));
    }
}
