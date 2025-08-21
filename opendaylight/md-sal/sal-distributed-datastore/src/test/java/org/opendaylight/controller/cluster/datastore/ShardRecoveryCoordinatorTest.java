/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.TreeType;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class ShardRecoveryCoordinatorTest extends AbstractTest {
    private ShardDataTree peopleDataTree;
    private EffectiveModelContext peopleSchemaContext;
    private EffectiveModelContext carsSchemaContext;
    private ShardRecoveryCoordinator coordinator;

    @Before
    public void setUp() {
        peopleSchemaContext = SchemaContextHelper.select(SchemaContextHelper.PEOPLE_YANG);
        carsSchemaContext = SchemaContextHelper.select(SchemaContextHelper.CARS_YANG);

        final Shard mockShard = Mockito.mock(Shard.class);

        peopleDataTree = new ShardDataTree(mockShard, peopleSchemaContext, TreeType.OPERATIONAL);
        coordinator = new ShardRecoveryCoordinator(peopleDataTree, "foobar");
        coordinator.startLogRecoveryBatch(10);
    }

    @Test
    public void testAppendRecoveredLogEntryCommitTransactionPayload() throws IOException,
            DataValidationFailedException {
        try {
            coordinator.appendRecoveredCommand(CommitTransactionPayload.create(nextTransactionId(), createCar()));
        } catch (final SchemaValidationFailedException e) {
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }

        coordinator.applyCurrentLogRecoveryBatch();
    }

    @Test
    public void testApplyRecoveredSnapshot() throws DataValidationFailedException {
        coordinator.applyRecoveredSnapshot(createSnapshot());

        assertFalse(readCars(peopleDataTree).isPresent());
        assertTrue(readPeople(peopleDataTree).isPresent());
    }


    @Test
    public void testApplyCurrentLogRecoveryBatch() {
        try {
            coordinator.applyCurrentLogRecoveryBatch();
        } catch (final IllegalArgumentException e) {
            fail("IllegalArgumentException should not happen - if the pruning modification delegate is passed");
        }
    }

    private DataTreeCandidate createCar() throws DataValidationFailedException {
        final DataTree dataTree = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_OPERATIONAL, carsSchemaContext);

        final DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        final DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());
        modification.ready();
        return dataTree.prepare(modification);
    }

    private Optional<NormalizedNode> readCars(final ShardDataTree shardDataTree) {
        final DataTree dataTree = shardDataTree.getDataTree();
        // FIXME: this should not be called here
        dataTree.setEffectiveModelContext(peopleSchemaContext);

        return shardDataTree.readNode(CarsModel.BASE_PATH);
    }

    private Optional<NormalizedNode> readPeople(final ShardDataTree shardDataTree) {
        final DataTree dataTree = shardDataTree.getDataTree();
        // FIXME: this should not be called here
        dataTree.setEffectiveModelContext(peopleSchemaContext);

        return shardDataTree.readNode(PeopleModel.BASE_PATH);
    }

    private static ShardSnapshotState createSnapshot() throws DataValidationFailedException {
        final DataTree dataTree = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_OPERATIONAL, SchemaContextHelper.select(SchemaContextHelper.CARS_YANG,
                SchemaContextHelper.PEOPLE_YANG));

        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());
        modification.merge(PeopleModel.BASE_PATH, PeopleModel.create());
        modification.ready();
        dataTree.commit(dataTree.prepare(modification));

        return new ShardSnapshotState(new MetadataShardDataTreeSnapshot(dataTree.takeSnapshot().readNode(
                YangInstanceIdentifier.of()).orElseThrow()));
    }
}
