/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.TopContainer;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.TopContainerBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.top.container.grouping.ChildContainer;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.top.container.grouping.ChildContainerBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.top.container.grouping.OrderedList;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.top.container.grouping.OrderedListBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.top.container.grouping.OrderedListKey;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.top.container.grouping.UnorderList;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.top.container.grouping.UnorderListBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XPathTest extends AbstractDataBrokerTest {

    private XPath xpath;
    private static QName TOP_CONTAINER_QNAME = QName.create("opendaylight:xpath:test:simple:list",
            "2014-08-25", "topContainer");

    /** Holds the wrapped normalized node element used in xpath queries */
    private NormalizedNodeElement proxiedNormalizedNodeRoot;

    /** Holds the raw NormalizedNode that is wrapped in mdSalRootElement */
    private NormalizedNode<?, ?> normalizedNode;

    /**
     * Holds the java generated node which was parsed from the string
     * representation of the xml.
     */
    private Node xmlRootElementFromMDSalXMLWrapper;
    private Node xmlRootElementFromParsingXMLString;

    @Override
    protected void setupWithDataBroker(DataBroker dataBroker) {
        xpath = XPathFactory.newInstance().newXPath();
        // System.out.println( "Class Path:" + System.getProperty( "classpath" )
        // );
        initialVariables();
    }

    private void initializeTestContainer() throws InterruptedException, ExecutionException {
        DataBroker dataBroker = getDataBroker();
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();

        InstanceIdentifier<TopContainer> topId = InstanceIdentifier.builder(TopContainer.class)
                .build();
        ChildContainer cContainer = new ChildContainerBuilder().setChildContainerLeafString(
                "childContainerLeaf").build();

        UnorderList unorderedList1 = new UnorderListBuilder().setUnorderLeafList(
                Arrays.asList("first", "second")).build();
        UnorderList unorderedList2 = new UnorderListBuilder().setUnorderLeafList(
                Arrays.asList("third", "forth")).build();

        OrderedListKey orderedListEntryKey1 = new OrderedListKey("key 1");
        OrderedList orderedList1 = new OrderedListBuilder().setKey(orderedListEntryKey1)
                .setOrderedListKey("key 1").setOrderedListNestedList(Arrays.asList("3", "2", "1"))
                .build();

        TopContainer topContainer = new TopContainerBuilder().setTopLeafString("topLeaf")
                .setChildContainer(cContainer)
                .setUnorderList(Arrays.asList(unorderedList1, unorderedList2))
                .setOrderedList(Arrays.asList(orderedList1))
                .setUserOrderedList(Arrays.asList("9", "8", "7")).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, topId, topContainer);
        rwTx.submit().get();
    }

    private void initialVariables() {
        try {
            initializeTestContainer();
            normalizedNode = readNormalizedNode();
            proxiedNormalizedNodeRoot = new NormalizedNodeElement(normalizedNode, null);

            org.opendaylight.yangtools.yang.data.api.Node<?> legacy = DataNormalizer
                    .toLegacy(normalizedNode);

            DataSchemaNode schema = extractRootSchema();

            //This create an MD-SAL "XML Document" - not necessarily matching what java creates.
            Document xmlDocument = new XmlMapper().write((CompositeNode) legacy,
                    (DataNodeContainer) schema);

            xmlRootElementFromMDSalXMLWrapper = (Node) xpath.evaluate("/*[1]", xmlDocument, XPathConstants.NODE);
            xmlDocument.removeChild(xmlRootElementFromMDSalXMLWrapper);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            printDocument( xmlRootElementFromMDSalXMLWrapper, baos, false );

            //System.out.println( baos.toString() );
            //lets parse the XML again as the XmlMapper may actually be using differnet fields
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document d =  dBuilder.parse( new ByteArrayInputStream( baos.toString().getBytes() ));

            xmlRootElementFromParsingXMLString = (Node)xpath.evaluate( "/topContainer", d, XPathConstants.NODE );

            d.removeChild( xmlRootElementFromParsingXMLString );
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }

        assertNotNull(xmlRootElementFromMDSalXMLWrapper);
    }

    @Test
    //@Ignore
    public void printMethodCalls() throws Exception{
//        System.out.println("Expected XML Output:");
//        printDocument(xmlRootElement, System.out, false );
//
//        System.out.println("\nActual XML Output:");
//        printDocument(mdSalRootElement, System.out, false );
//        System.out.println();

        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document d =  dBuilder.parse( new FileInputStream("src/test/resources/condensed_sample_yang.xml"));

        Node nodeRoot = (Node)xpath.evaluate( "/topContainer", d, XPathConstants.NODE );

        d.removeChild( nodeRoot );

        printMethodCalls( "//childContainerLeafString", proxiedNormalizedNodeRoot, "Proxied Normalized Node Root" );
        printMethodCalls( "//childContainerLeafString", nodeRoot, "Prased XML File" );
        printMethodCalls( "//childContainerLeafString", xmlRootElementFromParsingXMLString, "Parsed MD-SAL To String XML" );
    }

    private void printMethodCalls( String xpathStr, Node node, String label ) throws Exception{
        System.out.println( "Printing method calls for xpath '" + xpathStr + "' on " + label + ":" );
        System.out.println( "Result: '" + xpath.evaluate( xpathStr,
                                          NodeProxy.wrapNode(node ) ) + "'" );
        System.out.println( "Done printing method calls." );
    }

    /**
     * Tests that the XML produced by NormalizedNodeElement matches the XML
     * produced by restconf.
     *
     * @throws Exception
     */
    @Test
    //@Ignore // ignoring for now since the default namespace is causing some problems.
    public void testXMLEqual() throws Exception {

        ByteArrayOutputStream expectedXML = new ByteArrayOutputStream();
        printDocument(xmlRootElementFromMDSalXMLWrapper, expectedXML, true );

        ByteArrayOutputStream actualXML = new ByteArrayOutputStream();
        printDocument(proxiedNormalizedNodeRoot, actualXML, true );

        assertEquals("XML Produced by restconf and XML produced by xpath-nodes differs.",
                expectedXML.toString(), actualXML.toString());

    }

    @Test
    //@Ignore
    public void printExpectedXMLAsGeneratedByRestConf() throws Exception {
        System.out.println("Expected XML Output:");
        printDocument(xmlRootElementFromMDSalXMLWrapper, System.out, true );

        System.out.println("Actual XML Output:");
        printDocument(proxiedNormalizedNodeRoot, System.out, true );

    }

    @Test
    //@Ignore
    public void recursivelyValidateNodeEquality() {
        validateNode(xmlRootElementFromMDSalXMLWrapper, proxiedNormalizedNodeRoot, "");
    }

    /*
     *
        <topContainer xmlns="opendaylight:xpath:test:simple:list">
            <childContainer>
                <childContainerLeafString>childContainerLeaf</childContainerLeafString>
            </childContainer>
            <unorderList>
                <unorderLeafList>second</unorderLeafList>
                <unorderLeafList>first</unorderLeafList>
            </unorderList>
            <unorderList>
                <unorderLeafList>third</unorderLeafList>
                <unorderLeafList>forth</unorderLeafList>
            </unorderList>
            <topLeafString>topLeaf</topLeafString>
            <orderedList>
                <orderedListKey>key 1</orderedListKey>
                <orderedListNestedList>1</orderedListNestedList>
                <orderedListNestedList>2</orderedListNestedList>
                <orderedListNestedList>3</orderedListNestedList>
            </orderedList>
            <userOrderedList>8</userOrderedList>
            <userOrderedList>7</userOrderedList>
            <userOrderedList>9</userOrderedList>
        </topContainer>
     *
     */
    @Test
    //@Ignore //still not working right.
    public void testExpressions() throws Exception {


        NamespaceContext nsContext = new NamespaceContext() {

            Map<String,String> prefixToNamespace = new HashMap<>();
            Map<String,String> namespaceToPrefix = new HashMap<>();

            {
                prefixToNamespace.put( "ns", "opendaylight:xpath:test:simple:list" );
                namespaceToPrefix.put( "opendaylight:xpath:test:simple:list", "ns" );
            }
            @Override
            public Iterator getPrefixes(String namespaceURI) {
                System.out.println( "getPrefixes: " + namespaceURI );
                return Collections.singleton( namespaceToPrefix.get( namespaceURI ) ).iterator();
            }

            @Override
            public String getPrefix(String namespaceURI) {
                System.out.println( "getPrefix: " + namespaceURI );
                return namespaceToPrefix.get( namespaceURI );
            }

            @Override
            public String getNamespaceURI(String prefix) {
                System.out.println( "getNamespaceURI: '" + prefix + "'");
                return prefixToNamespace.get( prefix );
            }
        };
        xpath.setNamespaceContext(nsContext);

        //compareXPath( "name(.)", "topContainer" );
        compareXPath( "//childContainer" );
        //compareXPath( "count( //unorderLeafList[text()='second'] )", "1" );
        //compareXPath( "//childContainerLeafString", "childContainerLeaf" );


        /*
         * System.out.println("========== "); printDocument(rootTestNode,
         * System.out); System.out.println("========== ");
         *
         * evaluate("name(.)", "toaster"); evaluate("//toasterModelNumber",
         * "Model 1 - Binding Aware"); evaluate("//toasterStatus", "up");
         * evaluate("toasterManufacturer", "Opendaylight");
         */

        /*
         * TODO: Handle unkeyedListNode and LeafSetNode
         *
         * evaluate("//toasterMaker", "ChildOpendaylight");
         * evaluate("//toasterModel", "ChildModel 1 - Binding Aware");
         * evaluate("childtoaster/toasterMaker", "ChildOpendaylight");
         * evaluate("childtoaster/toasterModel",
         * "ChildModel 1 - Binding Aware"); evaluate("//toastervalue",
         * "Value2");
         */
    }

    private void compareXPath(String xpathStr ) throws Exception {
        compareXPath( xpathStr, null );
    }

    private void compareXPath(String xpathStr, Object expectedValue ) throws Exception {
        Object expectedResult = xpath.evaluate(xpathStr, xmlRootElementFromMDSalXMLWrapper, XPathConstants.STRING);
        Object actualResult = xpath.evaluate(xpathStr, proxiedNormalizedNodeRoot, XPathConstants.STRING);
        printAndAssertEquals("Evaluating xpath expression '" + xpathStr + "'. ",
                expectedResult,
                actualResult);
        if( expectedValue != null ){
            printAndAssertEquals( "XPaths, while equal, did not return hardcoded expected result.",
                                  expectedValue, expectedResult );
        }
    }

    private void validateNode(Node n1, Node n2, String path) {
        validateNodeNames(n1, n2, path);
        if (n1 == null && n2 == null) {
            // allowed
            return;
        }
        printAndAssertEquals("[" + path + "]Has child node - '" + n1.getNodeName() + "'. ",
                n1.hasChildNodes(), n2.hasChildNodes());
        validateNodeNames(n1.getParentNode(), n2.getParentNode(), path + "(parents)");

        printAndAssertEquals( "[" + path + "]Node Value. ", n1.getNodeValue(), n2.getNodeValue() );
        printAndAssertEquals( "[" + path + "]Text Content. ", n1.getTextContent(), n2.getTextContent() );

        if (n1.hasChildNodes()) {
            Node n1Child = n1.getFirstChild();
            Node n2Child = n2.getFirstChild();

            while (n1Child != null || n2Child != null) {
                validateNode(n1Child, n2Child, path + "-" + n1.getNodeName());
                n1Child = n1Child.getNextSibling();
                n2Child = n2Child.getNextSibling();
            }
        }

    }

    private void validateNodeNames(Node n1, Node n2, String path) {
        if ((n1 == null && n2 != null) || (n1 != null && n2 == null)) {
            fail("One node is null and the other is not. Node1: " + n1 + ", Node2: " + n2);
        }

        if (n1 == null && n2 == null) {
            // allowed
            return;
        }

        printAndAssertEquals("[" + path + "]Node Name Comparison: ", n1.getNodeName(),
                n2.getNodeName());
        //Don't compare namespaces for now - we know they are not right.
//        printAndAssertEquals("[" + path + "]Root Namespace. ", n1.getNamespaceURI(),
//                n2.getNamespaceURI());
    }

    private void printAndAssertEquals(String msg, Object o1, Object o2) {
        System.out.print((msg != null ? msg : "") + "Comparing '" + o1 + "' to '" + o2 + "' ...");
        System.out.flush();
        try {
            assertEquals(msg, o1, o2);
        } catch (Throwable t) {
            System.out.println("NOT EQUAL!");
            throw t;
        }
        System.out.println("EQUAL!");
    }

    private DataSchemaNode extractRootSchema() {
        SchemaContext schemaContext = getSchemaContext();
        DataSchemaNode schema = schemaContext.getDataChildByName(TOP_CONTAINER_QNAME);
        return schema;
    }

    private NormalizedNode<?, ?> readNormalizedNode() throws InterruptedException,
            ExecutionException {
        DOMDataBroker domBroker = getDomBroker();
        DOMDataReadOnlyTransaction readT = domBroker.newReadOnlyTransaction();

        YangInstanceIdentifier domTopId = YangInstanceIdentifier.builder()
                .node(TOP_CONTAINER_QNAME).build();

        // System.out.println( "Getting value just written" );
        NormalizedNode<?, ?> normalizedNode = readT
                .read(LogicalDatastoreType.CONFIGURATION, domTopId).get().get();
        return normalizedNode;
    }

    private static void printDocument(org.w3c.dom.Node doc, OutputStream out, boolean indent ) throws IOException,
            TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        if( indent ){
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out,
                "UTF-8")));
    }

}
