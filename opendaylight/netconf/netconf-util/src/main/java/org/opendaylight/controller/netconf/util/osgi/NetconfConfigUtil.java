/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.osgi;

import com.google.common.base.Optional;
import java.net.InetSocketAddress;
import org.osgi.framework.BundleContext;
import static com.google.common.base.Preconditions.checkNotNull;

    public class NetconfConfigUtil {
    private static final String PREFIX_PROP = "netconf.";

    private enum InfixProp {
        tcp, ssh
    }

    private static final String PORT_SUFFIX_PROP = ".port";
    private static final String ADDRESS_SUFFIX_PROP = ".address";

    public static InetSocketAddress extractTCPNetconfAddress(BundleContext context, String exceptionMessageIfNotFound) {

        Optional<InetSocketAddress> inetSocketAddressOptional = extractSomeNetconfAddress(context, InfixProp.tcp, exceptionMessageIfNotFound);

        if (inetSocketAddressOptional.isPresent() == false) {
            throw new IllegalStateException("Netconf tcp address not found." + exceptionMessageIfNotFound);
        }
        return inetSocketAddressOptional.get();
    }

    public static Optional<InetSocketAddress> extractSSHNetconfAddress(BundleContext context, String exceptionMessage) {
        return extractSomeNetconfAddress(context, InfixProp.ssh, exceptionMessage);
    }

    /**
     * @param context
     *            from which properties are being read.
     * @param infixProp
     *            either tcp or ssh
     * @return value if address and port are valid.
     * @throws IllegalStateException
     *             if address or port are invalid, or configuration is missing
     */
    private static Optional<InetSocketAddress> extractSomeNetconfAddress(BundleContext context,
            InfixProp infixProp, String exceptionMessage) {
        String address = context.getProperty(PREFIX_PROP + infixProp + ADDRESS_SUFFIX_PROP);
        if (address == null) {
            throw new IllegalStateException("Cannot find initial netconf configuration for parameter    "
                    +PREFIX_PROP + infixProp + ADDRESS_SUFFIX_PROP
                    +" in config.ini. "+exceptionMessage);
        }
        String portKey = PREFIX_PROP + infixProp + PORT_SUFFIX_PROP;
        String portString = context.getProperty(portKey);
        checkNotNull(portString, "Netconf port must be specified in properties file with " + portKey);
        try {
            int port = Integer.valueOf(portString);
            return Optional.of(new InetSocketAddress(address, port));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Cannot create " + infixProp + " netconf address from address:" + address
                    + " and port:" + portString, e);
        }
    }
}
