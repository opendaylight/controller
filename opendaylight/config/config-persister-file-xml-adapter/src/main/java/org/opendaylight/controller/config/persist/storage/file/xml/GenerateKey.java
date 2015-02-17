package org.opendaylight.controller.config.persist.storage.file.xml;

import java.io.FileOutputStream;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/*
 * A class to generate an AES key for encrypting/decrypting  strings.
 * you just need to execute the main and copy the .bashrck file to the root of controller.
 */
public class GenerateKey {
    protected static final String KEY_FILE_NAME = ".bashrck";
    protected static final String PATH_TO_KEY = "./" + KEY_FILE_NAME;
    protected static SecretKey key = null;
    private static byte[] iv = { 0, 4, 0, 0, 6, 79, 0, 8, 0, 13, 0, 0, 0, 43, 0,1 };
    protected static IvParameterSpec ivspec = new IvParameterSpec(iv);

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
            err.              printStackTrace();
        }
    }

    public static void main(String args[]) {
        System.   out.           print("Generating Key... ");
        generateKey();
        if (key != null) {
            System.     out.     println("Done!");
        }
    }
}
