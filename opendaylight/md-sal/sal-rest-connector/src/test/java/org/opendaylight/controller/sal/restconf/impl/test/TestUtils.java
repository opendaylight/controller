/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.base.Preconditions;
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
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.composite.node.schema.cnsn.parser.CnSnToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class TestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

    private final static YangContextParser parser = new YangParserImpl();


    private static class TypeDef {
        public final TypeDefinition<? extends Object> typedef;
        public final QName qName;

        TypeDef(final TypeDefinition<? extends Object> typedef, final QName qName) {
            this.typedef = typedef;
            this.qName = qName;
        }
    }


    private static Set<Module> loadModules(final String resourceDirectory) throws FileNotFoundException {
        final File testDir = new File(resourceDirectory);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<File>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory);
        }
        for (int i = 0; i < fileList.length; i++) {
            final String fileName = fileList[i];
            if (new File(testDir, fileName).isDirectory() == false) {
                testFiles.add(new File(testDir, fileName));
            }
        }
        return parser.parseYangModels(testFiles);
    }

    public static Set<Module> loadModulesFrom(final String yangPath) {
        try {
            return TestUtils.loadModules(TestUtils.class.getResource(yangPath).getPath());
        } catch (final FileNotFoundException e) {
            LOG.error("Yang files at path: " + yangPath + " weren't loaded.");
        }

        return null;
    }

    public static SchemaContext loadSchemaContext(final Set<Module> modules) {
        return parser.resolveSchemaContext(modules);
    }

    public static SchemaContext loadSchemaContext(final String resourceDirectory) throws FileNotFoundException {
        return parser.resolveSchemaContext(loadModulesFrom(resourceDirectory));
    }

    public static Module findModule(final Set<Module> modules, final String moduleName) {
        for (final Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public static Document loadDocumentFrom(final InputStream inputStream) {
        try {
            final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            return docBuilder.parse(inputStream);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            LOG.error("Error during loading Document from XML", e);
            return null;
        }
    }

    public static String getDocumentInPrintableForm(final Document doc) {
        Preconditions.checkNotNull(doc);
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
            final byte[] charData = out.toByteArray();
            return new String(charData, "UTF-8");
        } catch (IOException | TransformerException e) {
            final String msg = "Error during transformation of Document into String";
            LOG.error(msg, e);
            return msg;
        }

    }

//    /**
//     * @deprecated method will be removed in Lithium release
//     *      we don't wish to use Node and CompositeNode anywhere -
//     *
//     * Fill missing data (namespaces) and build correct data type in {@code compositeNode} according to
//     * {@code dataSchemaNode}. The method {@link RestconfImpl#createConfigurationData createConfigurationData} is used
//     * because it contains calling of method {code normalizeNode}
//     */
//    @Deprecated
//    public static void normalizeCompositeNode(final Node<?> node, final Set<Module> modules, final String schemaNodePath) {
//        final RestconfImpl restconf = RestconfImpl.getInstance();
//        ControllerContext.getInstance().setSchemas(TestUtils.loadSchemaContext(modules));
//        prepareMocksForRestconf(modules, restconf);
//
//        final InstanceIdentifierContext iiContext = ControllerContext.getInstance().toInstanceIdentifier(schemaNodePath);
//        final DOMMountPoint mountPoint = iiContext.getMountPoint();
//        final CompositeNode value = normalizeNode(node, (DataSchemaNode) iiContext.getSchemaNode(), mountPoint);
//        final NormalizedNode<?, ?> normNodePayload = compositeNodeToDatastoreNormalizedNode(value, (DataSchemaNode) iiContext.getSchemaNode());
//        final NormalizedNodeContext normlNodeContext = new NormalizedNodeContext(iiContext, normNodePayload);
//
//        restconf.updateConfigurationData(schemaNodePath, normlNodeContext);
//    }
//
//    /**
//     * @deprecated
//     */
//    @Deprecated
//    private static void normalizeNode(final NodeWrapper<? extends Object> nodeBuilder, final DataSchemaNode schema,
//            final QName previousAugment, final DOMMountPoint mountPoint) {
//        if (schema == null) {
//            throw new RestconfDocumentedException("Data has bad format.\n\"" + nodeBuilder.getLocalName()
//                    + "\" does not exist in yang schema.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
//        }
//
//        QName currentAugment = null;
//        if (nodeBuilder.getQname() != null) {
//            currentAugment = previousAugment;
//        } else {
//            currentAugment = normalizeNodeName(nodeBuilder, schema, previousAugment, mountPoint);
//            if (nodeBuilder.getQname() == null) {
//                throw new RestconfDocumentedException(
//                        "Data has bad format.\nIf data is in XML format then namespace for \""
//                                + nodeBuilder.getLocalName() + "\" should be \"" + schema.getQName().getNamespace()
//                                + "\".\n" + "If data is in JSON format then module name for \""
//                                + nodeBuilder.getLocalName() + "\" should be corresponding to namespace \""
//                                + schema.getQName().getNamespace() + "\".", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
//            }
//        }
//
//        if (nodeBuilder instanceof CompositeNodeWrapper) {
//            if (schema instanceof DataNodeContainer) {
//                normalizeCompositeNode((CompositeNodeWrapper) nodeBuilder, (DataNodeContainer) schema, mountPoint,
//                        currentAugment);
//            } else if (schema instanceof AnyXmlSchemaNode) {
//                normalizeAnyXmlNode((CompositeNodeWrapper) nodeBuilder, (AnyXmlSchemaNode) schema);
//            }
//        } else if (nodeBuilder instanceof SimpleNodeWrapper) {
//            normalizeSimpleNode((SimpleNodeWrapper) nodeBuilder, schema, mountPoint);
//        } else if ((nodeBuilder instanceof EmptyNodeWrapper)) {
//            normalizeEmptyNode((EmptyNodeWrapper) nodeBuilder, schema);
//        }
//    }

//    @Deprecated
//    private static void normalizeCompositeNode(final CompositeNodeWrapper compositeNodeBuilder,
//            final DataNodeContainer schema, final DOMMountPoint mountPoint, final QName currentAugment) {
//        final List<NodeWrapper<?>> children = compositeNodeBuilder.getValues();
//        checkNodeMultiplicityAccordingToSchema(schema, children);
//        for (final NodeWrapper<? extends Object> child : children) {
//            final List<DataSchemaNode> potentialSchemaNodes = ControllerContext.findInstanceDataChildrenByName(
//                    schema, child.getLocalName());
//
//            if (potentialSchemaNodes.size() > 1 && child.getNamespace() == null) {
//                final StringBuilder builder = new StringBuilder();
//                for (final DataSchemaNode potentialSchemaNode : potentialSchemaNodes) {
//                    builder.append("   ").append(potentialSchemaNode.getQName().getNamespace().toString()).append("\n");
//                }
//
//                throw new RestconfDocumentedException("Node \"" + child.getLocalName()
//                        + "\" is added as augment from more than one module. "
//                        + "Therefore node must have namespace (XML format) or module name (JSON format)."
//                        + "\nThe node is added as augment from modules with namespaces:\n" + builder,
//                        ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
//            }
//
//            boolean rightNodeSchemaFound = false;
//            for (final DataSchemaNode potentialSchemaNode : potentialSchemaNodes) {
//                if (!rightNodeSchemaFound) {
//                    final QName potentialCurrentAugment = normalizeNodeName(child, potentialSchemaNode,
//                            currentAugment, mountPoint);
//                    if (child.getQname() != null) {
//                        normalizeNode(child, potentialSchemaNode, potentialCurrentAugment, mountPoint);
//                        rightNodeSchemaFound = true;
//                    }
//                }
//            }
//
//            if (!rightNodeSchemaFound) {
//                throw new RestconfDocumentedException("Schema node \"" + child.getLocalName()
//                        + "\" was not found in module.", ErrorType.APPLICATION, ErrorTag.UNKNOWN_ELEMENT);
//            }
//        }
//
//        if ((schema instanceof ListSchemaNode)) {
//            final ListSchemaNode listSchemaNode = (ListSchemaNode) schema;
//            final List<QName> listKeys = listSchemaNode.getKeyDefinition();
//            for (final QName listKey : listKeys) {
//                boolean foundKey = false;
//                for (final NodeWrapper<? extends Object> child : children) {
//                    if (Objects.equal(child.unwrap().getNodeType().getLocalName(), listKey.getLocalName())) {
//                        foundKey = true;
//                    }
//                }
//
//                if (!foundKey) {
//                    throw new RestconfDocumentedException("Missing key in URI \"" + listKey.getLocalName()
//                            + "\" of list \"" + listSchemaNode.getQName().getLocalName() + "\"", ErrorType.PROTOCOL,
//                            ErrorTag.DATA_MISSING);
//                }
//            }
//        }
//    }

//    @Deprecated
//    private static void checkNodeMultiplicityAccordingToSchema(final DataNodeContainer dataNodeContainer,
//            final List<NodeWrapper<?>> nodes) {
//        final Map<String, Integer> equalNodeNamesToCounts = new HashMap<String, Integer>();
//        for (final NodeWrapper<?> child : nodes) {
//            Integer count = equalNodeNamesToCounts.get(child.getLocalName());
//            equalNodeNamesToCounts.put(child.getLocalName(), count == null ? 1 : ++count);
//        }
//
//        for (final DataSchemaNode childSchemaNode : dataNodeContainer.getChildNodes()) {
//            if (childSchemaNode instanceof ContainerSchemaNode || childSchemaNode instanceof LeafSchemaNode) {
//                final String localName = childSchemaNode.getQName().getLocalName();
//                final Integer count = equalNodeNamesToCounts.get(localName);
//                if (count != null && count > 1) {
//                    throw new RestconfDocumentedException("Multiple input data elements were specified for '"
//                            + childSchemaNode.getQName().getLocalName()
//                            + "'. The data for this element type can only be specified once.", ErrorType.APPLICATION,
//                            ErrorTag.BAD_ELEMENT);
//                }
//            }
//        }
//    }

//    @Deprecated
//    private static QName normalizeNodeName(final NodeWrapper<? extends Object> nodeBuilder, final DataSchemaNode schema,
//            final QName previousAugment, final DOMMountPoint mountPoint) {
//        QName validQName = schema.getQName();
//        QName currentAugment = previousAugment;
//        if (schema.isAugmenting()) {
//            currentAugment = schema.getQName();
//        } else if (previousAugment != null
//                && !Objects.equal(schema.getQName().getNamespace(), previousAugment.getNamespace())) {
//            validQName = QName.create(currentAugment, schema.getQName().getLocalName());
//        }
//
//        String moduleName = null;
//        if (mountPoint == null) {
//            moduleName = ControllerContext.getInstance().findModuleNameByNamespace(validQName.getNamespace());
//        } else {
//            moduleName = ControllerContext.getInstance().findModuleNameByNamespace(mountPoint, validQName.getNamespace());
//        }
//
//        if (nodeBuilder.getNamespace() == null || Objects.equal(nodeBuilder.getNamespace(), validQName.getNamespace())
//                || Objects.equal(nodeBuilder.getNamespace().toString(), moduleName)) {
//            /*
//             * || Note : this check is wrong -
//             * can never be true as it compares
//             * a URI with a String not sure what
//             * the intention is so commented out
//             * ... Objects . equal ( nodeBuilder
//             * . getNamespace ( ) ,
//             * MOUNT_POINT_MODULE_NAME )
//             */
//
//            nodeBuilder.setQname(validQName);
//        }
//
//        return currentAugment;
//    }

//    @Deprecated
//    private static void normalizeAnyXmlNode(final CompositeNodeWrapper compositeNode, final AnyXmlSchemaNode schema) {
//        final List<NodeWrapper<?>> children = compositeNode.getValues();
//        for (final NodeWrapper<? extends Object> child : children) {
//            child.setNamespace(schema.getQName().getNamespace());
//            if (child instanceof CompositeNodeWrapper) {
//                normalizeAnyXmlNode((CompositeNodeWrapper) child, schema);
//            }
//        }
//    }

//    @Deprecated
//    private static void normalizeEmptyNode(final EmptyNodeWrapper emptyNodeBuilder, final DataSchemaNode schema) {
//        if ((schema instanceof LeafSchemaNode)) {
//            emptyNodeBuilder.setComposite(false);
//        } else {
//            if ((schema instanceof ContainerSchemaNode)) {
//                // FIXME: Add presence check
//                emptyNodeBuilder.setComposite(true);
//            }
//        }
//    }

//    @Deprecated
//    private static void normalizeSimpleNode(final SimpleNodeWrapper simpleNode, final DataSchemaNode schema,
//            final DOMMountPoint mountPoint) {
//        final Object value = simpleNode.getValue();
//        Object inputValue = value;
//        final TypeDef typeDef = typeDefinition(schema);
//        TypeDefinition<? extends Object> typeDefinition = typeDef != null ? typeDef.typedef : null;
//
//        // For leafrefs, extract the type it is pointing to
//        if(typeDefinition instanceof LeafrefTypeDefinition) {
//            if (schema.getQName().equals(typeDef.qName)) {
//                typeDefinition = SchemaContextUtil.getBaseTypeForLeafRef(((LeafrefTypeDefinition) typeDefinition), mountPoint == null ? ControllerContext.getInstance().getGlobalSchema() : mountPoint.getSchemaContext(), schema);
//            } else {
//                typeDefinition = SchemaContextUtil.getBaseTypeForLeafRef(((LeafrefTypeDefinition) typeDefinition), mountPoint == null ? ControllerContext.getInstance().getGlobalSchema() : mountPoint.getSchemaContext(), typeDef.qName);
//            }
//        }
//
//        if (typeDefinition instanceof IdentityrefTypeDefinition) {
//            inputValue = parseToIdentityValuesDTO(simpleNode, value, inputValue);
//        }
//
//        Object outputValue = inputValue;
//
//        if (typeDefinition != null) {
//            final Codec<Object, Object> codec = RestCodec.from(typeDefinition, mountPoint);
//            outputValue = codec == null ? null : codec.deserialize(inputValue);
//        }
//
//        simpleNode.setValue(outputValue);
//    }

//    @Deprecated
//    private static TypeDef typeDefinition(final DataSchemaNode node) {
//        if (node instanceof LeafListSchemaNode) {
//            return typeDefinition(((LeafListSchemaNode)node).getType(), node.getQName());
//        } else if (node instanceof LeafSchemaNode) {
//            return typeDefinition(((LeafSchemaNode)node).getType(), node.getQName());
//        } else if (node instanceof AnyXmlSchemaNode) {
//            return null;
//        } else {
//            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(node).toString());
//        }
//    }

//    @Deprecated
//    private static TypeDef typeDefinition(final TypeDefinition<?> type, final QName nodeQName) {
//        TypeDefinition<?> baseType = type;
//        QName qName = nodeQName;
//        while (baseType.getBaseType() != null) {
//            if (baseType instanceof ExtendedType) {
//                qName = baseType.getQName();
//            }
//            baseType = baseType.getBaseType();
//        }
//
//        return new TypeDef(baseType, qName);
//
//    }

//    @Deprecated
//    private static Object parseToIdentityValuesDTO(final SimpleNodeWrapper simpleNode, final Object value, Object inputValue) {
//        if ((value instanceof String)) {
//            inputValue = new IdentityValuesDTO(simpleNode.getNamespace().toString(), (String) value, null,
//                    (String) value);
//        } // else value is already instance of IdentityValuesDTO
//        return inputValue;
//    }

//    /**
//     * @deprecated method will be removed in Lithium release
//     *      we don't wish to use Node and CompositeNode anywhere
//     */
//    @Deprecated
//    public static CompositeNode normalizeNode(final Node<?> node, final DataSchemaNode schema, final DOMMountPoint mountPoint) {
//        if (schema == null) {
//            final String localName = node == null ? null :
//                    node instanceof NodeWrapper ? ((NodeWrapper<?>)node).getLocalName() :
//                    node.getNodeType().getLocalName();
//
//            throw new RestconfDocumentedException("Data schema node was not found for " + localName,
//                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
//        }
//
//        if (!(schema instanceof DataNodeContainer)) {
//            throw new RestconfDocumentedException("Root element has to be container or list yang datatype.",
//                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
//        }
//
//        if ((node instanceof NodeWrapper<?>)) {
//            NodeWrapper<?> nodeWrap = (NodeWrapper<?>) node;
//            final boolean isChangeAllowed = ((NodeWrapper<?>) node).isChangeAllowed();
//            if (isChangeAllowed) {
//                nodeWrap = topLevelElementAsCompositeNodeWrapper((NodeWrapper<?>) node, schema);
//                try {
//                    normalizeNode(nodeWrap, schema, null, mountPoint);
//                } catch (final IllegalArgumentException e) {
//                    final RestconfDocumentedException restconfDocumentedException = new RestconfDocumentedException(e.getMessage(), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
//                    restconfDocumentedException.addSuppressed(e);
//                    throw restconfDocumentedException;
//                }
//                if (nodeWrap instanceof CompositeNodeWrapper) {
//                    return ((CompositeNodeWrapper) nodeWrap).unwrap();
//                }
//            }
//        }
//
//        if (node instanceof CompositeNode) {
//            return (CompositeNode) node;
//        }
//
//        throw new RestconfDocumentedException("Top level element is not interpreted as composite node.",
//                ErrorType.APPLICATION, ErrorTag.INVALID_VALUE);
//    }

//    /**
//     * @deprecated
//     */
//    @Deprecated
//    private static CompositeNodeWrapper topLevelElementAsCompositeNodeWrapper(final NodeWrapper<?> node,
//            final DataSchemaNode schemaNode) {
//        if (node instanceof CompositeNodeWrapper) {
//            return (CompositeNodeWrapper) node;
//        } else if (node instanceof SimpleNodeWrapper && isDataContainerNode(schemaNode)) {
//            final SimpleNodeWrapper simpleNodeWrapper = (SimpleNodeWrapper) node;
//            return new CompositeNodeWrapper(namespace(simpleNodeWrapper), localName(simpleNodeWrapper));
//        }
//
//        throw new RestconfDocumentedException(new RestconfError(ErrorType.APPLICATION, ErrorTag.INVALID_VALUE,
//                "Top level element has to be composite node or has to represent data container node."));
//    }

//    /**
//     * @deprecated
//     */
//    @Deprecated
//    private static URI namespace(final Node<?> data) {
//        if (data instanceof NodeWrapper) {
//            return ((NodeWrapper<?>) data).getNamespace();
//        } else if (data != null) {
//            return data.getNodeType().getNamespace();
//        } else {
//            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(data).toString());
//        }
//    }
//
//    /**
//     * @deprecated
//     */
//    @Deprecated
//    private static String localName(final Node<?> data) {
//        if (data instanceof NodeWrapper) {
//            return ((NodeWrapper<?>) data).getLocalName();
//        } else if (data != null) {
//            return data.getNodeType().getLocalName();
//        } else {
//            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(data).toString());
//        }
//    }

//    /**
//     * @deprecated
//     */
//    @Deprecated
//    private static boolean isDataContainerNode(final DataSchemaNode schemaNode) {
//        if (schemaNode instanceof ContainerSchemaNode || schemaNode instanceof ListSchemaNode) {
//            return true;
//        }
//        return false;
//    }

    /**
     * Searches module with name {@code searchedModuleName} in {@code modules}. If module name isn't specified and
     * module set has only one element then this element is returned.
     *
     */
    public static Module resolveModule(final String searchedModuleName, final Set<Module> modules) {
        assertNotNull("Modules can't be null.", modules);
        if (searchedModuleName != null) {
            for (final Module m : modules) {
                if (m.getName().equals(searchedModuleName)) {
                    return m;
                }
            }
        } else if (modules.size() == 1) {
            return modules.iterator().next();
        }
        return null;
    }

    public static DataSchemaNode resolveDataSchemaNode(final String searchedDataSchemaName, final Module module) {
        assertNotNull("Module can't be null", module);

        if (searchedDataSchemaName != null) {
            for (final DataSchemaNode dsn : module.getChildNodes()) {
                if (dsn.getQName().getLocalName().equals(searchedDataSchemaName)) {
                    return dsn;
                }
            }
        } else if (module.getChildNodes().size() == 1) {
            return module.getChildNodes().iterator().next();
        }
        return null;
    }

    public static QName buildQName(final String name, final String uri, final String date, final String prefix) {
        try {
            final URI u = new URI(uri);
            Date dt = null;
            if (date != null) {
                dt = Date.valueOf(date);
            }
            return QName.create(u, dt, name);
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    public static QName buildQName(final String name, final String uri, final String date) {
        return buildQName(name, uri, date, null);
    }

    public static QName buildQName(final String name) {
        return buildQName(name, "", null);
    }

//    private static void addDummyNamespaceToAllNodes(final NodeWrapper<?> wrappedNode) throws URISyntaxException {
//        wrappedNode.setNamespace(new URI(""));
//        if (wrappedNode instanceof CompositeNodeWrapper) {
//            for (final NodeWrapper<?> childNodeWrapper : ((CompositeNodeWrapper) wrappedNode).getValues()) {
//                addDummyNamespaceToAllNodes(childNodeWrapper);
//            }
//        }
//    }

    private static void prepareMocksForRestconf(final Set<Module> modules, final RestconfImpl restconf) {
        final ControllerContext controllerContext = ControllerContext.getInstance();
        final BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);

        controllerContext.setSchemas(TestUtils.loadSchemaContext(modules));

        when(mockedBrokerFacade.commitConfigurationDataPut(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(mock(CheckedFuture.class));

        restconf.setControllerContext(controllerContext);
        restconf.setBroker(mockedBrokerFacade);
    }

//    public static Node<?> readInputToCnSn(final String path, final boolean dummyNamespaces,
//            final MessageBodyReader<Node<?>> reader) throws WebApplicationException {
//
//        final InputStream inputStream = TestUtils.class.getResourceAsStream(path);
//        try {
//            final Node<?> node = reader.readFrom(null, null, null, null, null, inputStream);
//            assertTrue(node instanceof CompositeNodeWrapper);
//            if (dummyNamespaces) {
//                try {
//                    TestUtils.addDummyNamespaceToAllNodes((CompositeNodeWrapper) node);
//                    return ((CompositeNodeWrapper) node).unwrap();
//                } catch (final URISyntaxException e) {
//                    LOG.error(e.getMessage());
//                    assertTrue(e.getMessage(), false);
//                }
//            }
//            return node;
//        } catch (final IOException e) {
//            LOG.error(e.getMessage());
//            assertTrue(e.getMessage(), false);
//        }
//        return null;
//    }

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

//    public static Node<?> readInputToCnSn(final String path, final MessageBodyReader<Node<?>> reader) {
//        return readInputToCnSn(path, false, reader);
//    }

//    public static String writeCompNodeWithSchemaContextToOutput(final Node<?> node, final Set<Module> modules,
//            final DataSchemaNode dataSchemaNode, final MessageBodyWriter<StructuredData> messageBodyWriter) throws IOException,
//            WebApplicationException {
//
//        assertNotNull(dataSchemaNode);
//        assertNotNull("Composite node can't be null", node);
//        final ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
//
//        ControllerContext.getInstance().setSchemas(loadSchemaContext(modules));
//
//        assertTrue(node instanceof CompositeNode);
//        messageBodyWriter.writeTo(new StructuredData((CompositeNode)node, dataSchemaNode, null), null, null, null, null,
//                null, byteArrayOS);
//
//        return byteArrayOS.toString();
//    }

    public static String loadTextFile(final String filePath) throws IOException {
        final FileReader fileReader = new FileReader(filePath);
        final BufferedReader bufReader = new BufferedReader(fileReader);

        String line = null;
        final StringBuilder result = new StringBuilder();
        while ((line = bufReader.readLine()) != null) {
            result.append(line);
        }
        bufReader.close();
        return result.toString();
    }

    private static Pattern patternForStringsSeparatedByWhiteChars(final String... substrings) {
        final StringBuilder pattern = new StringBuilder();
        pattern.append(".*");
        for (final String substring : substrings) {
            pattern.append(substring);
            pattern.append("\\s*");
        }
        pattern.append(".*");
        return Pattern.compile(pattern.toString(), Pattern.DOTALL);
    }

    public static boolean containsStringData(final String jsonOutput, final String... substrings) {
        final Pattern pattern = patternForStringsSeparatedByWhiteChars(substrings);
        final Matcher matcher = pattern.matcher(jsonOutput);
        return matcher.matches();
    }

    public static NormalizedNode compositeNodeToDatastoreNormalizedNode(final CompositeNode compositeNode,
            final DataSchemaNode schema) {
        final List<Node<?>> lst = new ArrayList<Node<?>>();
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

    public static YangInstanceIdentifier.NodeIdentifier getNodeIdentifier(final String localName, final String namespace,
            final String revision) throws ParseException {
        return new YangInstanceIdentifier.NodeIdentifier(QName.create(namespace, revision, localName));
    }

    public static YangInstanceIdentifier.NodeIdentifierWithPredicates getNodeIdentifierPredicate(final String localName,
            final String namespace, final String revision, final Map<String, Object> keys) throws ParseException {
        final Map<QName, Object> predicate = new HashMap<>();
        for (final String key : keys.keySet()) {
            predicate.put(QName.create(namespace, revision, key), keys.get(key));
        }

        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(

        QName.create(namespace, revision, localName), predicate);
    }

    public static YangInstanceIdentifier.NodeIdentifierWithPredicates getNodeIdentifierPredicate(final String localName,
            final String namespace, final String revision, final String... keysAndValues) throws ParseException {
        final java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(revision);
        if (keysAndValues.length % 2 != 0) {
            new IllegalArgumentException("number of keys argument have to be divisible by 2 (map)");
        }
        final Map<QName, Object> predicate = new HashMap<>();

        int i = 0;
        while (i < keysAndValues.length) {
            predicate.put(QName.create(namespace, revision, keysAndValues[i++]), keysAndValues[i++]);
        }

        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(QName.create(namespace, revision, localName),
                predicate);
    }

//    /**
//     * @deprecated method has to be removed for Lithium release
//     *          so please use prepareNormalizedNodeWithIetfInterfacesInterfacesData method
//     */
//    @Deprecated
//    public static CompositeNode prepareCompositeNodeWithIetfInterfacesInterfacesData() {
//        final CompositeNodeBuilder<ImmutableCompositeNode> interfaceBuilder = ImmutableCompositeNode.builder();
//        interfaceBuilder.addLeaf(buildQName("name", "dummy", "2014-07-29"), "eth0");
//        interfaceBuilder.addLeaf(buildQName("type", "dummy", "2014-07-29"), "ethernetCsmacd");
//        interfaceBuilder.addLeaf(buildQName("enabled", "dummy", "2014-07-29"), "false");
//        interfaceBuilder.addLeaf(buildQName("description", "dummy", "2014-07-29"), "some interface");
//        return interfaceBuilder.toInstance();
//    }

    static NormalizedNode<?,?> prepareNormalizedNodeWithIetfInterfacesInterfacesData() throws ParseException {
        final String ietfInterfacesDate = "2013-07-04";
        final String namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces";
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> mapEntryNode = ImmutableMapEntryNodeBuilder.create();

        final Map<String, Object> predicates = new HashMap<>();
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
}
