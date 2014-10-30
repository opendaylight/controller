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
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for ReadDataReply.
 *
 * @author Thomas Pantelis
 */
public class ReadDataReplyTest {

    @Test
    public void testSerialization() {
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        ReadDataReply expected = new ReadDataReply(data);

        Object serialized = expected.toSerializable(DataStoreVersions.CURRENT_VERSION);
        assertEquals("Serialized type", ExternalizableReadDataReply.class, serialized.getClass());

        ReadDataReply actual = ReadDataReply.fromSerializable(SerializationUtils.clone(
                (Serializable) serialized));
        assertEquals("getNormalizedNode", expected.getNormalizedNode(), actual.getNormalizedNode());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, ReadDataReply.isSerializedType(
                ShardTransactionMessages.ReadDataReply.newBuilder().build()));
        assertEquals("isSerializedType", true,
                ReadDataReply.isSerializedType(new ExternalizableReadDataReply()));
        assertEquals("isSerializedType", false, ReadDataReply.isSerializedType(new Object()));
    }

    /**
     * Tests backwards compatible serialization/deserialization of a ReadDataReply message with the
     * base and R1 Helium versions, which used the protobuff ReadDataReply message.
     */
    @Test
    public void testSerializationWithHeliumR1Version() throws Exception {
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        ReadDataReply expected = new ReadDataReply(data);

        Object serialized = expected.toSerializable(DataStoreVersions.HELIUM_1_VERSION);
        assertEquals("Serialized type", ShardTransactionMessages.ReadDataReply.class, serialized.getClass());

        ReadDataReply actual = ReadDataReply.fromSerializable(SerializationUtils.clone(
                (Serializable) serialized));
        assertEquals("getNormalizedNode", expected.getNormalizedNode(), actual.getNormalizedNode());
    }
}
