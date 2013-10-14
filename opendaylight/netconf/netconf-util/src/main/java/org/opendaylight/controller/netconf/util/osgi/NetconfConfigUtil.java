/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.osgi;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.opendaylight.controller.config.stat.ConfigProvider;
import org.opendaylight.protocol.util.SSLUtil;

import com.google.common.base.Optional;

public class NetconfConfigUtil {
    private static final String PREFIX_PROP = "netconf.";

    private enum InfixProp {
        tcp, tls
    }

    private static final String PORT_SUFFIX_PROP = ".port";
    private static final String ADDRESS_SUFFIX_PROP = ".address";

    private static final String NETCONF_TLS_KEYSTORE_PROP = PREFIX_PROP + InfixProp.tls + ".keystore";
    private static final String NETCONF_TLS_KEYSTORE_PASSWORD_PROP = NETCONF_TLS_KEYSTORE_PROP + ".password";

    public static Optional<InetSocketAddress> extractTCPNetconfAddress(ConfigProvider configProvider) {
        return extractSomeNetconfAddress(configProvider, InfixProp.tcp);
    }

    public static Optional<TLSConfiguration> extractTLSConfiguration(ConfigProvider configProvider) {
        Optional<InetSocketAddress> address = extractSomeNetconfAddress(configProvider, InfixProp.tls);
        if (address.isPresent()) {
            String keystoreFileName = configProvider.getProperty(NETCONF_TLS_KEYSTORE_PROP);
            File keystoreFile = new File(keystoreFileName);
            checkState(keystoreFile.exists() && keystoreFile.isFile() && keystoreFile.canRead(),
                    "Keystore file %s does not exist or is not readable file", keystoreFileName);
            keystoreFile = keystoreFile.getAbsoluteFile();
            String keystorePassword = configProvider.getProperty(NETCONF_TLS_KEYSTORE_PASSWORD_PROP);
            checkNotNull(keystoreFileName, "Property %s must be defined for tls netconf server",
                    NETCONF_TLS_KEYSTORE_PROP);
            keystorePassword = keystorePassword != null ? keystorePassword : "";
            return Optional.of(new TLSConfiguration(address.get(), keystoreFile, keystorePassword));
        } else {
            return Optional.absent();
        }
    }

    public static class TLSConfiguration {
        private final InetSocketAddress address;
        private final File keystoreFile;
        private final String keystorePassword;
        private final SSLContext sslContext;

        TLSConfiguration(InetSocketAddress address, File keystoreFile, String keystorePassword) {
            this.address = address;
            this.keystoreFile = keystoreFile;
            this.keystorePassword = keystorePassword;
            try {
                try (InputStream keyStoreIS = new FileInputStream(keystoreFile)) {
                    try (InputStream trustStoreIS = new FileInputStream(keystoreFile)) {
                        sslContext = SSLUtil.initializeSecureContext("password", keyStoreIS, trustStoreIS, KeyManagerFactory.getDefaultAlgorithm());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot initialize ssl context for netconf file " + keystoreFile, e);
            }
        }

        public SSLContext getSslContext() {
            return sslContext;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public File getKeystoreFile() {
            return keystoreFile;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }
    }

    /**
     * @param configProvider
     *            from which properties are being read.
     * @param infixProp
     *            either tcp or tls
     * @return absent if address is missing, value if address and port are
     *         valid.
     * @throws IllegalStateException
     *             if address or port are invalid
     */
    private static Optional<InetSocketAddress> extractSomeNetconfAddress(ConfigProvider configProvider,
            InfixProp infixProp) {
        String address = configProvider.getProperty(PREFIX_PROP + infixProp + ADDRESS_SUFFIX_PROP);
        if (address == null) {
            return Optional.absent();
        }
        String portKey = PREFIX_PROP + infixProp + PORT_SUFFIX_PROP;
        String portString = configProvider.getProperty(portKey);
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
