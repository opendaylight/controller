/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh.authentication;

import org.apache.commons.io.FileUtils;
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
    private static final Logger logger = LoggerFactory.getLogger(PEMGenerator.class);
    private static final int KEY_SIZE = 4096;

    public static String generateTo(File privateFile) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom sr = new SecureRandom();
        keyGen.initialize(KEY_SIZE, sr);
        KeyPair keypair = keyGen.generateKeyPair();
        logger.info("Generating private key to {}", privateFile.getAbsolutePath());
        String privatePEM = toString(keypair.getPrivate());
        FileUtils.write(privateFile, privatePEM);
        return privatePEM;
    }

    private static String toString(Key key) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            try (PEMWriter pemWriter = new PEMWriter(writer)) {
                pemWriter.writeObject(key);
            }
            return writer.toString();
        }
    }
}
