package org.opendaylight.controller.config.persist.storage.file.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataEncrypter {

    private static SecretKey key = null;
    private static final String ENCRYPTED_TAG = "Encrypted:";
    private static final Logger LOG = LoggerFactory.getLogger(DataEncrypter.class);
    private static TAG[] tagsToEncrypt = null;
    static {
        init();
    }
    private static final void init(){
        tagsToEncrypt = new TAG[1];
        tagsToEncrypt[0] = new TAG("password");

        if(key==null){
            try{
                //I was getting a ClassNotFoundException when trying to deserialize the key
                //I have no idea why? Overcome it by creating a StubObjectInputStream
                ObjectInputStream in = new StubObjectInputStream(new FileInputStream(".aesenc"));
                key = (SecretKey)in.readObject();
                in.close();
            }catch(Exception err){}
        }

        if(key==null){
            try{
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128);
                key = keyGen.generateKey();
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(".aesenc"));
                out.writeObject(key);
                out.close();
            }catch(Exception err){}
        }
    }

    public static String encrypt(String str){
        if(str.startsWith(ENCRYPTED_TAG)){
            return str;
        }
        try{
            Cipher cr = Cipher.getInstance("AES");
            cr.init(Cipher.ENCRYPT_MODE,key);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            CipherOutputStream out = new CipherOutputStream(bout,cr);
            byte[] data = str.getBytes();
            out.write(data);
            out.close();
            byte[] encData = bout.toByteArray();
            return ENCRYPTED_TAG+DatatypeConverter.printBase64Binary(encData);
        }catch(Exception err){
            LOG.error("Failed to encrypt....",err);
        }
        return null;
    }

    public static String decrypt(String encStr){
        if(!encStr.startsWith(ENCRYPTED_TAG)){
            return encStr;
        }
        try{
            Cipher cr = Cipher.getInstance("AES");
            cr.init(Cipher.DECRYPT_MODE,key);
            byte encData[] = DatatypeConverter.parseBase64Binary(encStr.substring(ENCRYPTED_TAG.length()));
            ByteArrayInputStream bin = new ByteArrayInputStream(encData);
            CipherInputStream in = new CipherInputStream(bin, cr);
            byte data[] = new byte[encStr.length()];
            in.read(data);
            in.close();
            return new String(data).trim();
        }catch(Exception err){
            LOG.error("Failed To Decrypt.",err);
        }
        return null;
    }

    private static String loadFileContent(String fileName){
        try{
            File f = new File(fileName);
            byte data[] = new byte[(int)f.length()];
            FileInputStream in = new FileInputStream(f);
            in.read(data);
            in.close();
            return new String(data);
        }catch(Exception err){
            return null;
        }
    }

    public static void encryptCredentialAttributes(String filename){
        String originalData = loadFileContent(filename);
        String lowerCaseData = originalData.toLowerCase();
        boolean encryptedAValue = false;

        for(TAG t:tagsToEncrypt){
            t.reset();
            String data = t.next(lowerCaseData, originalData);
            while(data!=null){
                if(data.startsWith(ENCRYPTED_TAG)){
                    data = t.next(lowerCaseData, originalData);
                }else{
                    encryptedAValue = true;
                    String eData = encrypt(data);
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(originalData.substring(0,t.x2+1));
                    buffer.append(eData);
                    buffer.append(originalData.substring(t.y1));
                    originalData = buffer.toString();
                    lowerCaseData = originalData.toLowerCase();
                    data = t.next(lowerCaseData, originalData);
                }
            }
        }
        if(encryptedAValue){
            try{
                FileOutputStream out = new FileOutputStream(filename);
                out.write(originalData.getBytes());
                out.close();
            }catch(Exception err){
                LOG.error("Failed to encrypt Credential Attributes",err);
            }
        }
    }

    public static StreamSource decryptCredentialAttributes(String filename){
        String originalData = loadFileContent(filename);

        int index = originalData.indexOf(ENCRYPTED_TAG);
        while(index!=-1){
            int index1 = originalData.indexOf("<",index);
            String eData = originalData.substring(index,index1);
            String data = decrypt(eData);
            StringBuffer buffer = new StringBuffer();
            buffer.append(originalData.substring(0,index));
            buffer.append(data);
            buffer.append(originalData.substring(index1));
            originalData = buffer.toString();
            index = originalData.indexOf(ENCRYPTED_TAG);
        }
        return new StreamSource(new ByteArrayInputStream(originalData.getBytes()));
    }

    private static class TAG {
        private String startTag = null;
        private int x1 = -1;
        private int x2 = -1;
        private int y1 = -1;

        public TAG(String _tag){
            this.startTag="<"+_tag;
        }

        public void reset(){
            this.x1 = -1;
            this.x2 = -1;
            this.y1 = -1;
        }

        public String next(String lowerCase,String originalTXT){
            x1 = lowerCase.indexOf(startTag,x1+1);
            if(x1==-1)
                return null;
            x2 = lowerCase.indexOf(">",x1);
            if(x2==-1)
                return null;
            y1 = lowerCase.indexOf("<",x1+1);
            if(y1==-1 || y1<x2)
                return null;
            return originalTXT.substring(x2+1,y1);
        }
    }

    private static class StubObjectInputStream extends ObjectInputStream{
        public StubObjectInputStream(InputStream in) throws IOException{
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc)throws IOException, ClassNotFoundException {
            try{
                return super.resolveClass(desc);
            }catch(Exception err){
                try{
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(128);
                    SecretKey sk = keyGen.generateKey();
                    return sk.getClass();
                }catch(Exception er){}
                throw err;
            }
        }
    }
}
