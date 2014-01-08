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
    private static final String CLIENT_PROP = ".client";
    private static final String PRIVATE_KEY_PATH_PROP = ".pk.path";

    public static InetSocketAddress extractTCPNetconfAddress(BundleContext context, String exceptionMessageIfNotFound, boolean forClient) {

        Optional<InetSocketAddress> inetSocketAddressOptional = extractSomeNetconfAddress(context, InfixProp.tcp, exceptionMessageIfNotFound, forClient);

        if (inetSocketAddressOptional.isPresent() == false) {
            throw new IllegalStateException("Netconf tcp address not found." + exceptionMessageIfNotFound);
        }
        return inetSocketAddressOptional.get();
    }

    public static Optional<InetSocketAddress> extractSSHNetconfAddress(BundleContext context, String exceptionMessage) {
        return extractSomeNetconfAddress(context, InfixProp.ssh, exceptionMessage, false);
    }

    public static String getPrivateKeyPath(BundleContext context){
        return getPropertyValue(context,PREFIX_PROP + InfixProp.ssh +PRIVATE_KEY_PATH_PROP);
    }
    private static String getPropertyValue(BundleContext context, String propertyName){
        String propertyValue = context.getProperty(propertyName);
        if (propertyValue == null){
            throw new IllegalStateException("Cannot find initial property with name '"+propertyName+"'");
        }
        return propertyValue;
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
                                                                         InfixProp infixProp,
                                                                         String exceptionMessage,
                                                                         boolean client) {
        String address = "";
        if (client) {
            address = context.getProperty(PREFIX_PROP + infixProp + CLIENT_PROP + ADDRESS_SUFFIX_PROP);
        }
        if (address == null || address.equals("")){
            address = context.getProperty(PREFIX_PROP + infixProp + ADDRESS_SUFFIX_PROP);
        }
        if (address == null || address.equals("")) {
            throw new IllegalStateException("Cannot find initial netconf configuration for parameter    "
                    +PREFIX_PROP + infixProp + ADDRESS_SUFFIX_PROP
                    +" in config.ini. "+exceptionMessage);
        }
        String portKey = "";
        if (client) {
            portKey = PREFIX_PROP + infixProp + CLIENT_PROP + PORT_SUFFIX_PROP;
        }
        if (portKey == null || portKey.equals("")){
            portKey = PREFIX_PROP + infixProp + PORT_SUFFIX_PROP;
        }
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
