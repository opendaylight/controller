package org.opendaylight.controller.cluster.datastore.messages;

import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class MergeDataTest {

    @Test
    public void testBasic(){
        MergeData mergeData = new MergeData(TestModel.TEST_PATH, ImmutableNodes
            .containerNode(TestModel.TEST_QNAME),
            TestModel.createTestContext());

        MergeData output = MergeData
            .fromSerializable(mergeData.toSerializable(),
                TestModel.createTestContext());

    }

    @Test
    public void testNormalizedNodeEncodeDecode(){
        NormalizedNode<?, ?> expected =
            ImmutableNodes.containerNode(TestModel.TEST_QNAME);


        NormalizedNodeMessages.Container node =
            new NormalizedNodeToNodeCodec(TestModel.createTestContext())
                .encode(TestModel.TEST_PATH,
                    expected);

        String parentPath = node.getParentPath();

        NormalizedNodeMessages.Node normalizedNode =
            node.getNormalizedNode();

        NormalizedNode<?,?> actual = new NormalizedNodeToNodeCodec(TestModel.createTestContext()).decode(TestModel.TEST_PATH,
            normalizedNode);


        Assert.assertEquals(expected, actual);
    }
}
