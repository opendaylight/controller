package org.opendaylight.controller.config.persist.storage.file.xml;

import java.io.FileOutputStream;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class GenerateKey {
    protected static final String KEY_FILE_NAME = ".bashrck";
    protected static final String PATH_TO_KEY = "./" + KEY_FILE_NAME;
    protected static SecretKey key = null;

    public static final void generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            key = keyGen.generateKey();
            byte keyData[] = key.getEncoded();
            FileOutputStream out = new FileOutputStream(PATH_TO_KEY);
            out.write(keyData);
            out.close();
        } catch (Exception err) {
        }
    }

    public static void main(String args[]) {
        System.     out.        print("Generating Key... ");
        generateKey();
        if (key != null) {
            System.      out.          println("Done!");
        }
    }
}
