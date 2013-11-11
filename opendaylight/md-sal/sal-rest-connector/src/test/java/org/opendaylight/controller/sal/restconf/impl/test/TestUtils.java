package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.controller.sal.rest.impl.*;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.XmlTreeBuilder;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

final class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    private final static YangModelParser parser = new YangParserImpl();

    public static Set<Module> loadModules(String resourceDirectory) throws FileNotFoundException {
        final File testDir = new File(resourceDirectory);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<File>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory);
        }
        for (int i = 0; i < fileList.length; i++) {
            String fileName = fileList[i];
            if (new File(testDir, fileName).isDirectory() == false) {
                testFiles.add(new File(testDir, fileName));
            }
        }
        return parser.parseYangModels(testFiles);
    }

    public static SchemaContext loadSchemaContext(Set<Module> modules) {
        return parser.resolveSchemaContext(modules);
    }

    public static SchemaContext loadSchemaContext(String resourceDirectory) throws FileNotFoundException {
        return parser.resolveSchemaContext(loadModules(resourceDirectory));
    }

    public static Module findModule(Set<Module> modules, String moduleName) {
        Module result = null;
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                result = module;
                break;
            }
        }
        return result;
    }

    public static CompositeNode loadCompositeNode(InputStream xmlInputStream) throws FileNotFoundException {
        if (xmlInputStream == null) {
            throw new IllegalArgumentException();
        }
        Node<?> dataTree;
        try {
            dataTree = XmlTreeBuilder.buildDataTree(xmlInputStream);
        } catch (XMLStreamException e) {
            logger.error("Error during building data tree from XML", e);
            return null;
        }
        if (dataTree == null) {
            logger.error("data tree is null");
            return null;
        }
        if (dataTree instanceof SimpleNode) {
            logger.error("RPC XML was resolved as SimpleNode");
            return null;
        }
        return (CompositeNode) dataTree;
    }

    public static String getDocumentInPrintableForm(Document doc) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
            byte[] charData = out.toByteArray();
            return new String(charData, "UTF-8");
        } catch (IOException | TransformerException e) {
            String msg = "Error during transformation of Document into String";
            logger.error(msg, e);
            return msg;
        }

    }

    static String convertXmlDataAndYangToJson(String xmlDataPath, String yangPath, String outputPath) {
        String jsonResult = null;
        Set<Module> modules = null;

        try {
            modules = TestUtils.loadModules(YangAndXmlToJsonBasicDataTypesTest.class.getResource(yangPath).getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assertNotNull("modules can't be null.", modules);

        InputStream xmlStream = YangAndXmlToJsonBasicDataTypesTest.class.getResourceAsStream(xmlDataPath);
        CompositeNode compositeNode = null;
        try {
            compositeNode = TestUtils.loadCompositeNode(xmlStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assertNotNull("Composite node can't be null", compositeNode);

        StructuredDataToJsonProvider structuredDataToJsonProvider = StructuredDataToJsonProvider.INSTANCE;
        for (Module module : modules) {
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            for (DataSchemaNode dataSchemaNode : module.getChildNodes()) {
                StructuredData structuredData = new StructuredData(compositeNode, dataSchemaNode);
                try {
                    structuredDataToJsonProvider.writeTo(structuredData, null, null, null, null, null, byteArrayOS);
                } catch (WebApplicationException | IOException e) {
                    e.printStackTrace();
                }
                assertFalse(
                        "Returning JSON string can't be empty for node " + dataSchemaNode.getQName().getLocalName(),
                        byteArrayOS.toString().isEmpty());
            }
            jsonResult = byteArrayOS.toString();
            try {
                outputToFile(byteArrayOS, outputPath);
            } catch (IOException e) {
                System.out.println("Output file wasn't cloased sucessfuly.");
            }

        }
        return jsonResult;
    }

    static void outputToFile(ByteArrayOutputStream outputStream, String outputDir) throws IOException {
        FileOutputStream fileOS = null;
        try {
            String path = YangAndXmlToJsonBasicDataTypesTest.class.getResource(outputDir).getPath();
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

    static String readJsonFromFile(String path, boolean removeWhiteChars) {
        FileReader fileReader = getFileReader(path);

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
        }
        String rawStr = strBuilder.toString();
        if (removeWhiteChars) {
            rawStr = rawStr.replace("\n", "");
            rawStr = rawStr.replace("\r", "");
            rawStr = rawStr.replace("\t", "");
            rawStr = removeSpaces(rawStr);
        }

        return rawStr;
    }

    private static FileReader getFileReader(String path) {
        String fullPath = YangAndXmlToJsonBasicDataTypesTest.class.getResource(path).getPath();
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
        return fileReader;
    }

    private static String removeSpaces(String rawStr) {
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

}
