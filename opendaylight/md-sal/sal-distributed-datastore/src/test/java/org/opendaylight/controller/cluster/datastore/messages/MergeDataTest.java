package org.opendaylight.controller.cluster.datastore.messages;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

public class MergeDataTest {

    @Test
    public void testSerialization() {
        MergeData expected = new MergeData(TestModel.TEST_PATH,
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build());

        MergeData actual = (MergeData) SerializationUtils.clone(expected);
        Assert.assertEquals("getPath", expected.getPath(), actual.getPath());
        Assert.assertEquals("getData", expected.getData(), actual.getData());
    }
}
