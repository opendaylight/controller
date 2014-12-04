/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.composite.node.schema.cnsn.parser.CnSnToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class TestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

    private final static YangContextParser parser = new YangParserImpl();

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
     * Fill missing data (namespaces) and build correct data type in {@code compositeNode} according to
     * {@code dataSchemaNode}. The method {@link RestconfImpl#createConfigurationData createConfigurationData} is used
     * because it contains calling of method {code normalizeNode}
     */
    public static void normalizeCompositeNode(Node<?> node, Set<Module> modules, String schemaNodePath) {
        RestconfImpl restconf = RestconfImpl.getInstance();
        ControllerContext.getInstance().setSchemas(TestUtils.loadSchemaContext(modules));

        prepareMocksForRestconf(modules, restconf);
        restconf.updateConfigurationData(schemaNodePath, node);
    }

    /**
     * Searches module with name {@code searchedModuleName} in {@code modules}. If module name isn't specified and
     * module set has only one element then this element is returned.
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

        when(mockedBrokerFacade.commitConfigurationDataPut(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(mock(CheckedFuture.class));

        restconf.setControllerContext(controllerContext);
        restconf.setBroker(mockedBrokerFacade);
    }

    public static Node<?> readInputToCnSn(String path, boolean dummyNamespaces,
            MessageBodyReader<Node<?>> reader) throws WebApplicationException {

        InputStream inputStream = TestUtils.class.getResourceAsStream(path);
        try {
            final Node<?> node = reader.readFrom(null, null, null, null, null, inputStream);
            assertTrue(node instanceof CompositeNodeWrapper);
            if (dummyNamespaces) {
                try {
                    TestUtils.addDummyNamespaceToAllNodes((CompositeNodeWrapper) node);
                    return ((CompositeNodeWrapper) node).unwrap();
                } catch (URISyntaxException e) {
                    LOG.error(e.getMessage());
                    assertTrue(e.getMessage(), false);
                }
            }
            return node;
        } catch (IOException e) {
            LOG.error(e.getMessage());
            assertTrue(e.getMessage(), false);
        }
        return null;
    }

//    public static Node<?> readInputToCnSnNew(String path, MessageBodyReader<Node<?>> reader) throws WebApplicationException {
//        InputStream inputStream = TestUtils.class.getResourceAsStream(path);
//        try {
//            return reader.readFrom(null, null, null, null, null, inputStream);
//        } catch (IOException e) {
//            LOG.error(e.getMessage());
//            assertTrue(e.getMessage(), false);
//        }
//        return null;
//    }

    public static Node<?> readInputToCnSn(String path, MessageBodyReader<Node<?>> reader) {
        return readInputToCnSn(path, false, reader);
    }

    public static String writeCompNodeWithSchemaContextToOutput(Node<?> node, Set<Module> modules,
            DataSchemaNode dataSchemaNode, MessageBodyWriter<StructuredData> messageBodyWriter) throws IOException,
            WebApplicationException {

        assertNotNull(dataSchemaNode);
        assertNotNull("Composite node can't be null", node);
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

        ControllerContext.getInstance().setSchemas(loadSchemaContext(modules));

        assertTrue(node instanceof CompositeNode);
        messageBodyWriter.writeTo(new StructuredData((CompositeNode)node, dataSchemaNode, null), null, null, null, null,
                null, byteArrayOS);

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

    private static Pattern patternForStringsSeparatedByWhiteChars(String... substrings) {
        StringBuilder pattern = new StringBuilder();
        pattern.append(".*");
        for (String substring : substrings) {
            pattern.append(substring);
            pattern.append("\\s*");
        }
        pattern.append(".*");
        return Pattern.compile(pattern.toString(), Pattern.DOTALL);
    }

    public static boolean containsStringData(String jsonOutput, String... substrings) {
        Pattern pattern = patternForStringsSeparatedByWhiteChars(substrings);
        Matcher matcher = pattern.matcher(jsonOutput);
        return matcher.matches();
    }

    public static NormalizedNode compositeNodeToDatastoreNormalizedNode(final CompositeNode compositeNode,
            final DataSchemaNode schema) {
        List<Node<?>> lst = new ArrayList<Node<?>>();
        lst.add(compositeNode);
        if (schema instanceof ContainerSchemaNode) {
            return CnSnToNormalizedNodeParserFactory.getInstance().getContainerNodeParser()
                    .parse(lst, (ContainerSchemaNode) schema);
        } else if (schema instanceof ListSchemaNode) {
            return CnSnToNormalizedNodeParserFactory.getInstance().getMapNodeParser()
                    .parse(lst, (ListSchemaNode) schema);
        }

        LOG.error("Top level isn't of type container, list, leaf schema node but " + schema.getClass().getSimpleName());

        throw new RestconfDocumentedException(new RestconfError(ErrorType.APPLICATION, ErrorTag.INVALID_VALUE,
                "It wasn't possible to translate specified data to datastore readable form."));
    }

    public static YangInstanceIdentifier.NodeIdentifier getNodeIdentifier(String localName, String namespace,
            String revision) throws ParseException {
        return new YangInstanceIdentifier.NodeIdentifier(QName.create(namespace, revision, localName));
    }

    public static YangInstanceIdentifier.NodeIdentifierWithPredicates getNodeIdentifierPredicate(String localName,
            String namespace, String revision, Map<String, Object> keys) throws ParseException {
        Map<QName, Object> predicate = new HashMap<>();
        for (String key : keys.keySet()) {
            predicate.put(QName.create(namespace, revision, key), keys.get(key));
        }

        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(

        QName.create(namespace, revision, localName), predicate);
    }

    public static YangInstanceIdentifier.NodeIdentifierWithPredicates getNodeIdentifierPredicate(String localName,
            String namespace, String revision, String... keysAndValues) throws ParseException {
        java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(revision);
        if (keysAndValues.length % 2 != 0) {
            new IllegalArgumentException("number of keys argument have to be divisible by 2 (map)");
        }
        Map<QName, Object> predicate = new HashMap<>();

        int i = 0;
        while (i < keysAndValues.length) {
            predicate.put(QName.create(namespace, revision, keysAndValues[i++]), keysAndValues[i++]);
        }

        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(QName.create(namespace, revision, localName),
                predicate);
    }

    public static CompositeNode prepareCompositeNodeWithIetfInterfacesInterfacesData() {
        CompositeNodeBuilder<ImmutableCompositeNode> interfaceBuilder = ImmutableCompositeNode.builder();
        interfaceBuilder.addLeaf(buildQName("name", "dummy", "2014-07-29"), "eth0");
        interfaceBuilder.addLeaf(buildQName("type", "dummy", "2014-07-29"), "ethernetCsmacd");
        interfaceBuilder.addLeaf(buildQName("enabled", "dummy", "2014-07-29"), "false");
        interfaceBuilder.addLeaf(buildQName("description", "dummy", "2014-07-29"), "some interface");
        return interfaceBuilder.toInstance();
    }

    static NormalizedNode<?,?> prepareNormalizedNodeWithIetfInterfacesInterfacesData() throws ParseException {
        String ietfInterfacesDate = "2013-07-04";
        String namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces";
        DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> mapEntryNode = ImmutableMapEntryNodeBuilder.create();

        Map<String, Object> predicates = new HashMap<>();
        predicates.put("name", "eth0");

        mapEntryNode.withNodeIdentifier(getNodeIdentifierPredicate("interface", namespace, ietfInterfacesDate,
                predicates));
        mapEntryNode
                .withChild(new ImmutableLeafNodeBuilder<String>()
                        .withNodeIdentifier(getNodeIdentifier("name", namespace, ietfInterfacesDate)).withValue("eth0")
                        .build());
        mapEntryNode.withChild(new ImmutableLeafNodeBuilder<String>()
                .withNodeIdentifier(getNodeIdentifier("type", namespace, ietfInterfacesDate))
                .withValue("ethernetCsmacd").build());
        mapEntryNode.withChild(new ImmutableLeafNodeBuilder<Boolean>()
                .withNodeIdentifier(getNodeIdentifier("enabled", namespace, ietfInterfacesDate))
                .withValue(Boolean.FALSE).build());
        mapEntryNode.withChild(new ImmutableLeafNodeBuilder<String>()
                .withNodeIdentifier(getNodeIdentifier("description", namespace, ietfInterfacesDate))
                .withValue("some interface").build());

        return mapEntryNode.build();
    }

    static void compareInstanceIdentifier(String uri, SchemaContext schemaContext, Object... expPath) {
        InstanceIdentifierContext normalizedIIContext = ControllerContext.getInstance().toInstanceIdentifier(uri);
        YangInstanceIdentifier normalizedIINew = normalizedIIContext.getInstanceIdentifier();

        for (int i = 0; i < expPath.length; i++) {
            PathArgument actualArg = Iterables.get(normalizedIINew.getPathArguments(), i);
            if (expPath[i] instanceof Object[]) { // NodeIdentifierWithPredicates
                Object[] exp = (Object[]) expPath[i];
                assertTrue("Odd number of element expected.", exp.length % 2 == 1);

                assertEquals("Actual path arg " + (i + 1) + " class", NodeIdentifierWithPredicates.class,
                        actualArg.getClass());
                NodeIdentifierWithPredicates actualNode = (NodeIdentifierWithPredicates) actualArg;
                assertEquals("Missing key parts.", (exp.length - 1) / 2, actualNode.getKeyValues().size());
                Map<QName, Object> keyValues = actualNode.getKeyValues();
                assertEquals("Actual path arg " + (i + 1) + " node type", exp[0], actualNode.getNodeType());

                int j = 1;
                while (j < exp.length) {
                    Object keyValue = keyValues.get(exp[j]);
                    assertNotNull("Key " + exp[j] + " wasn't found.", keyValue);
                    j++;
                    assertEquals("Value of key " + exp[j - 1], exp[j], keyValue);
                    j++;
                }
            } else if (expPath[i] instanceof Set) { // AugmentationIdentifier
                assertEquals("Actual path arg " + (i + 1) + " class", AugmentationIdentifier.class,
                        actualArg.getClass());
                AugmentationIdentifier actualNode = (AugmentationIdentifier) actualArg;
                assertEquals("Actual path arg " + (i + 1) + " PossibleChildNames", expPath[i],
                        actualNode.getPossibleChildNames());
            } else {
                assertEquals("Actual path arg " + (i + 1) + " node type", expPath[i], actualArg.getNodeType());
            }
        }
    }

}
