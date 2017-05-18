/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class NormalizedNodeStreamReaderWriterTest {

    @Test
    public void testNormalizedNodeStreaming() throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(ByteStreams.newDataOutput(bos));

        NormalizedNode<?, ?> testContainer = createTestContainer();
        nnout.writeNormalizedNode(testContainer);

        QName toaster = QName.create("http://netconfcentral.org/ns/toaster","2009-11-20","toaster");
        QName darknessFactor = QName.create("http://netconfcentral.org/ns/toaster","2009-11-20","darknessFactor");
        QName description = QName.create("http://netconfcentral.org/ns/toaster","2009-11-20","description");
        ContainerNode toasterNode = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(toaster))
                .withChild(ImmutableNodes.leafNode(darknessFactor, "1000"))
                .withChild(ImmutableNodes.leafNode(description, largeString(20))).build();

        ContainerNode toasterContainer = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(SchemaContext.NAME)).withChild(toasterNode).build();
        nnout.writeNormalizedNode(toasterContainer);

        NormalizedNodeDataInput nnin = NormalizedNodeInputOutput.newDataInput(ByteStreams.newDataInput(
            bos.toByteArray()));

        NormalizedNode<?,?> node = nnin.readNormalizedNode();
        Assert.assertEquals(testContainer, node);

        node = nnin.readNormalizedNode();
        Assert.assertEquals(toasterContainer, node);
    }

    private static NormalizedNode<?, ?> createTestContainer() {
        byte[] bytes1 = {1,2,3};
        LeafSetEntryNode<Object> entry1 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new NodeWithValue<>(TestModel.BINARY_LEAF_LIST_QNAME, bytes1)).withValue(bytes1).build();

        byte[] bytes2 = {};
        LeafSetEntryNode<Object> entry2 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new NodeWithValue<>(TestModel.BINARY_LEAF_LIST_QNAME, bytes2)).withValue(bytes2).build();

        LeafSetEntryNode<Object> entry3 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new NodeWithValue<>(TestModel.BINARY_LEAF_LIST_QNAME, null)).withValue(null).build();

        return TestModel.createBaseTestContainerBuilder()
                .withChild(ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                        new NodeIdentifier(TestModel.BINARY_LEAF_LIST_QNAME))
                        .withChild(entry1).withChild(entry2).withChild(entry3).build())
                .withChild(ImmutableNodes.leafNode(TestModel.SOME_BINARY_DATA_QNAME, new byte[]{1,2,3,4}))
                .withChild(Builders.orderedMapBuilder()
                      .withNodeIdentifier(new NodeIdentifier(TestModel.ORDERED_LIST_QNAME))
                      .withChild(ImmutableNodes.mapEntry(TestModel.ORDERED_LIST_ENTRY_QNAME,
                              TestModel.ID_QNAME, 11)).build()).build();
    }

    @Test
    public void testYangInstanceIdentifierStreaming() throws IOException  {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.TEST_PATH)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(
                        TestModel.INNER_LIST_QNAME, TestModel.ID_QNAME, 10).build();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(ByteStreams.newDataOutput(bos));

        nnout.writeYangInstanceIdentifier(path);

        NormalizedNodeDataInput nnin = NormalizedNodeInputOutput.newDataInput(ByteStreams.newDataInput(
            bos.toByteArray()));

        YangInstanceIdentifier newPath = nnin.readYangInstanceIdentifier();
        Assert.assertEquals(path, newPath);
    }

    @Test
    public void testNormalizedNodeAndYangInstanceIdentifierStreaming() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        NormalizedNodeOutputStreamWriter writer = new NormalizedNodeOutputStreamWriter(
            ByteStreams.newDataOutput(byteArrayOutputStream));

        NormalizedNode<?, ?> testContainer = TestModel.createBaseTestContainerBuilder().build();
        writer.writeNormalizedNode(testContainer);

        YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.TEST_PATH)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(
                        TestModel.INNER_LIST_QNAME, TestModel.ID_QNAME, 10).build();

        writer.writeYangInstanceIdentifier(path);

        NormalizedNodeDataInput reader = NormalizedNodeInputOutput.newDataInput(
            ByteStreams.newDataInput(byteArrayOutputStream.toByteArray()));

        NormalizedNode<?,?> node = reader.readNormalizedNode();
        Assert.assertEquals(testContainer, node);

        YangInstanceIdentifier newPath = reader.readYangInstanceIdentifier();
        Assert.assertEquals(path, newPath);

        writer.close();
    }

    @Test(expected = InvalidNormalizedNodeStreamException.class, timeout = 10000)
    public void testInvalidNormalizedNodeStream() throws IOException {
        byte[] invalidBytes = {1,2,3};
        NormalizedNodeDataInput reader = NormalizedNodeInputOutput.newDataInput(
                ByteStreams.newDataInput(invalidBytes));

        reader.readNormalizedNode();
    }

    @Test(expected = InvalidNormalizedNodeStreamException.class, timeout = 10000)
    public void testInvalidYangInstanceIdentifierStream() throws IOException {
        byte[] invalidBytes = {1,2,3};
        NormalizedNodeDataInput reader = NormalizedNodeInputOutput.newDataInput(
            ByteStreams.newDataInput(invalidBytes));

        reader.readYangInstanceIdentifier();
    }

    @Test
    public void testWithSerializable() {
        NormalizedNode<?, ?> input = TestModel.createTestContainer();
        SampleNormalizedNodeSerializable serializable = new SampleNormalizedNodeSerializable(input);
        SampleNormalizedNodeSerializable clone =
                (SampleNormalizedNodeSerializable)SerializationUtils.clone(serializable);

        Assert.assertEquals(input, clone.getInput());
    }

    @Test
    public void testAnyXmlStreaming() throws Exception {
        String xml = "<foo xmlns=\"http://www.w3.org/TR/html4/\" x=\"123\"><bar>one</bar><bar>two</bar></foo>";
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Node xmlNode = factory.newDocumentBuilder().parse(
                new InputSource(new StringReader(xml))).getDocumentElement();

        assertEquals("http://www.w3.org/TR/html4/", xmlNode.getNamespaceURI());

        NormalizedNode<?, ?> anyXmlContainer = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).withChild(
                        Builders.anyXmlBuilder().withNodeIdentifier(new NodeIdentifier(TestModel.ANY_XML_QNAME))
                            .withValue(new DOMSource(xmlNode)).build()).build();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(ByteStreams.newDataOutput(bos));

        nnout.writeNormalizedNode(anyXmlContainer);

        NormalizedNodeDataInput nnin = NormalizedNodeInputOutput.newDataInput(ByteStreams.newDataInput(
            bos.toByteArray()));

        ContainerNode deserialized = (ContainerNode)nnin.readNormalizedNode();

        Optional<DataContainerChild<? extends PathArgument, ?>> child =
                deserialized.getChild(new NodeIdentifier(TestModel.ANY_XML_QNAME));
        assertEquals("AnyXml child present", true, child.isPresent());

        StreamResult xmlOutput = new StreamResult(new StringWriter());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(((AnyXmlNode)child.get()).getValue(), xmlOutput);

        assertEquals("XML", xml, xmlOutput.getWriter().toString());
        assertEquals("http://www.w3.org/TR/html4/", ((AnyXmlNode)child.get()).getValue().getNode().getNamespaceURI());
    }

    @Test
    public void testSchemaPathSerialization() throws Exception {
        final SchemaPath expected = SchemaPath.create(true, TestModel.ANY_XML_QNAME);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(ByteStreams.newDataOutput(bos));
        nnout.writeSchemaPath(expected);

        NormalizedNodeDataInput nnin = NormalizedNodeInputOutput.newDataInput(ByteStreams.newDataInput(
            bos.toByteArray()));
        SchemaPath actual = nnin.readSchemaPath();
        assertEquals(expected, actual);
    }

    private static String largeString(final int pow) {
        StringBuilder sb = new StringBuilder("X");
        for (int i = 0; i < pow; i++) {
            sb.append(sb);
        }
        return sb.toString();
    }
}
