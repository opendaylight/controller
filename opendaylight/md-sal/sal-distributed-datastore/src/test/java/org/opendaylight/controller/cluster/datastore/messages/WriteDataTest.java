/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import java.io.Serializable;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
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
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        WriteData expected = new WriteData(path, data);

        Object serialized = expected.toSerializable(DataStoreVersions.CURRENT_VERSION);
        assertEquals("Serialized type", ExternalizableWriteData.class, serialized.getClass());
        assertEquals("Version", DataStoreVersions.CURRENT_VERSION,
                ((ExternalizableWriteData)serialized).getVersion());

        Object clone = SerializationUtils.clone((Serializable) serialized);
        assertEquals("Version", DataStoreVersions.CURRENT_VERSION,
                ((ExternalizableWriteData)clone).getVersion());
        WriteData actual = WriteData.fromSerializable(clone);
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
     * Tests backwards compatible serialization/deserialization of a WriteData message with the
     * base and R1 Helium versions, which used the protobuff WriteData message.
     */
    @Test
    public void testSerializationWithHeliumR1Version() throws Exception {
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        WriteData expected = new WriteData(path, data);

        Object serialized = expected.toSerializable(DataStoreVersions.HELIUM_1_VERSION);
        assertEquals("Serialized type", ShardTransactionMessages.WriteData.class, serialized.getClass());

        WriteData actual = WriteData.fromSerializable(SerializationUtils.clone((Serializable) serialized));
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getData", expected.getData(), actual.getData());
    }
}
