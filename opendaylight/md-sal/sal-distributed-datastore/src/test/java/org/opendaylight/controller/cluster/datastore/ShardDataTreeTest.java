package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.utils.YangListChangeListener;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ShardDataTreeTest {

    SchemaContext fullSchema;

    @Before
    public void setUp(){
        fullSchema = SchemaContextHelper.full();
    }

    @Test
    public void testWrite() throws ExecutionException, InterruptedException {
        modify(new ShardDataTree(fullSchema), false, true, true);
    }

    @Test
    public void testMerge() throws ExecutionException, InterruptedException {
        modify(new ShardDataTree(fullSchema), true, true, true);
    }


    private void modify(ShardDataTree shardDataTree, boolean merge, boolean expectedCarsPresent, boolean expectedPeoplePresent) throws ExecutionException, InterruptedException {

        assertEquals(fullSchema, shardDataTree.getSchemaContext());

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

    @Test
    public void testDataChangeListener() throws ExecutionException, InterruptedException {
        final ShardDataTree shardDataTree = new ShardDataTree(fullSchema);

        final List<Map.Entry<YangInstanceIdentifier, NormalizedNode<?,?>>> createdEntries = new ArrayList();
        final List<YangInstanceIdentifier> deletedEntries = new ArrayList<>();
        final AtomicLong listSize = new AtomicLong();


        YangListChangeListener.registerYangListChangeListener(shardDataTree, CarsModel.CAR_LIST_PATH, new YangListChangeListener(CarsModel.CAR_LIST_PATH) {
            @Override
            protected void entryAdded(YangInstanceIdentifier key, NormalizedNode<?, ?> value) {
                createdEntries.add(new AbstractMap.SimpleEntry<YangInstanceIdentifier, NormalizedNode<?, ?>>(key, value));
                listSize.set(listSize());
            }

            @Override
            protected void entryRemoved(YangInstanceIdentifier key) {
                deletedEntries.add(key);
                listSize.set(listSize());
            }
        });

        addCar(shardDataTree, "tesla model s", 100000);
        addCar(shardDataTree, "porsche 911", 100000);
        addCar(shardDataTree, "honda accord", 100000);

        assertEquals(3, createdEntries.size());
        assertEquals(3, listSize.get());

        assertEquals(CarsModel.newCarPath("tesla model s"), createdEntries.get(0).getKey());
        assertEquals(CarsModel.newCarPath("porsche 911"), createdEntries.get(1).getKey());
        assertEquals(CarsModel.newCarPath("honda accord"), createdEntries.get(2).getKey());

        removeCar(shardDataTree, "tesla model s");

        assertEquals(1, deletedEntries.size());
        assertEquals(2, listSize.get());

        assertEquals(CarsModel.newCarPath("tesla model s"), deletedEntries.get(0));
    }

    private void removeCar(ShardDataTree shardDataTree, String carName) throws ExecutionException, InterruptedException {
        ReadWriteShardDataTreeTransaction transaction = shardDataTree.newReadWriteTransaction("txn-1", null);
        DataTreeModification snapshot = transaction.getSnapshot();

        snapshot.delete(CarsModel.newCarPath(carName));

        ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction);

        cohort.preCommit().get();
        cohort.commit().get();

    }


    private void addCar(ShardDataTree shardDataTree, String carName, long price) throws ExecutionException, InterruptedException {
        ReadWriteShardDataTreeTransaction transaction = shardDataTree.newReadWriteTransaction("txn-1", null);

        DataTreeModification snapshot = transaction.getSnapshot();
        snapshot.merge(CarsModel.BASE_PATH, CarsModel.createEmptyCarsList());
        snapshot.merge(CarsModel.newCarPath(carName), CarsModel.newCarEntry(carName, BigInteger.valueOf(price)));

        ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction);

        cohort.preCommit().get();
        cohort.commit().get();

    }

}