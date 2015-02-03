package org.opendaylight.controller.sal.match.extensible;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.NetUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class NwDst extends MatchField<InetAddress> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "NW_DST";
    private InetAddress address;
    private InetAddress mask;

    /**
     * Creates a Match field for the network destination address
     *
     * @param address
     *            the network address
     * @param mask
     *            the network mask
     */
    public NwDst(InetAddress address, InetAddress mask) {
        super(TYPE);
        this.address = address;
        this.mask = mask;
    }

    // To satisfy JAXB
    private NwDst() {
        super(TYPE);
    }

    public NwDst(InetAddress address) {
        super(TYPE);
        this.address = address;
        this.mask = null;
    }

    @Override
    public InetAddress getValue() {
        return address;
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return address.getHostAddress();
    }

    @Override
    public InetAddress getMask() {
        return mask;
    }

    @Override
    @XmlElement(name = "mask")
    protected String getMaskString() {
        return (mask == null) ? "null" : mask.getHostAddress();
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
    public NwSrc getReverse() {
        return new NwSrc(address, mask);
    }

    @Override
    public NwDst clone() {
        return new NwDst(address, mask);
    }

    @Override
    public boolean isV6() {
        return address instanceof Inet6Address;
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
        if (!(obj instanceof NwDst)) {
            return false;
        }
        NwDst other = (NwDst) obj;
        // Equality to be checked against prefix addresses
        int thisMaskLen = (this.mask == null) ? ((this.address instanceof Inet4Address) ? 32 : 128) : NetUtils
                .getSubnetMaskLength(this.mask);
        int otherMaskLen = (other.mask == null) ? ((other.address instanceof Inet4Address) ? 32 : 128) : NetUtils
                .getSubnetMaskLength(other.mask);

        return NetUtils.getSubnetPrefix(address, thisMaskLen).equals(
                NetUtils.getSubnetPrefix(other.address, otherMaskLen));
    }
}
