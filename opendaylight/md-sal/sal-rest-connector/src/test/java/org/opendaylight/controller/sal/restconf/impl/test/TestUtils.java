/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.NodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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

    private static Set<Module> loadModules(String resourceDirectory) throws FileNotFoundException {
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

    public static Set<Module> loadModulesFrom(String yangPath) {
        try {
            return TestUtils.loadModules(TestUtils.class.getResource(yangPath).getPath());
        } catch (FileNotFoundException e) {
            LOG.error("Yang files at path: " + yangPath + " weren't loaded.");
        }

        return null;
    }

    public static SchemaContext loadSchemaContext(Set<Module> modules) {
        return parser.resolveSchemaContext(modules);
    }

    public static SchemaContext loadSchemaContext(String resourceDirectory) throws FileNotFoundException {
        return parser.resolveSchemaContext(loadModulesFrom(resourceDirectory));
    }

    public static Module findModule(Set<Module> modules, String moduleName) {
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
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

        prepareMocksForRestconf(modules, restconf);
        restconf.updateConfigurationData(schemaNodePath, compositeNode);
    }

    /**
     * Searches module with name {@code searchedModuleName} in {@code modules}.
     * If module name isn't specified and module set has only one element then
     * this element is returned.
     * 
     */
    public static Module resolveModule(String searchedModuleName, Set<Module> modules) {
        assertNotNull("Modules can't be null.", modules);
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

    private static void addDummyNamespaceToAllNodes(NodeWrapper<?> wrappedNode) throws URISyntaxException {
        wrappedNode.setNamespace(new URI(""));
        if (wrappedNode instanceof CompositeNodeWrapper) {
            for (NodeWrapper<?> childNodeWrapper : ((CompositeNodeWrapper) wrappedNode).getValues()) {
                addDummyNamespaceToAllNodes(childNodeWrapper);
            }
        }
    }

    private static void prepareMocksForRestconf(Set<Module> modules, RestconfImpl restconf) {
        ControllerContext controllerContext = ControllerContext.getInstance();
        BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);

        controllerContext.setSchemas(TestUtils.loadSchemaContext(modules));

        when(mockedBrokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(
                        new DummyFuture.Builder().rpcResult(
                                new DummyRpcResult.Builder<TransactionStatus>().result(TransactionStatus.COMMITED)
                                        .build()).build());

        restconf.setControllerContext(controllerContext);
        restconf.setBroker(mockedBrokerFacade);
    }

    public static CompositeNode readInputToCnSn(String path, boolean dummyNamespaces,
            MessageBodyReader<CompositeNode> reader) throws WebApplicationException {

        InputStream inputStream = TestUtils.class.getResourceAsStream(path);
        try {
            CompositeNode compositeNode = reader.readFrom(null, null, null, null, null, inputStream);
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

    public static CompositeNode readInputToCnSn(String path, MessageBodyReader<CompositeNode> reader) {
        return readInputToCnSn(path, false, reader);
    }

    public static String writeCompNodeWithSchemaContextToOutput(CompositeNode compositeNode, Set<Module> modules,
            DataSchemaNode dataSchemaNode, MessageBodyWriter<StructuredData> messageBodyWriter) throws IOException,
            WebApplicationException {

        assertNotNull(dataSchemaNode);
        assertNotNull("Composite node can't be null", compositeNode);
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

        ControllerContext.getInstance().setSchemas(loadSchemaContext(modules));

        messageBodyWriter.writeTo(new StructuredData(compositeNode, dataSchemaNode, null), null, null, null, null, null,
                byteArrayOS);

        return byteArrayOS.toString();
    }

    public static String loadTextFile(String filePath) throws IOException {
        FileReader fileReader = new FileReader(filePath);
        BufferedReader bufReader = new BufferedReader(fileReader);

        String line = null;
        StringBuilder result = new StringBuilder();
        while ((line = bufReader.readLine()) != null) {
            result.append(line);
        }
        bufReader.close();
        return result.toString();

    }
}
