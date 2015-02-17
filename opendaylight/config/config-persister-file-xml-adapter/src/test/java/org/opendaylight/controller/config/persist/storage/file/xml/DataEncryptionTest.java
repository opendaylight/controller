package org.opendaylight.controller.config.persist.storage.file.xml;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Test;

public class DataEncryptionTest {

    @After
    public void after() {
        try {
            FileStorageAdapterTest.delete(new File(".bashrck"));
            File file = new File("./test.xml");
            FileStorageAdapterTest.delete(file);
        } catch (Exception err) {
        }
    }

    @Test
    public void testGeneratingAKey() {
        GenerateKey.main(null);
        assertEquals(true, GenerateKey.key != null);
        try {
            FileStorageAdapterTest.delete(new File(".bashrck"));
        } catch (Exception err) {
        }
    }

    @Test
    public void testEncryption() {
        GenerateKey.main(null);

        String before = "Testing String Encryption & Decryption";
        String encryptString = DataEncrypter.encrypt(before);
        assertEquals(false, before.equals(encryptString));
        String after = DataEncrypter.decrypt(encryptString);
        assertEquals(before, after);
        assertEquals(null, DataEncrypter.encrypt(null));
        assertEquals(null, DataEncrypter.decrypt(null));

        try {
            FileStorageAdapterTest.delete(new File(".bashrck"));
        } catch (Exception err) {
        }
    }

    @Test
    public void testFileEncryption() {
        GenerateKey.main(null);
        String before = "<test><value></value><password>testxml</password></test>";
        File file = new File("./test.xml");
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(before.getBytes());
            out.close();
        } catch (IOException err) {}
        DataEncrypter.encryptCredentialAttributes(file.getPath());
        try {
            FileInputStream in = new FileInputStream(file);
            byte data[] = new byte[(int) file.length()];
            in.read(data);
            in.close();
            String afterEnc = new String(data);
            assertEquals(true,
                    afterEnc.indexOf(DataEncrypter.ENCRYPTED_TAG) != -1);
        } catch (IOException err) {}
        try {
            InputStream in = DataEncrypter.decryptCredentialAttributes(
                    file.getPath()).getInputStream();
            byte data[] = new byte[before.length()];
            in.read(data);
            in.close();
            String after = new String(data);
            assertEquals(before, after);
        } catch (IOException err) {}

        try {
            DataEncrypter.encryptCredentialAttributes(null);
            assertEquals(true, true);
        } catch (Exception err) {
            assertEquals(true, false);
        }
        try {
            DataEncrypter.encryptCredentialAttributes("./ds,sdf,erwf,sdfsdfds");
            assertEquals(true, true);
        } catch (Exception err) {
            assertEquals(true, false);
        }
        assertEquals(DataEncrypter.decryptCredentialAttributes(null), null);
        assertEquals(
                DataEncrypter
                        .decryptCredentialAttributes(".dsfsdfsdfsdfsdfsdl") != null,
                true);

        try {
            FileStorageAdapterTest.delete(new File(".bashrck"));
            file = new File("./test.xml");
            FileStorageAdapterTest.delete(file);
        } catch (Exception err) {
        }
    }
}