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

public class MergeDataTest {

    @Test
    public void testSerialization() {
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        MergeData expected = new MergeData(path, data);

        Object serialized = expected.toSerializable(DataStoreVersions.CURRENT_VERSION);
        assertEquals("Serialized type", ExternalizableMergeData.class, serialized.getClass());
        assertEquals("Version", DataStoreVersions.CURRENT_VERSION,
                ((ExternalizableMergeData)serialized).getVersion());

        Object clone = SerializationUtils.clone((Serializable) serialized);
        assertEquals("Version", DataStoreVersions.CURRENT_VERSION,
                ((ExternalizableMergeData)clone).getVersion());
        MergeData actual = MergeData.fromSerializable(clone);
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getData", expected.getData(), actual.getData());
    }

    @Test
    public void testIsSerializedType() {
        assertEquals("isSerializedType", true, MergeData.isSerializedType(
                ShardTransactionMessages.MergeData.newBuilder()
                    .setInstanceIdentifierPathArguments(InstanceIdentifier.getDefaultInstance())
                    .setNormalizedNode(Node.getDefaultInstance()).build()));
        assertEquals("isSerializedType", true,
                MergeData.isSerializedType(new ExternalizableMergeData()));
        assertEquals("isSerializedType", false, MergeData.isSerializedType(new Object()));
    }

    /**
     * Tests backwards compatible serialization/deserialization of a MergeData message with the
     * base and R1 Helium versions, which used the protobuff MergeData message.
     */
    @Test
    public void testSerializationWithHeliumR1Version() throws Exception {
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        MergeData expected = new MergeData(path, data);

        Object serialized = expected.toSerializable(DataStoreVersions.HELIUM_1_VERSION);
        assertEquals("Serialized type", ShardTransactionMessages.MergeData.class, serialized.getClass());

        MergeData actual = MergeData.fromSerializable(SerializationUtils.clone((Serializable) serialized));
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getData", expected.getData(), actual.getData());
    }
}
