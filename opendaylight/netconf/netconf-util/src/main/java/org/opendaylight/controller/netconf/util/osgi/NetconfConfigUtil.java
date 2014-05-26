/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.osgi;

import com.google.common.base.Optional;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public final class NetconfConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(NetconfConfigUtil.class);

    public static final InetSocketAddress DEFAULT_NETCONF_TCP_ADDRESS
            = new InetSocketAddress("127.0.0.1", 8383);
    public static final InetSocketAddress DEFAULT_NETCONF_SSH_ADDRESS
            = new InetSocketAddress("0.0.0.0", 1830);

    private static final String PREFIX_PROP = "netconf.";

    private NetconfConfigUtil() {
    }

    private enum InfixProp {
        tcp, ssh
    }

    private static final String PORT_SUFFIX_PROP = ".port";
    private static final String ADDRESS_SUFFIX_PROP = ".address";
    private static final String CLIENT_PROP = ".client";
    private static final String PRIVATE_KEY_PATH_PROP = ".pk.path";

    private static final String CONNECTION_TIMEOUT_MILLIS_PROP = "connectionTimeoutMillis";
    private static final long DEFAULT_TIMEOUT_MILLIS = 5000;

    public static long extractTimeoutMillis(final BundleContext bundleContext) {
        final String key = PREFIX_PROP + CONNECTION_TIMEOUT_MILLIS_PROP;
        final String timeoutString = bundleContext.getProperty(key);
        if (timeoutString == null || timeoutString.length() == 0) {
            return DEFAULT_TIMEOUT_MILLIS;
        }
        try {
            return Long.parseLong(timeoutString);
        } catch (final NumberFormatException e) {
            logger.warn("Cannot parse {} property: {}, using defaults", key, timeoutString, e);
            return DEFAULT_TIMEOUT_MILLIS;
        }
    }

    public static InetSocketAddress extractTCPNetconfServerAddress(final BundleContext context, final InetSocketAddress defaultAddress) {
        final Optional<InetSocketAddress> extracted = extractNetconfServerAddress(context, InfixProp.tcp);
        final InetSocketAddress netconfTcpAddress = getNetconfAddress(defaultAddress, extracted, InfixProp.tcp);
        logger.debug("Using {} as netconf tcp address", netconfTcpAddress);
        if (netconfTcpAddress.getAddress().isAnyLocalAddress()) {
            logger.warn("Unprotected netconf TCP address is configured to ANY local address. This is a security risk. " +
                    "Consider changing {} to 127.0.0.1", PREFIX_PROP + InfixProp.tcp + ADDRESS_SUFFIX_PROP);
        }
        return netconfTcpAddress;
    }

    public static InetSocketAddress extractTCPNetconfClientAddress(final BundleContext context, final InetSocketAddress defaultAddress) {
        final Optional<InetSocketAddress> extracted = extractNetconfClientAddress(context, InfixProp.tcp);
        return getNetconfAddress(defaultAddress, extracted, InfixProp.tcp);
    }

    /**
     * Get extracted address or default.
     *
     * @throws java.lang.IllegalStateException if neither address is present.
     */
    private static InetSocketAddress getNetconfAddress(final InetSocketAddress defaultAddress, Optional<InetSocketAddress> extractedAddress, InfixProp infix) {
        InetSocketAddress inetSocketAddress;

        if (extractedAddress.isPresent() == false) {
            logger.debug("Netconf {} address not found, falling back to default {}", infix, defaultAddress);

            if (defaultAddress == null) {
                logger.warn("Netconf {} address not found, default address not provided", infix);
                throw new IllegalStateException("Netconf " + infix + " address not found, default address not provided");
            }
            inetSocketAddress = defaultAddress;
        } else {
            inetSocketAddress = extractedAddress.get();
        }

        return inetSocketAddress;
    }

    public static InetSocketAddress extractSSHNetconfAddress(final BundleContext context, final InetSocketAddress defaultAddress) {
        Optional<InetSocketAddress> extractedAddress = extractNetconfServerAddress(context, InfixProp.ssh);
        InetSocketAddress netconfSSHAddress = getNetconfAddress(defaultAddress, extractedAddress, InfixProp.ssh);
        logger.debug("Using {} as netconf SSH address", netconfSSHAddress);
        return netconfSSHAddress;
    }

    public static String getPrivateKeyPath(final BundleContext context) {
        return getPropertyValue(context, PREFIX_PROP + InfixProp.ssh + PRIVATE_KEY_PATH_PROP);
    }

    private static String getPropertyValue(final BundleContext context, final String propertyName) {
        final String propertyValue = context.getProperty(propertyName);
        if (propertyValue == null) {
            throw new IllegalStateException("Cannot find initial property with name '" + propertyName + "'");
        }
        return propertyValue;
    }

    /**
     * @param context   from which properties are being read.
     * @param infixProp either tcp or ssh
     * @return value if address and port are present and valid, Optional.absent otherwise.
     * @throws IllegalStateException if address or port are invalid, or configuration is missing
     */
    private static Optional<InetSocketAddress> extractNetconfServerAddress(final BundleContext context,
                                                                           final InfixProp infixProp) {

        final Optional<String> address = getProperty(context, PREFIX_PROP + infixProp + ADDRESS_SUFFIX_PROP);
        final Optional<String> port = getProperty(context, PREFIX_PROP + infixProp + PORT_SUFFIX_PROP);

        if (address.isPresent() && port.isPresent()) {
            try {
                return Optional.of(parseAddress(address, port));
            } catch (final RuntimeException e) {
                logger.warn("Unable to parse {} netconf address from {}:{}, fallback to default",
                        infixProp, address, port, e);
            }
        }
        return Optional.absent();
    }

    private static InetSocketAddress parseAddress(final Optional<String> address, final Optional<String> port) {
        final int portNumber = Integer.valueOf(port.get());
        return new InetSocketAddress(address.get(), portNumber);
    }

    private static Optional<InetSocketAddress> extractNetconfClientAddress(final BundleContext context,
                                                                           final InfixProp infixProp) {
        final Optional<String> address = getProperty(context,
                PREFIX_PROP + infixProp + CLIENT_PROP + ADDRESS_SUFFIX_PROP);
        final Optional<String> port = getProperty(context,
                PREFIX_PROP + infixProp + CLIENT_PROP + PORT_SUFFIX_PROP);

        if (address.isPresent() && port.isPresent()) {
            try {
                return Optional.of(parseAddress(address, port));
            } catch (final RuntimeException e) {
                logger.warn("Unable to parse client {} netconf address from {}:{}, fallback to server address",
                        infixProp, address, port, e);
            }
        }
        return extractNetconfServerAddress(context, infixProp);
    }

    private static Optional<String> getProperty(final BundleContext context, final String propKey) {
        String value = context.getProperty(propKey);
        if (value != null && value.isEmpty()) {
            value = null;
        }
        return Optional.fromNullable(value);
    }
}
