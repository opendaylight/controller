
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.arphandler;

import java.io.Serializable;
import java.net.InetAddress;
/*
 * ARP Event base class
 */
public abstract class ARPEvent implements Serializable{

    private static final long serialVersionUID = 1L;
    private final InetAddress tIP;


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime + ((tIP == null) ? 0 : tIP.hashCode());
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
        if (!(obj instanceof ARPEvent)) {
            return false;
        }
        ARPEvent other = (ARPEvent) obj;
        if (tIP == null) {
            if (other.tIP != null) {
                return false;
            }
        } else if (!tIP.equals(other.tIP)) {
            return false;
        }
        return true;
    }

    public ARPEvent(InetAddress ip) {
        this.tIP = ip;
    }

    public InetAddress getTargetIP() {
        return tIP;
    }
}
