/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.google.common.base.Preconditions;

public final class SSLUtil {

    private SSLUtil() {
    }

    public static SSLContext initializeSecureContext(final String pass, final InputStream ksKeysFile, final InputStream ksTrustFile,
            final String algorithm) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException {

        Preconditions.checkNotNull(ksTrustFile, "ksTrustFile cannot be null");
        Preconditions.checkNotNull(ksKeysFile, "ksKeysFile cannot be null");

        final char[] passphrase = pass.toCharArray();

        // First initialize the key and trust material.
        final KeyStore ksKeys = KeyStore.getInstance("JKS");
        ksKeys.load(ksKeysFile, passphrase);
        final KeyStore ksTrust = KeyStore.getInstance("JKS");
        ksTrust.load(ksTrustFile, passphrase);

        // KeyManager's decide which key material to use.
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ksKeys, passphrase);

        // TrustManager's decide whether to allow connections.
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        tmf.init(ksTrust);

        final SSLContext sslContext = SSLContext.getInstance("TLS");

        // Create/initialize the SSLContext with key material
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

}
