package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.util.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.parsers.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.rest.impl.UnsupportedFormatException;
import org.opendaylight.controller.sal.rest.impl.XmlReader;
import org.opendaylight.controller.sal.restconf.impl.*;
import org.opendaylight.controller.sal.restconf.impl.json.to.cnsn.test.JsonToCnSnTest;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;

public final class TestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

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

    public static Document loadDocumentFrom(InputStream inputStream) {
        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            return docBuilder.parse(inputStream);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            LOG.error("Error during loading Document from XML", e);
            return null;
        }
    }

    public static String getDocumentInPrintableForm(Document doc) {
        Preconditions.checkNotNull(doc);
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
            LOG.error(msg, e);
            return msg;
        }

    }

    /**
     * 
     * Fill missing data (namespaces) and build correct data type in
     * {@code compositeNode} according to {@code dataSchemaNode}. The method
     * {@link RestconfImpl#createConfigurationData createConfigurationData} is
     * used because it contains calling of method {code normalizeNode}
     */
    public static void normalizeCompositeNode(CompositeNode compositeNode, Set<Module> modules, String schemaNodePath) {
        RestconfImpl restconf = RestconfImpl.getInstance();
        ControllerContext.getInstance().setSchemas(TestUtils.loadSchemaContext(modules));

        prepareMockForRestconfBeforeNormalization(modules, restconf);
        restconf.createConfigurationData(schemaNodePath, compositeNode);
    }

    public static Module resolveModule(String searchedModuleName, Set<Module> modules) {
        assertNotNull("modules can't be null.", modules);
        if (searchedModuleName != null) {
            for (Module m : modules) {
                if (m.getName().equals(searchedModuleName)) {
                    return m;
                }
            }
        } else if (modules.size() == 1) {
            return modules.iterator().next();
        }
        return null;
    }

    public static Set<Module> resolveModulesFrom(String yangPath) {
        try {
            return TestUtils.loadModules(TestUtils.class.getResource(yangPath).getPath());
        } catch (FileNotFoundException e) {
            LOG.error("Yang files at path: " + yangPath + " weren't loaded.");
        }

        return null;
    }

    public static DataSchemaNode resolveDataSchemaNode(String searchedDataSchemaName, Module module) {
        assertNotNull("Module can't be null", module);

        if (searchedDataSchemaName != null) {
            for (DataSchemaNode dsn : module.getChildNodes()) {
                if (dsn.getQName().getLocalName().equals(searchedDataSchemaName)) {
                    return dsn;
                }
            }
        } else if (module.getChildNodes().size() == 1) {
            return module.getChildNodes().iterator().next();
        }
        return null;
    }

    public static CompositeNode loadCompositeNode(String xmlDataPath) {
        return loadCompositeNode(xmlDataPath, false);
    }

    public static CompositeNode loadCompositeNode(String xmlDataPath, boolean addDumyNamespace) {
        InputStream xmlStream = TestUtils.class.getResourceAsStream(xmlDataPath);
        CompositeNode compositeNode = null;
        try {
            XmlReader xmlReader = new XmlReader();
            compositeNode = xmlReader.read(xmlStream);
        } catch (UnsupportedFormatException | XMLStreamException e) {
            LOG.error(e.getMessage());
        }
        if (addDumyNamespace) {
            try {
                addDummyNamespaceToAllNodes((CompositeNodeWrapper) compositeNode);
            } catch (URISyntaxException e) {
                LOG.error(e.getMessage());
            }
        }
        return compositeNode;
    }

    public static QName buildQName(String name, String uri, String date, String prefix) {
        try {
            URI u = new URI(uri);
            Date dt = null;
            if (date != null) {
                dt = Date.valueOf(date);
            }
            return new QName(u, dt, prefix, name);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static QName buildQName(String name, String uri, String date) {
        return buildQName(name, uri, date, null);
    }

    public static QName buildQName(String name) {
        return buildQName(name, "", null);
    }

    public static DataSchemaNode obtainSchemaFromYang(String yangFolder) throws FileNotFoundException {
        return obtainSchemaFromYang(yangFolder, null);
    }

    public static DataSchemaNode obtainSchemaFromYang(String yangFolder, String moduleName)
            throws FileNotFoundException {
        Set<Module> modules = null;
        modules = TestUtils.loadModules(TestUtils.class.getResource(yangFolder).getPath());

        if (modules == null) {
            return null;
        }
        if (modules.size() < 1) {
            return null;
        }

        Module moduleRes = null;
        if (modules.size() > 1) {
            if (moduleName == null) {
                return null;
            } else {
                for (Module module : modules) {
                    if (module.getName().equals(moduleName)) {
                        moduleRes = module;
                    }
                }
                if (moduleRes == null) {
                    return null;
                }
            }
        } else {
            moduleRes = modules.iterator().next();
        }

        if (moduleRes.getChildNodes() == null) {
            return null;
        }

        if (moduleRes.getChildNodes().size() != 1) {
            return null;
        }
        DataSchemaNode dataSchemaNode = moduleRes.getChildNodes().iterator().next();
        return dataSchemaNode;
    }

    public static void addDummyNamespaceToAllNodes(NodeWrapper<?> wrappedNode) throws URISyntaxException {
        wrappedNode.setNamespace(new URI(""));
        if (wrappedNode instanceof CompositeNodeWrapper) {
            for (NodeWrapper<?> childNodeWrapper : ((CompositeNodeWrapper) wrappedNode).getValues()) {
                addDummyNamespaceToAllNodes(childNodeWrapper);
            }
        }
    }

    private static void prepareMockForRestconfBeforeNormalization(Set<Module> modules, RestconfImpl restconf) {

        ControllerContext controllerContext = ControllerContext.getInstance();
        BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);

        controllerContext.setSchemas(TestUtils.loadSchemaContext(modules));

        when(mockedBrokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(
                        new DummyFuture.Builder().rpcResult(
                                new DummyRpcResult.Builder<TransactionStatus>().result(TransactionStatus.COMMITED)
                                        .build()).build());

        restconf.setControllerContext(ControllerContext.getInstance());
        restconf.setBroker(mockedBrokerFacade);
    }

    public static CompositeNode readInputToCnSn(String jsonPath, boolean dummyNamespaces,
            MessageBodyReader<CompositeNode> reader) throws WebApplicationException {

        InputStream jsonStream = JsonToCnSnTest.class.getResourceAsStream(jsonPath);
        try {
            CompositeNode compositeNode = reader.readFrom(null, null, null, null, null, jsonStream);
            assertTrue(compositeNode instanceof CompositeNodeWrapper);
            if (dummyNamespaces) {
                try {
                    TestUtils.addDummyNamespaceToAllNodes((CompositeNodeWrapper) compositeNode);
                    return ((CompositeNodeWrapper) compositeNode).unwrap();
                } catch (URISyntaxException e) {
                    LOG.error(e.getMessage());
                    assertTrue(e.getMessage(), false);
                }
            }
            return compositeNode;
        } catch (IOException e) {
            LOG.error(e.getMessage());
            assertTrue(e.getMessage(), false);
        }
        return null;
    }

    public static String writeCompNodeWithSchemaContextToOutput(CompositeNode compositeNode, Set<Module> modules,
            DataSchemaNode dataSchemaNode, MessageBodyWriter<StructuredData> messageBodyWriter) throws IOException,
            WebApplicationException {

        assertNotNull(dataSchemaNode);
        assertNotNull("Composite node can't be null", compositeNode);
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

        ControllerContext.getInstance().setSchemas(loadSchemaContext(modules));

        messageBodyWriter.writeTo(new StructuredData(compositeNode, dataSchemaNode), null, null, null, null, null,
                byteArrayOS);

        return byteArrayOS.toString();

    }

}
