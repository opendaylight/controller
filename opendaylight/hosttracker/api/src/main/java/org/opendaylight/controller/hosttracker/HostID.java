package org.opendaylight.controller.hosttracker;

import java.io.Serializable;
import java.net.InetAddress;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.packet.address.DataLinkAddress;

public class HostID implements Serializable {
    private static final long serialVersionUID = 1L;
    InetAddress ipAddress;
    DataLinkAddress macAddress;

    public HostID(InetAddress ipAddress, DataLinkAddress macAddress) {
        super();
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setMacAddress(DataLinkAddress macAddress) {
        this.macAddress = macAddress;
    }

    public DataLinkAddress getMacAddress() {
        return macAddress;
    }

    public static HostID fromIPAndMac(InetAddress ip, DataLinkAddress mac) {
        return new HostID(ip, mac);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(163989, 56989).append(macAddress).append(ipAddress).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        HostID rhs = (HostID) obj;
        return new EqualsBuilder().append(this.getMacAddress(), rhs.getMacAddress())
                .append(this.getIpAddress(), rhs.getIpAddress()).isEquals();
    }

}
