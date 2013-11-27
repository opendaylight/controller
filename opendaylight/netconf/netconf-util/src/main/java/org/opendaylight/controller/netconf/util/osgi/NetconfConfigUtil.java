/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.osgi;

import org.osgi.framework.BundleContext;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;

public class NetconfConfigUtil {
    private static final String PREFIX_PROP = "netconf.";
    private static final String TCP = "tcp";
    private static final String PORT_SUFFIX_PROP = ".port";
    private static final String ADDRESS_SUFFIX_PROP = ".address";

    public static InetSocketAddress extractTCPNetconfAddress(BundleContext context, String exceptionMessageIfNotFound) {
        String addressKey = PREFIX_PROP + TCP + ADDRESS_SUFFIX_PROP;
        String address = context.getProperty(addressKey);
        if (address == null) {
            throw new IllegalStateException(addressKey + " not found." + exceptionMessageIfNotFound);
        }
        String portKey = PREFIX_PROP + TCP + PORT_SUFFIX_PROP;
        String portString = context.getProperty(portKey);
        checkNotNull(portString, "Netconf port must be specified in properties file with " + portKey);
        try {
            int port = Integer.valueOf(portString);
            return new InetSocketAddress(address, port);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Cannot create plaintext netconf address from address:" + address
                    + " and port:" + portString, e);
        }
    }
}
