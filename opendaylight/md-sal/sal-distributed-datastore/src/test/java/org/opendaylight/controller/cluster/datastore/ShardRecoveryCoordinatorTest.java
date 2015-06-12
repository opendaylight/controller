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
import org.opendaylight.controller.cluster.datastore.modification.ModificationPayload;
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

        peopleDataTree = new ShardDataTree(peopleSchemaContext);
    }

    @Test
    public void testAppendRecoveredLogEntryDataTreeCandidatePayload(){
        ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree, peopleSchemaContext, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);
        try {
            coordinator.appendRecoveredLogEntry(DataTreeCandidatePayload.create(createCar()));
        } catch(SchemaValidationFailedException e){
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }

        coordinator.applyCurrentLogRecoveryBatch();
    }

    @Test
    public void testAppendRecoveredLogEntryModificationPayload() throws IOException {
        ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree, peopleSchemaContext, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);
        try {
            MutableCompositeModification modification  = new MutableCompositeModification((short) 1);
            modification.addModification(new WriteModification(CarsModel.BASE_PATH, CarsModel.create()));
            coordinator.appendRecoveredLogEntry(new ModificationPayload(modification));
        } catch(SchemaValidationFailedException e){
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }
    }

    @Test
    public void testAppendRecoveredLogEntryCompositeModificationPayload() throws IOException {
        ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree, peopleSchemaContext, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);
        try {
            MutableCompositeModification modification  = new MutableCompositeModification((short) 1);
            modification.addModification(new WriteModification(CarsModel.BASE_PATH, CarsModel.create()));
            coordinator.appendRecoveredLogEntry(new CompositeModificationPayload(modification.toSerializable()));
        } catch(SchemaValidationFailedException e){
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }
    }

    @Test
    public void testAppendRecoveredLogEntryCompositeModificationByteStringPayload() throws IOException {
        ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree, peopleSchemaContext, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);
        try {
            MutableCompositeModification modification  = new MutableCompositeModification((short) 1);
            modification.addModification(new WriteModification(CarsModel.BASE_PATH, CarsModel.create()));
            coordinator.appendRecoveredLogEntry(new CompositeModificationByteStringPayload(modification.toSerializable()));
        } catch(SchemaValidationFailedException e){
            fail("SchemaValidationFailedException should not happen if pruning is done");
        }

        assertEquals(false, readCars(peopleDataTree).isPresent());
    }

    @Test
    public void testApplyRecoverySnapshot(){
        ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree , peopleSchemaContext, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);

        coordinator.applyRecoverySnapshot(createSnapshot());

        assertEquals(false, readCars(peopleDataTree).isPresent());
        assertEquals(true, readPeople(peopleDataTree).isPresent());
    }


    @Test
    public void testApplyCurrentLogRecoveryBatch(){
        ShardRecoveryCoordinator coordinator = new ShardRecoveryCoordinator(peopleDataTree, peopleSchemaContext, "foobar", LoggerFactory.getLogger("foo"));
        coordinator.startLogRecoveryBatch(10);

        try {
            coordinator.applyCurrentLogRecoveryBatch();
        } catch(IllegalArgumentException e){
            fail("IllegalArgumentException should not happen - if the pruning modification delegate is passed");
        }
    }

    private DataTreeCandidateTip createCar(){
        TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
        dataTree.setSchemaContext(carsSchemaContext);

        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());

        return dataTree.prepare(modification);
    }

    private Optional<NormalizedNode<?,?>> readCars(ShardDataTree shardDataTree){
        TipProducingDataTree dataTree = shardDataTree.getDataTree();
        dataTree.setSchemaContext(peopleSchemaContext);

        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        return modification.readNode(CarsModel.BASE_PATH);
    }

    private Optional<NormalizedNode<?,?>> readPeople(ShardDataTree shardDataTree){
        TipProducingDataTree dataTree = shardDataTree.getDataTree();
        dataTree.setSchemaContext(peopleSchemaContext);

        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        return modification.readNode(PeopleModel.BASE_PATH);
    }



    private byte[] createSnapshot(){
        TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
        dataTree.setSchemaContext(SchemaContextHelper.select(SchemaContextHelper.CARS_YANG, SchemaContextHelper.PEOPLE_YANG));

        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        modification.merge(CarsModel.BASE_PATH, CarsModel.create());
        modification.merge(PeopleModel.BASE_PATH, PeopleModel.create());

        DataTreeCandidateTip prepare = dataTree.prepare(modification);

        dataTree.commit(prepare);

        snapshot = dataTree.takeSnapshot();

        modification = snapshot.newModification();

        Optional<NormalizedNode<?, ?>> optional = modification.readNode(YangInstanceIdentifier.EMPTY);

        byte[] bytes = SerializationUtils.serializeNormalizedNode(optional.get());

        return bytes;


    }
}