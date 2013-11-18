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
 * IP only key class implementation using the marker interface IHostId
 * @author Deepak Udapudi
 */

public class IPHostId implements IHostId, Serializable {
    private static final long serialVersionUID = 1L;
    private InetAddress ipAddress;

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public IPHostId(InetAddress ipAddress) {
        super();
        this.ipAddress = ipAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
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
        IPHostId other = (IPHostId) obj;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        return true;
    }

    public static IHostId fromIP(InetAddress addr) {
        return new IPHostId(addr);
    }

}
