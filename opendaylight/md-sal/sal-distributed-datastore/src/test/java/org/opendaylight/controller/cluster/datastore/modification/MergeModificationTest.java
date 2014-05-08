package org.opendaylight.controller.cluster.datastore.modification;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class MergeModificationTest extends AbstractModificationTest{

  @Test
  public void testApply() throws Exception {
    //TODO : Need to write a better test for this

    //Write something into the datastore
    DOMStoreReadWriteTransaction writeTransaction = store.newReadWriteTransaction();
    MergeModification writeModification = new MergeModification(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
    writeModification.apply(writeTransaction);
    commitTransaction(writeTransaction);

    //Check if it's in the datastore
    Optional<NormalizedNode<?,?>> data = readData(TestModel.TEST_PATH);
    Assert.assertTrue(data.isPresent());

  }
}