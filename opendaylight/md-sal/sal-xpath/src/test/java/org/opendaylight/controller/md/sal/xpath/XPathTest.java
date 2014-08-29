/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.md.sal.xpath;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Ignore;
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
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.topcontainer.ChildContainer;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.topcontainer.ChildContainerBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.topcontainer.UnorderList;
import org.opendaylight.yang.gen.v1.opendaylight.xpath.test.simple.list.rev140825.topcontainer.UnorderListBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;

public class XPathTest extends AbstractDataBrokerTest {

    private XPath xpath;
    private static QName TOP_CONTAINER_QNAME =
            QName.create("opendaylight:xpath:test:simple:list",
                         "2014-08-25",
                         "topContainer");

    private NormalizedNodeElement rootElement;
    private NormalizedNode<?, ?> normalizedNode;

    private Document xmlDocument;

    @Override
    protected void setupWithDataBroker(DataBroker dataBroker) {
        try {
            initializeTestContainer();
            normalizedNode = readNormalizedNode();
            rootElement = new NormalizedNodeElement( normalizedNode, null );

            org.opendaylight.yangtools.yang.data.api.Node<?> legacy = DataNormalizer.toLegacy( normalizedNode );

            DataSchemaNode schema = extractRootSchema();

            xmlDocument = new XmlMapper().write( (CompositeNode)legacy, (DataNodeContainer)schema);
        } catch (Exception e) {
            if( e instanceof RuntimeException ){
                throw (RuntimeException)e;
            } else
            {
                throw new RuntimeException( e );
            }
        }
        xpath = XPathFactory.newInstance().newXPath();


    }

    /**
     * Tests that the XML produced by NormalizedNodeElement matches the XML produced by
     * restconf.
     * @throws Exception
     */
    @Test
    @Ignore
    public void testXMLEqual() throws Exception{

        ByteArrayOutputStream expectedXML = new ByteArrayOutputStream();
        printDocument( xmlDocument, expectedXML );

        ByteArrayOutputStream actualXML = new ByteArrayOutputStream();
        printDocument( rootElement, actualXML );

        assertEquals( "XML Produced by restconf and XML produced by xpath-nodes differs.",
                      expectedXML, actualXML );

    }

    @Test
    public void printExpectedXMLAsGeneratedByRestConf() throws Exception{
        org.opendaylight.yangtools.yang.data.api.Node<?> legacy = DataNormalizer.toLegacy( normalizedNode );

        DataSchemaNode schema = extractRootSchema();

        Document document = new XmlMapper().write( (CompositeNode)legacy, (DataNodeContainer)schema);

        printDocument( document, System.out );
    }

    public void evaluate(String xpathexp, String expectedValue)
            throws XPathExpressionException {
        String node = (String) xpath.evaluate(xpathexp, rootElement,
                XPathConstants.STRING);
        System.out.println("'" + node + "'");
        assertEquals(expectedValue, node);
    }

    private void compareXPath( String xpathStr ) throws Exception{
        assertEquals( "Mismatch when evaluating '" + xpathStr + "'",
                      xpath.evaluate( xpathStr, xmlDocument, XPathConstants.STRING ),
                      xpath.evaluate( xpathStr, rootElement, XPathConstants.STRING ) );
    }

    @Test
    @Ignore //We need to fix these issues first!
    public void testExpressions() throws Exception {

        compareXPath( "name(.)" );

/*        System.out.println("========== ");
        printDocument(rootTestNode, System.out);
        System.out.println("========== ");

        evaluate("name(.)", "toaster");
        evaluate("//toasterModelNumber", "Model 1 - Binding Aware");
        evaluate("//toasterStatus", "up");
        evaluate("toasterManufacturer", "Opendaylight");*/

    /* TODO: Handle unkeyedListNode and LeafSetNode

        evaluate("//toasterMaker", "ChildOpendaylight");
        evaluate("//toasterModel", "ChildModel 1 - Binding Aware");
        evaluate("childtoaster/toasterMaker", "ChildOpendaylight");
        evaluate("childtoaster/toasterModel", "ChildModel 1 - Binding Aware");
        evaluate("//toastervalue", "Value2");
*/
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

        YangInstanceIdentifier domTopId = YangInstanceIdentifier.builder().node(
                TOP_CONTAINER_QNAME ).build();

        System.out.println( "Getting value just written" );
        NormalizedNode<?, ?> normalizedNode = readT.read( LogicalDatastoreType.CONFIGURATION, domTopId ).get().get();
        return normalizedNode;
    }

    private void initializeTestContainer() throws InterruptedException, ExecutionException {
        DataBroker dataBroker = getDataBroker();
        WriteTransaction rwTx = dataBroker.newWriteOnlyTransaction();

        InstanceIdentifier<TopContainer> topId = InstanceIdentifier.builder( TopContainer.class ).build();
        ChildContainer cContainer = new ChildContainerBuilder()
                                .setChildContainerLeafString( "childContainerLeaf" ).build();


        UnorderList unorderedList1 = new UnorderListBuilder()
                               .setUnorderLeafList( Arrays.asList( "first", "second" )).build();
        UnorderList unorderedList2 = new UnorderListBuilder()
                    .setUnorderLeafList( Arrays.asList( "third", "forth" )).build();

        TopContainer topContainer =
                new TopContainerBuilder().setTopLeafString( "topLeaf")
                                         .setChildContainer( cContainer )
                                         .setUnorderList( Arrays.asList( unorderedList1, unorderedList2 ))
                                         .build();

        rwTx.put( LogicalDatastoreType.CONFIGURATION, topId, topContainer );

        rwTx.submit().get();
    }

    public static void printDocument(org.w3c.dom.Node doc, OutputStream out)
            throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc), new StreamResult(
                new OutputStreamWriter(out, "UTF-8")));
    }


}
