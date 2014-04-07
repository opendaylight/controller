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

    public static String generateTo(File privateFile) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom sr = new SecureRandom();
        keyGen.initialize(1024, sr);
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
