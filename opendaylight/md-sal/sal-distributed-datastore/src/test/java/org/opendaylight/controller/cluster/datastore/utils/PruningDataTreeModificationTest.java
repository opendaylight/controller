/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.md.cluster.datastore.model.CompositeModel.AUG_CONTAINER;
import static org.opendaylight.controller.md.cluster.datastore.model.CompositeModel.AUG_INNER_CONTAINER;
import static org.opendaylight.controller.md.cluster.datastore.model.CompositeModel.AUG_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.NAME_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntry;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNode;

import com.google.common.reflect.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.ReusableNormalizedNodePruner;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.data.tree.api.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.TreeType;
import org.opendaylight.yangtools.yang.data.tree.dagger.ReferenceDataTreeFactoryModule;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

@RunWith(MockitoJUnitRunner.class)
public class PruningDataTreeModificationTest {
    static final QName INVALID_TEST_QNAME = QName.create(TestModel.TEST_QNAME, "invalid");
    static final YangInstanceIdentifier INVALID_TEST_PATH = YangInstanceIdentifier.of(INVALID_TEST_QNAME);

    private static EffectiveModelContext MODEL_CONTEXT;
    private static DataSchemaContextTree CONTEXT_TREE;

    @Mock
    private DataTreeModification mockModification;

    private DataTree dataTree;
    private DataTreeModification realModification;
    private DataTreeModification proxyModification;
    private PruningDataTreeModification pruningDataTreeModification;

    @BeforeClass
    public static void beforeClass() {
        MODEL_CONTEXT = SchemaContextHelper.select(SchemaContextHelper.CARS_YANG,
            SchemaContextHelper.ODL_DATASTORE_TEST_YANG);
        CONTEXT_TREE = DataSchemaContextTree.from(MODEL_CONTEXT);
    }

