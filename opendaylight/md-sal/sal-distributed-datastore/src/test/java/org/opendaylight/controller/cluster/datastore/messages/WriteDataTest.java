/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.InstanceIdentifier;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for WriteData.
 *
 * @author Thomas Pantelis
 */
public class WriteDataTest {

    @Test
    public void testSerialization() {
        WriteData expected = new WriteData(TestModel.TEST_PATH, ImmutableNodes
            .containerNode(TestModel.TEST_QNAME));

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ExternalizableWriteData.class, serialized.getClass());

        WriteData actual = WriteData.fromSerializable(SerializationUtils.clone((Serializable) serialized));
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getData", expected.getData(), actual.getData());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, WriteData.isSerializedType(
                ShardTransactionMessages.WriteData.newBuilder()
                    .setInstanceIdentifierPathArguments(InstanceIdentifier.getDefaultInstance())
                    .setNormalizedNode(Node.getDefaultInstance()).build()));
        assertEquals("isSerializedType", true,
                WriteData.isSerializedType(new ExternalizableWriteData()));
        assertEquals("isSerializedType", false, WriteData.isSerializedType(new Object()));
    }

    /**
     * Tests backwards compatible deserialization of a WriteData message from the base Helium
     * version (messages version 0).
     */
    @Test
    public void testDeserializationFromVersion0() throws Exception {
        // The path and data node that was serialized in the file.
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        try(FileInputStream fis = new FileInputStream("src/test/resources/SerializedWriteDataV0.out")) {
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);

            ShardTransactionMessages.WriteData serialized =
                    ShardTransactionMessages.WriteData.parseFrom(bytes);

            WriteData actual = WriteData.fromSerializable(serialized);
            assertEquals("getPath", path, actual.getPath());
            assertEquals("getData", data, actual.getData());
        }
    }

    // Method to use for writing the serialized bytes to a file
    //@Test
    public void writeSerializedFile() throws Exception {
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        ShardTransactionMessages.WriteData serialized = (ShardTransactionMessages.WriteData)
                new WriteData(path, data).toSerializable();
        try(FileOutputStream fos = new FileOutputStream("src/test/resources/SerializedWriteDataV0.out")) {
            fos.write(serialized.toByteArray());
        }
    }
}
