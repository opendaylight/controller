package org.opendaylight.controller.cluster.datastore.modification;

import static org.junit.Assert.assertEquals;
import com.google.common.base.Optional;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

public class WriteModificationTest extends AbstractModificationTest {

    @Test
    public void testApply() throws Exception {
        //Write something into the datastore
        DOMStoreReadWriteTransaction writeTransaction = store.newReadWriteTransaction();
        WriteModification writeModification = new WriteModification(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        writeModification.apply(writeTransaction);
        commitTransaction(writeTransaction);

        //Check if it's in the datastore
        Optional<NormalizedNode<?,?>> data = readData(TestModel.TEST_PATH);
        Assert.assertTrue(data.isPresent());
    }

    @Test
    public void testSerialization() {
        YangInstanceIdentifier path = TestModel.TEST_PATH;
        NormalizedNode<?, ?> data = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        WriteModification expected = new WriteModification(path, data);

        WriteModification clone = (WriteModification) SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), clone.getPath());
        assertEquals("getData", expected.getData(), clone.getData());
    }
}
