package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ShardDataTreeTest {

    @Test
    public void testWrite() throws ExecutionException, InterruptedException {

        SchemaContext schemaContext = SchemaContextHelper.full();

        modify(new ShardDataTree(schemaContext), false, true, true);
    }

    @Test
    public void testWriteWithMissingSchema() throws ExecutionException, InterruptedException {

        SchemaContext schemaContext = SchemaContextHelper.select(SchemaContextHelper.ODL_DATASTORE_TEST_YANG, SchemaContextHelper.PEOPLE_YANG);

        modify(new ShardDataTree(schemaContext), false, false, true);
    }

    @Test
    public void testMerge() throws ExecutionException, InterruptedException {

        SchemaContext schemaContext = SchemaContextHelper.full();

        modify(new ShardDataTree(schemaContext), true, true, true);
    }

    @Test
    public void testMergeWithMissingSchema() throws ExecutionException, InterruptedException {

        SchemaContext schemaContext = SchemaContextHelper.select(SchemaContextHelper.ODL_DATASTORE_TEST_YANG, SchemaContextHelper.PEOPLE_YANG);

        modify(new ShardDataTree(schemaContext), true, false, true);
    }

    private void modify(ShardDataTree shardDataTree, boolean merge, boolean expectedCarsPresent, boolean expectedPeoplePresent) throws ExecutionException, InterruptedException {
        ReadWriteShardDataTreeTransaction transaction = shardDataTree.newReadWriteTransaction("txn-1", null);

        DataTreeModification snapshot = transaction.getSnapshot();

        assertNotNull(snapshot);

        if(merge){
            snapshot.merge(CarsModel.BASE_PATH, CarsModel.create());
            snapshot.merge(PeopleModel.BASE_PATH, PeopleModel.create());
        } else {
            snapshot.write(CarsModel.BASE_PATH, CarsModel.create());
            snapshot.write(PeopleModel.BASE_PATH, PeopleModel.create());
        }

        ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction);

        cohort.preCommit().get();
        cohort.commit().get();


        ReadOnlyShardDataTreeTransaction readOnlyShardDataTreeTransaction = shardDataTree.newReadOnlyTransaction("txn-2", null);

        DataTreeSnapshot snapshot1 = readOnlyShardDataTreeTransaction.getSnapshot();

        Optional<NormalizedNode<?, ?>> optional = snapshot1.readNode(CarsModel.BASE_PATH);

        assertEquals(expectedCarsPresent, optional.isPresent());

        Optional<NormalizedNode<?, ?>> optional1 = snapshot1.readNode(PeopleModel.BASE_PATH);

        assertEquals(expectedPeoplePresent, optional1.isPresent());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testAfterRecoveryDone() throws ExecutionException, InterruptedException {
        SchemaContext schemaContext = SchemaContextHelper.select(SchemaContextHelper.ODL_DATASTORE_TEST_YANG, SchemaContextHelper.PEOPLE_YANG);
        ShardDataTree shardDataTree = new ShardDataTree(schemaContext);
        assertTrue("transaction factory must be the recovery transaction factory",
                shardDataTree.getTransactionFactory() instanceof ShardDataTree.RecoveryShardDataTreeTransactionFactory);
        shardDataTree.recoveryDone();
        assertTrue("transaction factory must be the regular transaction factory",
                shardDataTree.getTransactionFactory() instanceof ShardDataTree.RegularShardDataTreeTransactionFactory);

        modify(shardDataTree, true, false, true);

    }

}