package org.opendaylight.controller.cluster.datastore.messages;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

public class MergeDataTest {

    @Test
    public void testSerialization() {
        MergeData expected = new MergeData(TestModel.TEST_PATH, ImmutableNodes
            .containerNode(TestModel.TEST_QNAME));

        MergeData actual = MergeData.fromSerializable(expected.toSerializable());
        Assert.assertEquals("getPath", expected.getPath(), actual.getPath());
        Assert.assertEquals("getData", expected.getData(), actual.getData());
    }

    /**
     * Tests backwards compatible deserialization of a MergeData message from the base Helium
     * version (messages version 0).
     */
    @Test
    public void testDeserializationFromVersion0() throws Exception {
        // The path and data node that was serialized in the file.
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        try(FileInputStream fis = new FileInputStream("src/test/resources/SerializedMergeDataV0.out")) {
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);

            ShardTransactionMessages.MergeData serialized =
                    ShardTransactionMessages.MergeData.parseFrom(bytes);

            MergeData actual = MergeData.fromSerializable(serialized);
            Assert.assertEquals("getPath", path, actual.getPath());
            Assert.assertEquals("getData", data, actual.getData());
        }
    }

    // Method to use for writing the serialized bytes to a file
    //@Test
    public void writeSerializedFile() throws Exception {
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        ShardTransactionMessages.MergeData serialized = (ShardTransactionMessages.MergeData)
                new MergeData(path, data).toSerializable();
        try(FileOutputStream fos = new FileOutputStream("src/test/resources/SerializedMergeDataV0.out")) {
            fos.write(serialized.toByteArray());
        }
    }
}
