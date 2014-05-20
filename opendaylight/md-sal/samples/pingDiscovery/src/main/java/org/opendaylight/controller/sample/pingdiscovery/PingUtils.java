/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Utility class for ping
 * 
 * @author Devin Avery
 * @author Greg Hall
 */

package org.opendaylight.controller.sample.pingdiscovery;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;

public class PingUtils {

    public static String getIPAddressAsString(IpAddress ipAddr) {
        String ipAddrStr = null;
        if (ipAddr.getIpv4Address() != null) {
            ipAddrStr = ipAddr.getIpv4Address().getValue();
        } else {
            ipAddrStr = ipAddr.getIpv6Address().getValue();
        }

        return ipAddrStr;
    }
}
