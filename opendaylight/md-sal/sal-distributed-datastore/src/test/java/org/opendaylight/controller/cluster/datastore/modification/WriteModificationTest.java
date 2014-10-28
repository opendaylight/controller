package org.opendaylight.controller.cluster.datastore.modification;

import com.google.common.base.Optional;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class WriteModificationTest extends AbstractModificationTest{

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
        NormalizedNode<?, ?> node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        WriteModification expected = new WriteModification(TestModel.TEST_PATH, node);
        WriteModification actual = (WriteModification) SerializationUtils.clone(expected);
        Assert.assertEquals("getPath", TestModel.TEST_PATH, actual.getPath());
        Assert.assertEquals("getData", node, actual.getData());
    }
}
