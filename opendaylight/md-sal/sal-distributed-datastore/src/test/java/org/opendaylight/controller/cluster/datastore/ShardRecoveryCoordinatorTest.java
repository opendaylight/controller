/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import com.google.common.base.Optional;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
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
import org.slf4j.LoggerFactory;

public class ShardRecoveryCoordinatorTest {

    private ShardDataTree peopleDataTree;
    private SchemaContext peopleSchemaContext;
    private SchemaContext carsSchemaContext;

    @Before
    public void setUp(){
        peopleSchemaContext = SchemaContextHelper.select(SchemaContextHelper.PEOPLE_YANG);
        carsSchemaContext = SchemaContextHelper.select(SchemaContextHelper.CARS_YANG);

        peopleDataTree = new ShardDataTree(peopleSchemaContext, TreeType.OPERATIONAL);
    }

    @Test
    public void testAppendRecoveredLogEntryDataTreeCandidatePayload(){
        final ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree,
                peopleSchemaContext, null, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);
        try {
            coordinator.appendRecoveredLogEntry(DataTreeCandidatePayload.create(createCar()));
        } catch(final SchemaValidationFailedException e){
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }

        coordinator.applyCurrentLogRecoveryBatch();
    }

    @Test
    public void testAppendRecoveredLogEntryCompositeModificationPayload() throws IOException {
        final ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree,
                peopleSchemaContext, null, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);
        try {
            final MutableCompositeModification modification  = new MutableCompositeModification((short) 1);
            modification.addModification(new WriteModification(CarsModel.BASE_PATH, CarsModel.create()));
            coordinator.appendRecoveredLogEntry(new CompositeModificationPayload(modification.toSerializable()));
        } catch(final SchemaValidationFailedException e){
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }
    }

    @Test
    public void testAppendRecoveredLogEntryCompositeModificationByteStringPayload() throws IOException {
        final ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree,
                peopleSchemaContext, null, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);
        try {
            final MutableCompositeModification modification  = new MutableCompositeModification((short) 1);
            modification.addModification(new WriteModification(CarsModel.BASE_PATH, CarsModel.create()));
            coordinator.appendRecoveredLogEntry(new CompositeModificationByteStringPayload(modification.toSerializable()));
        } catch(final SchemaValidationFailedException e){
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }

        assertEquals(false, readCars(peopleDataTree).isPresent());
    }

    @Test
    public void testApplyRecoverySnapshot(){
        final ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree,
                peopleSchemaContext, null, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);

        coordinator.applyRecoverySnapshot(createSnapshot());

        assertEquals(false, readCars(peopleDataTree).isPresent());
        assertEquals(true, readPeople(peopleDataTree).isPresent());
    }


    @Test
    public void testApplyCurrentLogRecoveryBatch(){
        final ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree,
                peopleSchemaContext, null, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);

        try {
            coordinator.applyCurrentLogRecoveryBatch();
        } catch(final IllegalArgumentException e){
            fail("IllegalArgumentException should not happen - if the pruning modification delegate is passed");
        }
    }

    private DataTreeCandidateTip createCar(){
        final TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        dataTree.setSchemaContext(carsSchemaContext);

        final DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        final DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());
        modification.ready();
        return dataTree.prepare(modification);
    }

    private Optional<NormalizedNode<?,?>> readCars(final ShardDataTree shardDataTree){
        final TipProducingDataTree dataTree = shardDataTree.getDataTree();
        // FIXME: this should not be called here
        dataTree.setSchemaContext(peopleSchemaContext);

        return shardDataTree.readNode(CarsModel.BASE_PATH);
    }

    private Optional<NormalizedNode<?,?>> readPeople(final ShardDataTree shardDataTree){
        final TipProducingDataTree dataTree = shardDataTree.getDataTree();
        // FIXME: this should not be called here
        dataTree.setSchemaContext(peopleSchemaContext);

        return shardDataTree.readNode(PeopleModel.BASE_PATH);
    }

    private static byte[] createSnapshot(){
        final TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        dataTree.setSchemaContext(SchemaContextHelper.select(SchemaContextHelper.CARS_YANG, SchemaContextHelper.PEOPLE_YANG));

        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());
        modification.merge(PeopleModel.BASE_PATH, PeopleModel.create());
        modification.ready();
        final DataTreeCandidateTip prepare = dataTree.prepare(modification);

        dataTree.commit(prepare);

        snapshot = dataTree.takeSnapshot();

        modification = snapshot.newModification();

        final Optional<NormalizedNode<?, ?>> optional = modification.readNode(YangInstanceIdentifier.EMPTY);

        final byte[] bytes = SerializationUtils.serializeNormalizedNode(optional.get());

        return bytes;


    }
}