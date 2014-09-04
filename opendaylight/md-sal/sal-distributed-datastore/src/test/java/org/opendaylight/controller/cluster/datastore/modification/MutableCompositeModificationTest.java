package org.opendaylight.controller.cluster.datastore.modification;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class MutableCompositeModificationTest extends AbstractModificationTest {

    @Test
    public void testApply() throws Exception {

        MutableCompositeModification compositeModification = new MutableCompositeModification();
        compositeModification.addModification(new WriteModification(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), TestModel.createTestContext()));

        DOMStoreReadWriteTransaction transaction = store.newReadWriteTransaction();
        compositeModification.apply(transaction);
        commitTransaction(transaction);

        Optional<NormalizedNode<?, ?>> data = readData(TestModel.TEST_PATH);

        assertNotNull(data.get());
        assertEquals(TestModel.TEST_QNAME, data.get().getNodeType());
    }

    @Test
    public void testEverySerializedCompositeModificationObjectMustBeDifferent(){
        MutableCompositeModification compositeModification = new MutableCompositeModification();
        compositeModification.addModification(new WriteModification(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), TestModel.createTestContext()));

        assertNotEquals(compositeModification.toSerializable(), compositeModification.toSerializable());

    }
}
