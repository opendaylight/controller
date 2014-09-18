package org.opendaylight.controller.cluster.datastore.modification;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class MergeModificationTest extends AbstractModificationTest{

    @Test
    public void testApply() throws Exception {
        //TODO : Need to write a better test for this

        //Write something into the datastore
        DOMStoreReadWriteTransaction writeTransaction = store.newReadWriteTransaction();
        MergeModification writeModification = new MergeModification(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME), TestModel.createTestContext());
        writeModification.apply(writeTransaction);
        commitTransaction(writeTransaction);

        //Check if it's in the datastore
        Optional<NormalizedNode<?,?>> data = readData(TestModel.TEST_PATH);
        Assert.assertTrue(data.isPresent());

    }

    @Test
    public void testSerialization() {
        SchemaContext schemaContext = TestModel.createTestContext();
        NormalizedNode<?, ?> node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        MergeModification mergeModification = new MergeModification(TestModel.TEST_PATH,
                node, schemaContext);

        Object serialized = mergeModification.toSerializable();

        MergeModification newModification = MergeModification.fromSerializable(serialized, schemaContext);

        Assert.assertEquals("getPath", TestModel.TEST_PATH, newModification.getPath());
        Assert.assertEquals("getData", node, newModification.getData());
    }
}
