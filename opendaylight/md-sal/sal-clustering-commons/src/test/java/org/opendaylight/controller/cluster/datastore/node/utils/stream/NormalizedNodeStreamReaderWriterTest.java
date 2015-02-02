/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeStreamReaderWriterTest {

    @Test
    public void testNormalizedNodeStreaming() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        NormalizedNodeOutputStreamWriter writer = new NormalizedNodeOutputStreamWriter(byteArrayOutputStream);

        NormalizedNode<?, ?> testContainer = createTestContainer();
        writer.writeNormalizedNode(testContainer);

        QName toaster = QName.create("http://netconfcentral.org/ns/toaster","2009-11-20","toaster");
        QName darknessFactor = QName.create("http://netconfcentral.org/ns/toaster","2009-11-20","darknessFactor");
        ContainerNode toasterNode = Builders.containerBuilder().
                withNodeIdentifier(new NodeIdentifier(toaster)).
                withChild(ImmutableNodes.leafNode(darknessFactor, "1000")).build();

        ContainerNode toasterContainer = Builders.containerBuilder().
                withNodeIdentifier(new NodeIdentifier(SchemaContext.NAME)).
                withChild(toasterNode).build();
        writer.writeNormalizedNode(toasterContainer);

        NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

        NormalizedNode<?,?> node = reader.readNormalizedNode();
        Assert.assertEquals(testContainer, node);

        node = reader.readNormalizedNode();
        Assert.assertEquals(toasterContainer, node);

        writer.close();
    }

    private NormalizedNode<?, ?> createTestContainer() {
        byte[] bytes1 = {1,2,3};
        LeafSetEntryNode<Object> entry1 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue(TestModel.BINARY_LEAF_LIST_QNAME, bytes1)).
                withValue(bytes1).build();

        byte[] bytes2 = {};
        LeafSetEntryNode<Object> entry2 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue(TestModel.BINARY_LEAF_LIST_QNAME, bytes2)).
                withValue(bytes2).build();

        LeafSetEntryNode<Object> entry3 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue(TestModel.BINARY_LEAF_LIST_QNAME, null)).
                withValue(null).build();


        return TestModel.createBaseTestContainerBuilder().
                withChild(ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.BINARY_LEAF_LIST_QNAME)).
                        withChild(entry1).withChild(entry2).withChild(entry3).build()).
                withChild(ImmutableNodes.leafNode(TestModel.SOME_BINARY_DATA_QNAME, new byte[]{1,2,3,4})).
                withChild(Builders.orderedMapBuilder().
                      withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.ORDERED_LIST_QNAME)).
                      withChild(ImmutableNodes.mapEntry(TestModel.ORDERED_LIST_ENTRY_QNAME,
                              TestModel.ID_QNAME, 11)).build()).
                build();
    }

    @Test
    public void testYangInstanceIdentifierStreaming() throws IOException  {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.TEST_PATH).
                node(TestModel.OUTER_LIST_QNAME).nodeWithKey(
                        TestModel.INNER_LIST_QNAME, TestModel.ID_QNAME, 10).build();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        NormalizedNodeOutputStreamWriter writer =
                new NormalizedNodeOutputStreamWriter(byteArrayOutputStream);
        writer.writeYangInstanceIdentifier(path);

        NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

        YangInstanceIdentifier newPath = reader.readYangInstanceIdentifier();
        Assert.assertEquals(path, newPath);

        writer.close();
    }

    @Test
    public void testNormalizedNodeAndYangInstanceIdentifierStreaming() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        NormalizedNodeOutputStreamWriter writer = new NormalizedNodeOutputStreamWriter(byteArrayOutputStream);

        NormalizedNode<?, ?> testContainer = TestModel.createBaseTestContainerBuilder().build();
        writer.writeNormalizedNode(testContainer);

        YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.TEST_PATH).
                node(TestModel.OUTER_LIST_QNAME).nodeWithKey(
                        TestModel.INNER_LIST_QNAME, TestModel.ID_QNAME, 10).build();

        writer.writeYangInstanceIdentifier(path);

        NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

        NormalizedNode<?,?> node = reader.readNormalizedNode();
        Assert.assertEquals(testContainer, node);

        YangInstanceIdentifier newPath = reader.readYangInstanceIdentifier();
        Assert.assertEquals(path, newPath);

        writer.close();
    }

    @Test(expected=InvalidNormalizedNodeStreamException.class, timeout=10000)
    public void testInvalidNormalizedNodeStream() throws IOException {
        byte[] protobufBytes = new NormalizedNodeToNodeCodec(null).encode(
                TestModel.createBaseTestContainerBuilder().build()).getNormalizedNode().toByteArray();

        NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(
                new ByteArrayInputStream(protobufBytes));

        reader.readNormalizedNode();
    }

    @Test(expected=InvalidNormalizedNodeStreamException.class, timeout=10000)
    public void testInvalidYangInstanceIdentifierStream() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.TEST_PATH).build();

        byte[] protobufBytes = ShardTransactionMessages.DeleteData.newBuilder().setInstanceIdentifierPathArguments(
                InstanceIdentifierUtils.toSerializable(path)).build().toByteArray();

        NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(
                new ByteArrayInputStream(protobufBytes));

        reader.readYangInstanceIdentifier();
    }

    @Test
    public void testWithSerializable() {
        NormalizedNode<?, ?> input = TestModel.createTestContainer();
        SampleNormalizedNodeSerializable serializable = new SampleNormalizedNodeSerializable(input );
        SampleNormalizedNodeSerializable clone = (SampleNormalizedNodeSerializable)SerializationUtils.clone(serializable);

        Assert.assertEquals(input, clone.getInput());

    }
}
