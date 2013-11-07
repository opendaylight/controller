package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Set;
import java.util.regex.*;

import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class YangAndXmlToJsonConversion {

    @Ignore
    @Test
    /**
     * Test for simple yang types (leaf, list, leaf-list, container and various combination of them)
     * 
     */
    public void simpleYangTypesTest() {
        String jsonOutput = null;

        jsonOutput = convertXmlDataAndYangToJson("/yang-to-json-conversion/simple-yang-types/xml/data.xml",
                "/yang-to-json-conversion/simple-yang-types");

//         jsonOutput =
//         readJsonFromFile("/yang-to-json-conversion/simple-yang-types/xml/output.json");

        verifyJsonOutputForSimpleYangTypes(jsonOutput);

    }

    private void verifyJsonOutputForSimpleYangTypes(String jsonOutput) {

        assertTrue("First and last character has to be '{' and '}'", Pattern.compile("\\A\\{.*\\}\\z", Pattern.DOTALL)
                .matcher(jsonOutput).matches());

        String prefix = "\"(smptp:|)";

        // subnodes of cont1
        String cont1 = prefix + "cont1\":\\{";
        testLeaf(cont1, "lf11", new String("lf"), jsonOutput, prefix);
        testLeafList(cont1, "lflst11", jsonOutput, prefix, new Integer(55), new Integer(56), new Integer(57));
        testLeafList(cont1, "lflst12", jsonOutput, prefix, new String("lflst12 str1"), new String("lflst12 str2"),
                new String("lflst12 str1"));

        // subnodes of lst111
        // first object of lst111
        String lst11 = cont1 + ".*" + prefix + "lst11\":\\[\\{";
        testLeaf(lst11, "lf111", new Integer(140), jsonOutput, prefix);
        testLeaf(lst11, "lf112", new String("lf112 str"), jsonOutput, prefix);

        // subnodes of cont111
        String cont111 = lst11 + ".*" + prefix + "cont111\":\\{";
        testLeaf(cont111, "lf1111", new String("lf1111 str"), jsonOutput, prefix);
        testLeafList(cont1, "lflst1111", jsonOutput, prefix, new Integer(2048), new Integer(1024), new Integer(4096));

        // subnodes of lst1111
        String lst1111 = cont111 + ".*" + prefix + "lst1111\":\\[\\{";
        testLeaf(lst1111, "lf1111A", new String("lf1111A str11"), jsonOutput, prefix);
        testLeaf(lst1111, "lf1111B", new Integer(4), jsonOutput, prefix);
        testLeaf(lst1111, "lf1111A", new String("lf1111A str12"), jsonOutput, prefix);
        testLeaf(lst1111, "lf1111B", new Integer(7), jsonOutput, prefix);
        // :subnodes of lst1111
        // :subnodes of cont111
        // :first object of lst111

        // second object of lst111
        testLeaf(lst11, "lf111", new Integer(141), jsonOutput, prefix);
        testLeaf(lst11, "lf112", new String("lf112 str2"), jsonOutput, prefix);

        // subnodes of cont111
        testLeaf(cont111, "lf1111", new String("lf1111 str2"), jsonOutput, prefix);
        testLeafList(cont1, "lflst1111", jsonOutput, prefix, new Integer(2049), new Integer(1025), new Integer(4097));

        // subnodes of lst1111
        testLeaf(lst1111, "lf1111A", new String("lf1111A str21"), jsonOutput, prefix);
        testLeaf(lst1111, "lf1111B", new Integer(5), jsonOutput, prefix);
        testLeaf(lst1111, "lf1111A", new String("lf1111A str22"), jsonOutput, prefix);
        testLeaf(lst1111, "lf1111B", new Integer(8), jsonOutput, prefix);
        // :subnodes of lst111
        // :subnodes of cont111
        // :second object of lst111
        // :second object of lst111
        // :subnodes of cont1
    }

    private void testLeaf(String prevRegEx, String lstName, Object value, String jsonFile, String elementPrefix) {
        String newValue = null;
        if (value instanceof Integer) {
            newValue = value.toString();
        } else if (value instanceof String) {
            newValue = "\"" + value.toString() + "\"";
        }
        String lf = elementPrefix + lstName + "\":" + newValue;
        assertTrue(">>\"" + lstName + "\":" + newValue + "<< is missing",
                Pattern.compile(".*" + prevRegEx + ".*" + lf + ".*", Pattern.DOTALL).matcher(jsonFile).matches());

    }

    private void testLeafList(String prevRegEx, String lflstName, String jsonFile, String elementPrefix,
            Object... dataInList) {
        // order of element in the list isn't defined :(
        String lflstBegin = elementPrefix + lflstName + "\":\\[";
        String lflstEnd = ".*\\],";
        assertTrue(
                ">>\"" + lflstName + "\": [],<< is missing",
                Pattern.compile(".*" + prevRegEx + ".*" + lflstBegin + lflstEnd + ".*", Pattern.DOTALL)
                        .matcher(jsonFile).matches());

        for (Object obj : dataInList) {
            testValueOfLeafList(prevRegEx, lflstBegin, obj, jsonFile);
        }
    }

    private void testValueOfLeafList(String prevRegEx, String lflstPrevRegEx, Object value, String jsonFile) {
        String lflstData = null;
        lflstData = regExForDataValueInList(value);
        assertNotNull(lflstPrevRegEx + ": value doesn't have correct type.", lflstData);
        assertTrue(
                prevRegEx + ": data value >" + value + "< is missing.",
                Pattern.compile(".*" + prevRegEx + ".*" + lflstPrevRegEx + lflstData + ".*", Pattern.DOTALL)
                        .matcher(jsonFile).matches());

    }

    /**
     * Data value can be first, inner, last or only one member of list
     * 
     * @param dataValue
     * @return
     */
    private String regExForDataValueInList(Object dataValue) {
        String newDataValue;
        if (dataValue instanceof Integer) {
            newDataValue = dataValue.toString();
            return "(" + newDataValue + "(,[0-9]+)+|([0-9]+,)+" + newDataValue + "(,[0-9]+)+|([0-9]+,)+" + newDataValue
                    + "|" + newDataValue + ")\\]";
        } else if (dataValue instanceof String) {
            newDataValue = "\"" + dataValue.toString() + "\"";
            return "(" + newDataValue + "(,\".+\")+|(\".+\",)+" + newDataValue + "(,\".+\")+|(\".+\",)+" + newDataValue
                    + "|" + newDataValue + ")\\]";
        }
        return null;
    }

    private String readJsonFromFile(String path) {
        String fullPath = YangAndXmlToJsonConversion.class.getResource(path).getPath();
        assertNotNull("Path to file can't be null.", fullPath);
        File file = new File(fullPath);
        assertNotNull("File can't be null", file);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assertNotNull("File reader can't be null.", fileReader);

        StringBuilder strBuilder = new StringBuilder();
        char[] buffer = new char[1000];

        while (true) {
            int loadedCharNum;
            try {
                loadedCharNum = fileReader.read(buffer);
            } catch (IOException e) {
                break;
            }
            if (loadedCharNum == -1) {
                break;
            }
            strBuilder.append(buffer, 0, loadedCharNum);
        }
        try {
            fileReader.close();
        } catch (IOException e) {
            System.out.println("The file wasn't closed");
            ;
        }
        String rawStr = strBuilder.toString();
        rawStr = rawStr.replace("\n", "");
        rawStr = rawStr.replace("\r", "");
        rawStr = rawStr.replace("\t", "");
        rawStr = removeSpaces(rawStr);

        return rawStr;
    }

    private String removeSpaces(String rawStr) {
        StringBuilder strBuilder = new StringBuilder();
        int i = 0;
        int quoteCount = 0;
        while (i < rawStr.length()) {
            if (rawStr.substring(i, i + 1).equals("\"")) {
                quoteCount++;
            }

            if (!rawStr.substring(i, i + 1).equals(" ") || (quoteCount % 2 == 1)) {
                strBuilder.append(rawStr.charAt(i));
            }
            i++;
        }

        return strBuilder.toString();
    }

    private String convertXmlDataAndYangToJson(String xmlDataPath, String yangPath) {
        String jsonResult = null;
        Set<Module> modules = null;

        try {
            modules = TestUtils.loadModules(YangAndXmlToJsonConversion.class.getResource(yangPath).getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assertNotNull("modules can't be null.", modules);

        InputStream xmlStream = YangAndXmlToJsonConversion.class.getResourceAsStream(xmlDataPath);
        CompositeNode compositeNode = null;
        try {
            compositeNode = TestUtils.loadCompositeNode(xmlStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assertNotNull("Composite node can't be null", compositeNode);

        StructuredDataToJsonProvider structuredDataToJsonProvider = StructuredDataToJsonProvider.INSTANCE;
        for (Module module : modules) {
            for (DataSchemaNode dataSchemaNode : module.getChildNodes()) {
                StructuredData structuredData = new StructuredData(compositeNode, dataSchemaNode);
                ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
                try {
                    structuredDataToJsonProvider.writeTo(structuredData, null, null, null, null, null, byteArrayOS);
                } catch (WebApplicationException | IOException e) {
                    e.printStackTrace();
                }
                assertFalse(
                        "Returning JSON string can't be empty for node " + dataSchemaNode.getQName().getLocalName(),
                        byteArrayOS.toString().isEmpty());
                jsonResult = byteArrayOS.toString();
                try {
                    outputToFile(byteArrayOS);
                } catch (IOException e) {
                    System.out.println("Output file wasn't cloased sucessfuly.");
                }
            }
        }
        return jsonResult;
    }

    private void outputToFile(ByteArrayOutputStream outputStream) throws IOException {
        FileOutputStream fileOS = null;
        try {
            String path = YangAndXmlToJsonConversion.class.getResource("/yang-to-json-conversion/xml").getPath();
            File outFile = new File(path + "/data.json");
            fileOS = new FileOutputStream(outFile);
            try {
                fileOS.write(outputStream.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileOS.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }
}
