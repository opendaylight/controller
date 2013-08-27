
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.arphandler;

import java.net.InetAddress;

import org.opendaylight.controller.arphandler.ARPEvent;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.switchmanager.Subnet;
/*
 * ARP Request event wrapper Consists of IP and Subnet (and a
 * HostNodeConnector if is unicast) For unicast request, construct with a
 * specified host
 */
public class ARPRequest extends ARPEvent {
    private final Subnet subnet;
    private final HostNodeConnector host;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((subnet == null) ? 0 : subnet.hashCode());
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
        if (!(obj instanceof ARPRequest)) {
            return false;
        }
        ARPRequest other = (ARPRequest) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (subnet == null) {
            if (other.subnet != null) {
                return false;
            }
        } else if (!subnet.equals(other.subnet)) {
            return false;
        }
        return true;
    }

    // broadcast
    public ARPRequest(InetAddress ip, Subnet subnet) {
        super(ip);
        this.subnet = subnet;
        this.host = null;
    }

    // unicast
    public ARPRequest(HostNodeConnector host, Subnet subnet) {
        super(host.getNetworkAddress());
        this.host = host;
        this.subnet = subnet;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public HostNodeConnector getHost() {
        return host;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ARPRequest [");
        if (subnet != null) {
            builder.append("subnet=")
                    .append(subnet)
                    .append(", ");
        }
        if (host != null) {
            builder.append("host=")
                    .append(host);
        }
        builder.append("]");
        return builder.toString();
    }
}
