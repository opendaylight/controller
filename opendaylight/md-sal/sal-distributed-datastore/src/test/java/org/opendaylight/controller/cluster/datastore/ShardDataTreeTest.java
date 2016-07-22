/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ShardDataTreeTest extends AbstractTest {

    SchemaContext fullSchema;

    @Before
    public void setUp(){
        fullSchema = SchemaContextHelper.full();
    }

    @Test
    public void testWrite() throws Exception {
        modify(new ShardDataTree(fullSchema, TreeType.OPERATIONAL), false, true, true);
    }

    @Test
    public void testMerge() throws Exception {
        modify(new ShardDataTree(fullSchema, TreeType.OPERATIONAL), true, true, true);
    }

    private void modify(final ShardDataTree shardDataTree, final boolean merge, final boolean expectedCarsPresent,
            final boolean expectedPeoplePresent) throws Exception {

        assertEquals(fullSchema, shardDataTree.getSchemaContext());

        ReadWriteShardDataTreeTransaction transaction = shardDataTree.newReadWriteTransaction(nextTransactionId());

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

        cohort.preCommit();
        cohort.commit();


        ReadOnlyShardDataTreeTransaction readOnlyShardDataTreeTransaction = shardDataTree.newReadOnlyTransaction(nextTransactionId());

        DataTreeSnapshot snapshot1 = readOnlyShardDataTreeTransaction.getSnapshot();

        Optional<NormalizedNode<?, ?>> optional = snapshot1.readNode(CarsModel.BASE_PATH);

        assertEquals(expectedCarsPresent, optional.isPresent());

        Optional<NormalizedNode<?, ?>> optional1 = snapshot1.readNode(PeopleModel.BASE_PATH);

        assertEquals(expectedPeoplePresent, optional1.isPresent());

    }

    @Test
    public void bug4359AddRemoveCarOnce() throws Exception {
        ShardDataTree shardDataTree = new ShardDataTree(fullSchema, TreeType.OPERATIONAL);

        List<DataTreeCandidateTip> candidates = new ArrayList<>();
        candidates.add(addCar(shardDataTree));
        candidates.add(removeCar(shardDataTree));

        NormalizedNode<?, ?> expected = getCars(shardDataTree);

        applyCandidates(shardDataTree, candidates);

        NormalizedNode<?, ?> actual = getCars(shardDataTree);

        assertEquals(expected, actual);
    }

    @Test
    public void bug4359AddRemoveCarTwice() throws Exception {
        ShardDataTree shardDataTree = new ShardDataTree(fullSchema, TreeType.OPERATIONAL);

        List<DataTreeCandidateTip> candidates = new ArrayList<>();
        candidates.add(addCar(shardDataTree));
        candidates.add(removeCar(shardDataTree));
        candidates.add(addCar(shardDataTree));
        candidates.add(removeCar(shardDataTree));

        NormalizedNode<?, ?> expected = getCars(shardDataTree);

        applyCandidates(shardDataTree, candidates);

        NormalizedNode<?, ?> actual = getCars(shardDataTree);

        assertEquals(expected, actual);
    }

    private static NormalizedNode<?, ?> getCars(final ShardDataTree shardDataTree) {
        ReadOnlyShardDataTreeTransaction readOnlyShardDataTreeTransaction = shardDataTree.newReadOnlyTransaction(nextTransactionId());
        DataTreeSnapshot snapshot1 = readOnlyShardDataTreeTransaction.getSnapshot();

        Optional<NormalizedNode<?, ?>> optional = snapshot1.readNode(CarsModel.BASE_PATH);

        assertEquals(true, optional.isPresent());

        return optional.get();
    }

    private static DataTreeCandidateTip addCar(final ShardDataTree shardDataTree) throws Exception {
        return doTransaction(shardDataTree, snapshot -> {
                snapshot.merge(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                snapshot.merge(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                snapshot.write(CarsModel.newCarPath("altima"), CarsModel.newCarEntry("altima", new BigInteger("100")));
            });
    }

    private static DataTreeCandidateTip removeCar(final ShardDataTree shardDataTree) throws Exception {
        return doTransaction(shardDataTree, snapshot -> snapshot.delete(CarsModel.newCarPath("altima")));
    }

    @FunctionalInterface
    private static interface DataTreeOperation {
        void execute(DataTreeModification snapshot);
    }

    private static DataTreeCandidateTip doTransaction(final ShardDataTree shardDataTree, final DataTreeOperation operation)
            throws Exception {
        ReadWriteShardDataTreeTransaction transaction = shardDataTree.newReadWriteTransaction(nextTransactionId());
        DataTreeModification snapshot = transaction.getSnapshot();
        operation.execute(snapshot);
        ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction);

        cohort.canCommit();
        cohort.preCommit();
        DataTreeCandidateTip candidate = cohort.getCandidate();
        cohort.commit();

        return candidate;
    }

    private static DataTreeCandidateTip applyCandidates(final ShardDataTree shardDataTree, final List<DataTreeCandidateTip> candidates)
            throws Exception {
        ReadWriteShardDataTreeTransaction transaction = shardDataTree.newReadWriteTransaction(nextTransactionId());
        DataTreeModification snapshot = transaction.getSnapshot();
        for(DataTreeCandidateTip candidateTip : candidates){
            DataTreeCandidates.applyToModification(snapshot, candidateTip);
        }
        ShardDataTreeCohort cohort = shardDataTree.finishTransaction(transaction);

        cohort.canCommit();
        cohort.preCommit();
        DataTreeCandidateTip candidate = cohort.getCandidate();
        cohort.commit();

        return candidate;
    }

}