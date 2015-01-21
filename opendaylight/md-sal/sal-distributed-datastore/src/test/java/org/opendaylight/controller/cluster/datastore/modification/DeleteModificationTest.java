package org.opendaylight.controller.cluster.datastore.modification;

import static org.junit.Assert.assertEquals;
import com.google.common.base.Optional;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class DeleteModificationTest extends AbstractModificationTest {

    @Test
    public void testApply() throws Exception {
        // Write something into the datastore
        DOMStoreReadWriteTransaction writeTransaction = store.newReadWriteTransaction();
        WriteModification writeModification = new WriteModification(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        writeModification.apply(writeTransaction);
        commitTransaction(writeTransaction);

        // Check if it's in the datastore
        Optional<NormalizedNode<?, ?>> data = readData(TestModel.TEST_PATH);
        Assert.assertTrue(data.isPresent());

        // Delete stuff from the datastore
        DOMStoreWriteTransaction deleteTransaction = store.newWriteOnlyTransaction();
        DeleteModification deleteModification = new DeleteModification(TestModel.TEST_PATH);
        deleteModification.apply(deleteTransaction);
        commitTransaction(deleteTransaction);

        data = readData(TestModel.TEST_PATH);
        Assert.assertFalse(data.isPresent());
    }

    @Test
    public void testSerialization() {
        YangInstanceIdentifier path = TestModel.TEST_PATH;

        DeleteModification expected = new DeleteModification(path);

        DeleteModification clone = (DeleteModification) SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), clone.getPath());
    }
}