    @Before
    @SuppressWarnings("checkstyle:avoidHidingCauseException")
    public void setUp() {
        dataTree = ReferenceDataTreeFactoryModule.provideDataTreeFactory()
            .create(DataTreeConfiguration.DEFAULT_CONFIGURATION, MODEL_CONTEXT);

        realModification = dataTree.takeSnapshot().newModification();
        proxyModification = Reflection.newProxy(DataTreeModification.class, (proxy, method, args) -> {
            try {
                method.invoke(mockModification, args);
                return method.invoke(realModification, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });

        pruningDataTreeModification = new PruningDataTreeModification.Reactive(proxyModification, dataTree,
            // Cannot reuse with parallel tests
            ReusableNormalizedNodePruner.forDataSchemaContext(CONTEXT_TREE));
    }

    @Test
    public void testDelete() {
        pruningDataTreeModification.delete(CarsModel.BASE_PATH);

        verify(mockModification, times(1)).delete(CarsModel.BASE_PATH);
    }

    @Test
    public void testDeleteOnException() {
        var path = CarsModel.BASE_PATH;
        doThrow(SchemaValidationFailedException.class).when(mockModification).delete(path);

        pruningDataTreeModification.delete(path);

        verify(mockModification, times(1)).delete(path);
    }


    @Test
    public void testMerge() {
        var normalizedNode = CarsModel.create();
        var path = CarsModel.BASE_PATH;
        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);
    }

    @Test
    public void testMergeWithInvalidNamespace() throws DataValidationFailedException {
        var normalizedNode = PeopleModel.emptyContainer();
        var path = PeopleModel.BASE_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);

        var candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().modificationType());
    }

    @Test
    public void testMergeWithInvalidChildNodeNames() throws DataValidationFailedException {
        var outerNode = outerNode(outerEntry(1, innerNode("one", "two")));
        var normalizedNode = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outerNode)
            .withChild(ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(AUG_CONTAINER))
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(new NodeIdentifier(AUG_INNER_CONTAINER))
                    .build())
                .build())
            .withChild(ImmutableNodes.leafNode(AUG_QNAME, "aug"))
            .build();

        var path = TestModel.TEST_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        dataTree.commit(getCandidate());

        var prunedNode = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outerNode)
            .build();

        assertEquals("After pruning", Optional.of(prunedNode), dataTree.takeSnapshot().readNode(path));
    }

    @Test
    public void testMergeWithValidNamespaceAndInvalidNodeName() throws DataValidationFailedException {
        var normalizedNode = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(INVALID_TEST_QNAME))
            .build();
        var path = INVALID_TEST_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);

        DataTreeCandidate candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().modificationType());
    }

    @Test
    public void testWrite() {
        var normalizedNode = CarsModel.create();
        var path = CarsModel.BASE_PATH;
        pruningDataTreeModification.write(path, normalizedNode);

        verify(mockModification, times(1)).write(path, normalizedNode);
    }

    @Test
    public void testWriteRootNode() throws Exception {
        final var localDataTree = ReferenceDataTreeFactoryModule.provideDataTreeFactory()
            .create(DataTreeConfiguration.DEFAULT_CONFIGURATION, MODEL_CONTEXT);

        var mod = localDataTree.takeSnapshot().newModification();
        mod.write(CarsModel.BASE_PATH, CarsModel.create());
        mod.ready();
        localDataTree.validate(mod);
        localDataTree.commit(localDataTree.prepare(mod));

        var normalizedNode = dataTree.takeSnapshot().readNode(YangInstanceIdentifier.of()).orElseThrow();
        pruningDataTreeModification.write(YangInstanceIdentifier.of(), normalizedNode);
        dataTree.commit(getCandidate());

        assertEquals(Optional.of(normalizedNode), dataTree.takeSnapshot().readNode(YangInstanceIdentifier.of()));
    }

    @Test
    public void testWriteRootNodeWithInvalidChild() throws Exception {
        final var mockShard = mock(Shard.class);

        var shardDataTree = new ShardDataTree(mockShard, MODEL_CONTEXT, TreeType.CONFIGURATION);
        var root = shardDataTree.readNode(YangInstanceIdentifier.of()).orElseThrow();

        var normalizedNode = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(root.name().getNodeType()))
            .withChild(ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(AUG_CONTAINER))
                .build())
            .build();
        pruningDataTreeModification.write(YangInstanceIdentifier.of(), normalizedNode);
        dataTree.commit(getCandidate());

        assertEquals(Optional.of(root), dataTree.takeSnapshot().readNode(YangInstanceIdentifier.of()));

    }

    @Test
    public void testWriteWithInvalidNamespace() throws DataValidationFailedException {
        var normalizedNode = PeopleModel.emptyContainer();
        var path = PeopleModel.BASE_PATH;

        pruningDataTreeModification.write(path, normalizedNode);

        verify(mockModification, times(1)).write(path, normalizedNode);

        var candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().modificationType());
    }

    @Test
    public void testWriteWithInvalidChildNodeNames() throws DataValidationFailedException {
        var outerNode = outerNode(outerEntry(1, innerNode("one", "two")));
        var normalizedNode = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outerNode)
            .withChild(ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(AUG_CONTAINER))
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(new NodeIdentifier(AUG_INNER_CONTAINER))
                    .build())
                .build())
            .withChild(ImmutableNodes.leafNode(AUG_QNAME, "aug"))
            .withChild(ImmutableNodes.leafNode(NAME_QNAME, "name"))
            .build();

        var path = TestModel.TEST_PATH;

        pruningDataTreeModification.write(path, normalizedNode);

        dataTree.commit(getCandidate());

        var prunedNode = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outerNode)
            .withChild(ImmutableNodes.leafNode(NAME_QNAME, "name"))
            .build();

        assertEquals(Optional.of(prunedNode), dataTree.takeSnapshot().readNode(path));
    }

    @Test
    public void testReady() {
        pruningDataTreeModification.ready();

        verify(mockModification).ready();
    }

    @Test
    public void testApplyToCursor() {
        var dataTreeModificationCursor = mock(DataTreeModificationCursor.class);
        pruningDataTreeModification.applyToCursor(dataTreeModificationCursor);

        verify(mockModification).applyToCursor(dataTreeModificationCursor);
    }

    @Test
    public void testReadNode() {
        pruningDataTreeModification.readNode(CarsModel.BASE_PATH);

        verify(mockModification).readNode(CarsModel.BASE_PATH);
    }

    @Test
    public void testNewModification() {
        realModification.ready();
        var dataTreeModification = pruningDataTreeModification.newModification();

        assertTrue("new modification not of type PruningDataTreeModification",
                dataTreeModification instanceof PruningDataTreeModification);
    }

    private DataTreeCandidate getCandidate() throws DataValidationFailedException {
        pruningDataTreeModification.ready();
        var mod = pruningDataTreeModification.delegate();
        mod = mod == proxyModification ? realModification : mod;
        dataTree.validate(mod);
        return dataTree.prepare(mod);
    }
}
