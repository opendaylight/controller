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
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeStreamReaderWriterTest {

    @Test
    public void testNormalizedNodeStreamReaderWriter() throws IOException {

        testNormalizedNodeStreamReaderWriter(createTestContainer());

        QName toaster = QName.create("http://netconfcentral.org/ns/toaster","2009-11-20","toaster");
        QName darknessFactor = QName.create("http://netconfcentral.org/ns/toaster","2009-11-20","darknessFactor");
        ContainerNode toasterNode = Builders.containerBuilder().
                withNodeIdentifier(new NodeIdentifier(toaster)).
                withChild(ImmutableNodes.leafNode(darknessFactor, "1000")).build();

        testNormalizedNodeStreamReaderWriter(Builders.containerBuilder().
                withNodeIdentifier(new NodeIdentifier(SchemaContext.NAME)).
                withChild(toasterNode).build());
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

        return TestModel.createBaseTestContainerBuilder().
                withChild(ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.BINARY_LEAF_LIST_QNAME)).
                        withChild(entry1).withChild(entry2).build()).
                withChild(ImmutableNodes.leafNode(TestModel.SOME_BINARY_DATA_QNAME, new byte[]{1,2,3,4})).
                withChild(Builders.orderedMapBuilder().
                      withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.ORDERED_LIST_QNAME)).
                      withChild(ImmutableNodes.mapEntry(TestModel.ORDERED_LIST_ENTRY_QNAME,
                              TestModel.ID_QNAME, 11)).build()).
                build();
    }

    private void testNormalizedNodeStreamReaderWriter(NormalizedNode<?, ?> input) throws IOException {

        byte[] byteData = null;

        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            NormalizedNodeStreamWriter writer = new NormalizedNodeOutputStreamWriter(byteArrayOutputStream)) {

            NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(writer);
            normalizedNodeWriter.write(input);
            byteData = byteArrayOutputStream.toByteArray();

        }

        NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(
                new ByteArrayInputStream(byteData));

        NormalizedNode<?,?> node = reader.readNormalizedNode();
        Assert.assertEquals(input, node);
    }

    @Test
    public void testWithSerializable() {
        NormalizedNode<?, ?> input = TestModel.createTestContainer();
        SampleNormalizedNodeSerializable serializable = new SampleNormalizedNodeSerializable(input );
        SampleNormalizedNodeSerializable clone = (SampleNormalizedNodeSerializable)SerializationUtils.clone(serializable);

        Assert.assertEquals(input, clone.getInput());

    }
}
