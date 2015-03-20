package org.opendaylight.controller.sal.restconf.impl.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class YangCoverage {

    public YangCoverage() throws URISyntaxException {

        validateFiles();
    }

    private static ArrayList<File> yangFiles = new ArrayList<File>();

    @BeforeClass
    public static void init() {

    }

    private static void validateFiles() throws URISyntaxException {

        File thisDir = new File(YangTest.class.getResource("/yang").toURI());

        traverseDir(thisDir);

        for (File file : yangFiles) {
            try {
                // System.out.println(file);
                validateFile(file);
            } catch (IOException | YangSyntaxErrorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static void validateFile(File resourceFile) throws IOException, YangSyntaxErrorException {

        File resourceDir = resourceFile.getParentFile();

        YangParserImpl parser = YangParserImpl.getInstance();
        parser.parseFile(resourceFile, resourceDir);
    }

    public static void traverseDir(File file) {

        if (getFileExtension(file).equals("yang"))
            yangFiles.add(file);

        if (file.isDirectory()) {
            String entries[] = file.list();

            if (entries != null) {
                for (String entry : entries) {
                    traverseDir(new File(file, entry));
                }
            }
        }
    }

    private static String getFileExtension(File file) {

        String fileName = file.getName();

        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else
            return "";
    }

    @Test
    public void test() {

    }
}