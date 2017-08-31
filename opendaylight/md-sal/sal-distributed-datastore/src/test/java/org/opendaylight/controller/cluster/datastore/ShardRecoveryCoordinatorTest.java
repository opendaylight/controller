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

import com.google.common.base.Optional;
import java.io.IOException;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShardRecoveryCoordinatorTest extends AbstractTest {
    private static final Logger FOO_LOGGER = LoggerFactory.getLogger("foo");

    private ShardDataTree peopleDataTree;
    private SchemaContext peopleSchemaContext;
    private SchemaContext carsSchemaContext;
    private ShardRecoveryCoordinator coordinator;

    @Before
    public void setUp() {
        peopleSchemaContext = SchemaContextHelper.select(SchemaContextHelper.PEOPLE_YANG);
        carsSchemaContext = SchemaContextHelper.select(SchemaContextHelper.CARS_YANG);

        final Shard mockShard = Mockito.mock(Shard.class);

        peopleDataTree = new ShardDataTree(mockShard, peopleSchemaContext, TreeType.OPERATIONAL);
        coordinator = ShardRecoveryCoordinator.create(peopleDataTree, "foobar", FOO_LOGGER);
        coordinator.startLogRecoveryBatch(10);
    }

    @Test
    public void testAppendRecoveredLogEntryCommitTransactionPayload() throws IOException {
        try {
            coordinator.appendRecoveredLogEntry(CommitTransactionPayload.create(nextTransactionId(), createCar()));
        } catch (final SchemaValidationFailedException e) {
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }

        coordinator.applyCurrentLogRecoveryBatch();
    }

    @Test
    public void testApplyRecoverySnapshot() {
        coordinator.applyRecoverySnapshot(createSnapshot());

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

    private DataTreeCandidateTip createCar() {
        final TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        dataTree.setSchemaContext(carsSchemaContext);

        final DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        final DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());
        modification.ready();
        return dataTree.prepare(modification);
    }

    private Optional<NormalizedNode<?,?>> readCars(final ShardDataTree shardDataTree) {
        final TipProducingDataTree dataTree = shardDataTree.getDataTree();
        // FIXME: this should not be called here
        dataTree.setSchemaContext(peopleSchemaContext);

        return shardDataTree.readNode(CarsModel.BASE_PATH);
    }

    private Optional<NormalizedNode<?,?>> readPeople(final ShardDataTree shardDataTree) {
        final TipProducingDataTree dataTree = shardDataTree.getDataTree();
        // FIXME: this should not be called here
        dataTree.setSchemaContext(peopleSchemaContext);

        return shardDataTree.readNode(PeopleModel.BASE_PATH);
    }

    private static ShardSnapshotState createSnapshot() {
        final TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        dataTree.setSchemaContext(SchemaContextHelper.select(SchemaContextHelper.CARS_YANG,
                SchemaContextHelper.PEOPLE_YANG));

        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());
        modification.merge(PeopleModel.BASE_PATH, PeopleModel.create());
        modification.ready();
        dataTree.commit(dataTree.prepare(modification));

        return new ShardSnapshotState(new MetadataShardDataTreeSnapshot(dataTree.takeSnapshot().readNode(
                YangInstanceIdentifier.EMPTY).get()));
    }
}
