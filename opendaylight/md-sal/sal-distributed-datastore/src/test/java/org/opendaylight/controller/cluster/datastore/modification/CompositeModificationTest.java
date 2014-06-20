package org.opendaylight.controller.cluster.datastore.modification;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class CompositeModificationTest extends AbstractModificationTest {

  @Test
  public void testApply() throws Exception {

    CompositeModification compositeModification = new CompositeModification();
    compositeModification.addModification(new WriteModification(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME)));

    DOMStoreReadWriteTransaction transaction = store.newReadWriteTransaction();
    compositeModification.apply(transaction);

    //Commit it
    DOMStoreThreePhaseCommitCohort cohort = transaction.ready();
    cohort.preCommit();
    cohort.commit();

    DOMStoreReadTransaction readTransaction = store.newReadOnlyTransaction();

    ListenableFuture<Optional<NormalizedNode<?, ?>>> future = readTransaction.read(TestModel.TEST_PATH);

    Assert.assertNotNull(future.get().get());
    Assert.assertEquals(TestModel.TEST_QNAME, future.get().get().getNodeType());
  }
}