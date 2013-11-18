/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.hosttracker;

import java.net.InetAddress;

import org.opendaylight.controller.sal.packet.address.DataLinkAddress;

/*
 * Class used to generate a key based on the scheme choosen for hostsdb storage in hosttracker.
 * @author Deepak Udapudi
 */
public class HostIdFactory {
    public static final String DEFAULT_IP_KEY_SCHEME = "IP";
    public static final String IP_MAC_KEY_SCHEME = "IP+MAC";
    private static String scheme = null;
    static {
        scheme = System.getProperty("hosttracker.keyscheme");
    }

    public static String getScheme() {
        return scheme;
    }

    public static IHostId create(InetAddress ip, DataLinkAddress mac) {
        IHostId ipHostId = new IPHostId(ip);
        if (scheme != null) {
            switch (scheme) {

            case DEFAULT_IP_KEY_SCHEME:
                return ipHostId;
            case IP_MAC_KEY_SCHEME:
                IHostId ipMacHostId = new IPMacHostId(ip, mac);
                return ipMacHostId;
            default:
                return ipHostId;

            }
        }
        return ipHostId;
    }

}
