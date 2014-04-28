
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.arphandler;

import java.net.InetAddress;
import java.util.Arrays;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.HexEncode;
/*
 * ARP Reply event wrapper
 */
public class ARPReply extends ARPEvent {
    private static final long serialVersionUID = 1L;
    private final NodeConnector port;
    private final byte[] tMac;
    private final byte[] sMac;
    private final InetAddress sIP;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((sIP == null) ? 0 : sIP.hashCode());
        result = prime * result + Arrays.hashCode(sMac);
        result = prime * result + Arrays.hashCode(tMac);
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
        if (!(obj instanceof ARPReply)) {
            return false;
        }
        ARPReply other = (ARPReply) obj;
        if (sIP == null) {
            if (other.sIP != null) {
                return false;
            }
        } else if (!sIP.equals(other.sIP)) {
            return false;
        }
        if (!Arrays.equals(sMac, other.sMac)) {
            return false;
        }
        if (!Arrays.equals(tMac, other.tMac)) {
            return false;
        }
        return true;
    }

    public ARPReply(NodeConnector port, InetAddress sIP, byte[] sMAC, InetAddress tIP, byte[] tMAC) {
        super(tIP);
        this.tMac = tMAC;
        this.sIP = sIP;
        this.sMac = sMAC;
        this.port = port;
    }

    public ARPReply(InetAddress tIP, byte[] tMAC) {
        super(tIP);
        this.tMac = tMAC;
        this.sIP = null;
        this.sMac = null;
        this.port = null;
    }

    public byte[] getTargetMac() {
        return tMac;
    }

    public byte[] getSourceMac() {
        return sMac;
    }

    public InetAddress getSourceIP() {
        return sIP;
    }

    public NodeConnector getPort() {
        return port;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ARPReply [");
        if (port != null) {
            builder.append("port=")
                    .append(port)
                    .append(", ");
        }
        if (tMac != null) {
            builder.append("tMac=")
                    .append(HexEncode.bytesToHexString(tMac))
                    .append(", ");
        }
        if (sMac != null) {
            builder.append("sMac=")
                    .append(HexEncode.bytesToHexString(sMac))
                    .append(", ");
        }
        if (sIP != null) {
            builder.append("sIP=")
                    .append(sIP);
        }
        builder.append("]");
        return builder.toString();
    }
}
