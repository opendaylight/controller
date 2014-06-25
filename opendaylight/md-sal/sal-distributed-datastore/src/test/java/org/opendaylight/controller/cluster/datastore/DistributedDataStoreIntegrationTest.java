package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DistributedDataStoreIntegrationTest extends AbstractActorTest {

    @Test
    public void integrationTest() throws Exception {
        DistributedDataStore distributedDataStore =
            new DistributedDataStore(getSystem(), "config");

        distributedDataStore.onGlobalContextUpdated(TestModel.createTestContext());

        DOMStoreReadWriteTransaction transaction =
            distributedDataStore.newReadWriteTransaction();

        transaction.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        ListenableFuture<Optional<NormalizedNode<?, ?>>> future =
            transaction.read(TestModel.TEST_PATH);

        Optional<NormalizedNode<?, ?>> optional = future.get();

        NormalizedNode<?, ?> normalizedNode = optional.get();

        assertEquals(TestModel.TEST_QNAME, normalizedNode.getNodeType());

        DOMStoreThreePhaseCommitCohort ready = transaction.ready();

        ListenableFuture<Boolean> canCommit = ready.canCommit();

        assertTrue(canCommit.get());

        ListenableFuture<Void> preCommit = ready.preCommit();

        preCommit.get();

        ListenableFuture<Void> commit = ready.commit();

        commit.get();

    }

}
