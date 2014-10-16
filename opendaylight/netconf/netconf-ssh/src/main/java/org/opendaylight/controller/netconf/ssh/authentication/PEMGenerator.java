/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh.authentication;

import com.google.common.annotations.VisibleForTesting;
import java.io.FileInputStream;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openssl.PEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

public class PEMGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PEMGenerator.class);
    private static final int KEY_SIZE = 4096;

    private PEMGenerator() {
    }

    public static String readOrGeneratePK(File privateKeyFile) throws IOException {
        if (privateKeyFile.exists() == false) {
            // generate & save to file
            try {
                return generateTo(privateKeyFile);
            } catch (Exception e) {
                LOGGER.error("Exception occurred while generating PEM string to {}", privateKeyFile, e);
                throw new IllegalStateException("Error generating RSA key from file " + privateKeyFile);
            }
        } else {
            // read from file
            try (FileInputStream fis = new FileInputStream(privateKeyFile)) {
                return IOUtils.toString(fis);
            } catch (final IOException e) {
                LOGGER.error("Error reading RSA key from file {}", privateKeyFile, e);
                throw new IOException("Error reading RSA key from file " + privateKeyFile, e);
            }
        }
    }

    /**
     * Generate private key to a file and return its content as string.
     *
     * @param privateFile path where private key should be generated
     * @return String representation of private key
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @VisibleForTesting
    public static String generateTo(File privateFile) throws IOException, NoSuchAlgorithmException {
        LOGGER.info("Generating private key to {}", privateFile.getAbsolutePath());
        String privatePEM = generate();
        FileUtils.write(privateFile, privatePEM);
        return privatePEM;
    }

    @VisibleForTesting
    public static String generate() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom sr = new SecureRandom();
        keyGen.initialize(KEY_SIZE, sr);
        KeyPair keypair = keyGen.generateKeyPair();
        return toString(keypair.getPrivate());
    }

    /**
     * Get string representation of a key.
     */
    private static String toString(Key key) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            try (PEMWriter pemWriter = new PEMWriter(writer)) {
                pemWriter.writeObject(key);
            }
            return writer.toString();
        }
    }

}
