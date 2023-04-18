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
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNodeEntry;

import com.google.common.reflect.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.data.tree.api.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.TreeType;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

@RunWith(MockitoJUnitRunner.class)
public class PruningDataTreeModificationTest {
    static final QName INVALID_TEST_QNAME = QName.create(TestModel.TEST_QNAME, "invalid");
    static final YangInstanceIdentifier INVALID_TEST_PATH = YangInstanceIdentifier.of(INVALID_TEST_QNAME);

    private static EffectiveModelContext SCHEMA_CONTEXT;
    private static DataSchemaContextTree CONTEXT_TREE;

    @Mock
    private DataTreeModification mockModification;

    private DataTree dataTree;
    private DataTreeModification realModification;
    private DataTreeModification proxyModification;
    private PruningDataTreeModification pruningDataTreeModification;

    @BeforeClass
    public static void beforeClass() {
        SCHEMA_CONTEXT = SchemaContextHelper.select(SchemaContextHelper.CARS_YANG,
            SchemaContextHelper.ODL_DATASTORE_TEST_YANG);
        CONTEXT_TREE = DataSchemaContextTree.from(SCHEMA_CONTEXT);
    }

    @Before
    @SuppressWarnings("checkstyle:avoidHidingCauseException")
    public void setUp() {
        dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION,
            SCHEMA_CONTEXT);

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
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        doThrow(SchemaValidationFailedException.class).when(mockModification).delete(path);

        pruningDataTreeModification.delete(path);

        verify(mockModification, times(1)).delete(path);
    }


    @Test
    public void testMerge() {
        NormalizedNode normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);
    }

    @Test
    public void testMergeWithInvalidNamespace() throws DataValidationFailedException {
        NormalizedNode normalizedNode = PeopleModel.emptyContainer();
        YangInstanceIdentifier path = PeopleModel.BASE_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);

        DataTreeCandidate candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().getModificationType());
    }

    @Test
    public void testMergeWithInvalidChildNodeNames() throws DataValidationFailedException {
        DataContainerChild outerNode = outerNode(outerNodeEntry(1, innerNode("one", "two")));
        ContainerNode normalizedNode = Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outerNode)
            .withChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(AUG_CONTAINER))
                .withChild(ImmutableNodes.containerNode(AUG_INNER_CONTAINER))
                .build())
            .withChild(ImmutableNodes.leafNode(AUG_QNAME, "aug"))
            .build();

        YangInstanceIdentifier path = TestModel.TEST_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        dataTree.commit(getCandidate());

        ContainerNode prunedNode = Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outerNode)
            .build();

        assertEquals("After pruning", Optional.of(prunedNode), dataTree.takeSnapshot().readNode(path));
    }

    @Test
    public void testMergeWithValidNamespaceAndInvalidNodeName() throws DataValidationFailedException {
        NormalizedNode normalizedNode = ImmutableNodes.containerNode(INVALID_TEST_QNAME);
        YangInstanceIdentifier path = INVALID_TEST_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);

        DataTreeCandidate candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().getModificationType());
    }

    @Test
    public void testWrite() {
        NormalizedNode normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        pruningDataTreeModification.write(path, normalizedNode);

        verify(mockModification, times(1)).write(path, normalizedNode);
    }

    @Test
    public void testWriteRootNode() throws Exception {
        final DataTree localDataTree = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_CONFIGURATION, SCHEMA_CONTEXT);

        DataTreeModification mod = localDataTree.takeSnapshot().newModification();
        mod.write(CarsModel.BASE_PATH, CarsModel.create());
        mod.ready();
        localDataTree.validate(mod);
        localDataTree.commit(localDataTree.prepare(mod));

        NormalizedNode normalizedNode = dataTree.takeSnapshot().readNode(YangInstanceIdentifier.empty()).orElseThrow();
        pruningDataTreeModification.write(YangInstanceIdentifier.empty(), normalizedNode);
        dataTree.commit(getCandidate());

        assertEquals(Optional.of(normalizedNode), dataTree.takeSnapshot().readNode(YangInstanceIdentifier.empty()));
    }

    @Test
    public void testWriteRootNodeWithInvalidChild() throws Exception {
        final Shard mockShard = Mockito.mock(Shard.class);

        ShardDataTree shardDataTree = new ShardDataTree(mockShard, SCHEMA_CONTEXT, TreeType.CONFIGURATION);
        NormalizedNode root = shardDataTree.readNode(YangInstanceIdentifier.empty()).orElseThrow();

        NormalizedNode normalizedNode = Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(root.getIdentifier().getNodeType()))
            .withChild(ImmutableNodes.containerNode(AUG_CONTAINER))
            .build();
        pruningDataTreeModification.write(YangInstanceIdentifier.empty(), normalizedNode);
        dataTree.commit(getCandidate());

        assertEquals(Optional.of(root), dataTree.takeSnapshot().readNode(YangInstanceIdentifier.empty()));

    }

    @Test
    public void testWriteWithInvalidNamespace() throws DataValidationFailedException {
        NormalizedNode normalizedNode = PeopleModel.emptyContainer();
        YangInstanceIdentifier path = PeopleModel.BASE_PATH;

        pruningDataTreeModification.write(path, normalizedNode);

        verify(mockModification, times(1)).write(path, normalizedNode);

        DataTreeCandidate candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().getModificationType());
    }

    @Test
    public void testWriteWithInvalidChildNodeNames() throws DataValidationFailedException {
        DataContainerChild outerNode = outerNode(outerNodeEntry(1, innerNode("one", "two")));
        ContainerNode normalizedNode = Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outerNode)
            .withChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(AUG_CONTAINER))
                .withChild(ImmutableNodes.containerNode(AUG_INNER_CONTAINER))
                .build())
            .withChild(ImmutableNodes.leafNode(AUG_QNAME, "aug"))
            .withChild(ImmutableNodes.leafNode(NAME_QNAME, "name"))
            .build();

        YangInstanceIdentifier path = TestModel.TEST_PATH;

        pruningDataTreeModification.write(path, normalizedNode);

        dataTree.commit(getCandidate());

        ContainerNode prunedNode = Builders.containerBuilder()
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
        DataTreeModificationCursor dataTreeModificationCursor = mock(DataTreeModificationCursor.class);
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
        DataTreeModification dataTreeModification = pruningDataTreeModification.newModification();

        assertTrue("new modification not of type PruningDataTreeModification",
                dataTreeModification instanceof PruningDataTreeModification);
    }

    private DataTreeCandidate getCandidate() throws DataValidationFailedException {
        pruningDataTreeModification.ready();
        DataTreeModification mod = pruningDataTreeModification.delegate();
        mod = mod == proxyModification ? realModification : mod;
        dataTree.validate(mod);
        return dataTree.prepare(mod);
    }
}
