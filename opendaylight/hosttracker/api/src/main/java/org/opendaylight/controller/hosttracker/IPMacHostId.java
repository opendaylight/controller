/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.hosttracker;

import java.io.Serializable;
import java.net.InetAddress;

/*
 * IP + Mac key class implementation using the marker interface IHostId
 * @author Deepak Udapudi
 */

import org.opendaylight.controller.sal.packet.address.DataLinkAddress;

public class IPMacHostId implements IHostId, Serializable {

    private static final long serialVersionUID = 1L;
    private InetAddress ipAddress;
    private DataLinkAddress macAddr;

    public IPMacHostId(InetAddress ipAddress, DataLinkAddress macAddr) {
        super();
        this.ipAddress = ipAddress;
        this.macAddr = macAddr;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public DataLinkAddress getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(DataLinkAddress macAddr) {
        this.macAddr = macAddr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        result = prime * result + ((macAddr == null) ? 0 : macAddr.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IPMacHostId other = (IPMacHostId) obj;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        if (macAddr == null) {
            if (other.macAddr != null)
                return false;
        } else if (!macAddr.equals(other.macAddr))
            return false;
        return true;
    }

    public static IHostId fromIPAndMac(InetAddress ip, DataLinkAddress mac) {
        return new IPMacHostId(ip, mac);
    }

}
