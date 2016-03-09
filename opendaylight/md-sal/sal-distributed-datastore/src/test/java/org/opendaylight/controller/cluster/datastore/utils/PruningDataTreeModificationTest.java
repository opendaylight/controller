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
import com.google.common.base.Optional;
import com.google.common.reflect.Reflection;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class PruningDataTreeModificationTest {
    static final SchemaContext SCHEMA_CONTEXT = SchemaContextHelper.select(SchemaContextHelper.CARS_YANG,
            SchemaContextHelper.ODL_DATASTORE_TEST_YANG);

    static final QName INVALID_TEST_QNAME = QName.create(TestModel.TEST_QNAME, "invalid");
    static final YangInstanceIdentifier INVALID_TEST_PATH = YangInstanceIdentifier.of(INVALID_TEST_QNAME);

    @Mock
    private DataTreeModification mockModification;

    private TipProducingDataTree dataTree;
    private DataTreeModification realModification;
    private DataTreeModification proxyModification;
    private PruningDataTreeModification pruningDataTreeModification;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
        dataTree.setSchemaContext(SCHEMA_CONTEXT);

        realModification = dataTree.takeSnapshot().newModification();
        proxyModification = Reflection.newProxy(DataTreeModification.class, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                try {
                    method.invoke(mockModification, args);
                    return method.invoke(realModification, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        });

        pruningDataTreeModification = new PruningDataTreeModification(proxyModification, dataTree, SCHEMA_CONTEXT);
    }

    @Test
    public void testDelete(){
        pruningDataTreeModification.delete(CarsModel.BASE_PATH);

        verify(mockModification, times(1)).delete(CarsModel.BASE_PATH);
    }

    @Test
    public void testDeleteOnException(){
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        doThrow(SchemaValidationFailedException.class).when(mockModification).delete(path);

        pruningDataTreeModification.delete(path);

        verify(mockModification, times(1)).delete(path);
    }


    @Test
    public void testMerge(){
        NormalizedNode<?, ?> normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);
    }

    @Test
    public void testMergeWithInvalidNamespace() throws DataValidationFailedException{
        NormalizedNode<?, ?> normalizedNode = PeopleModel.emptyContainer();
        YangInstanceIdentifier path = PeopleModel.BASE_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);

        DataTreeCandidateTip candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().getModificationType());
    }

    @Test
    public void testMergeWithInvalidChildNodeNames() throws DataValidationFailedException{
        ContainerNode augContainer = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER)).withChild(
                        ImmutableNodes.containerNode(AUG_INNER_CONTAINER)).build();

        DataContainerChild<?, ?> outerNode = outerNode(outerNodeEntry(1, innerNode("one", "two")));
        ContainerNode normalizedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(outerNode).withChild(augContainer).
                            withChild(ImmutableNodes.leafNode(AUG_QNAME, "aug")).build();

        YangInstanceIdentifier path = TestModel.TEST_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        dataTree.commit(getCandidate());

        ContainerNode prunedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(outerNode).build();

        Optional<NormalizedNode<?, ?>> actual = dataTree.takeSnapshot().readNode(path);
        assertEquals("After pruning present", true, actual.isPresent());
        assertEquals("After pruning", prunedNode, actual.get());
    }

    @Test
    public void testMergeWithValidNamespaceAndInvalidNodeName() throws DataValidationFailedException{
        NormalizedNode<?, ?> normalizedNode = ImmutableNodes.containerNode(INVALID_TEST_QNAME);
        YangInstanceIdentifier path = INVALID_TEST_PATH;

        pruningDataTreeModification.merge(path, normalizedNode);

        verify(mockModification, times(1)).merge(path, normalizedNode);

        DataTreeCandidateTip candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().getModificationType());
    }

    @Test
    public void testWrite(){
        NormalizedNode<?, ?> normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        pruningDataTreeModification.write(path, normalizedNode);

        verify(mockModification, times(1)).write(path, normalizedNode);
    }

    @Test
    public void testWriteRootNode() throws Exception{
        ShardDataTree shardDataTree = new ShardDataTree(SCHEMA_CONTEXT, TreeType.CONFIGURATION);
        DataTreeModification mod = shardDataTree.newModification();

        mod.write(CarsModel.BASE_PATH, CarsModel.create());
        shardDataTree.commit(mod);

        NormalizedNode<?, ?> normalizedNode = shardDataTree.readNode(YangInstanceIdentifier.EMPTY).get();
        pruningDataTreeModification.write(YangInstanceIdentifier.EMPTY, normalizedNode);
        dataTree.commit(getCandidate());

        Optional<NormalizedNode<?, ?>> actual = dataTree.takeSnapshot().readNode(YangInstanceIdentifier.EMPTY);
        assertEquals("Root present", true, actual.isPresent());
        assertEquals("Root node", normalizedNode, actual.get());

    }

    @Test
    public void testWriteRootNodeWithInvalidChild() throws Exception{
        ShardDataTree shardDataTree = new ShardDataTree(SCHEMA_CONTEXT, TreeType.CONFIGURATION);
        NormalizedNode<?, ?> root = shardDataTree.readNode(YangInstanceIdentifier.EMPTY).get();

        NormalizedNode<?, ?> normalizedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(root.getNodeType())).withChild(
                        ImmutableNodes.containerNode(AUG_CONTAINER)).build();
        pruningDataTreeModification.write(YangInstanceIdentifier.EMPTY, normalizedNode);
        dataTree.commit(getCandidate());

        Optional<NormalizedNode<?, ?>> actual = dataTree.takeSnapshot().readNode(YangInstanceIdentifier.EMPTY);
        assertEquals("Root present", true, actual.isPresent());
        assertEquals("Root node", root, actual.get());

    }

    @Test
    public void testWriteWithInvalidNamespace() throws DataValidationFailedException{
        NormalizedNode<?, ?> normalizedNode = PeopleModel.emptyContainer();
        YangInstanceIdentifier path = PeopleModel.BASE_PATH;

        pruningDataTreeModification.write(path, normalizedNode);

        verify(mockModification, times(1)).write(path, normalizedNode);

        DataTreeCandidateTip candidate = getCandidate();
        assertEquals("getModificationType", ModificationType.UNMODIFIED, candidate.getRootNode().getModificationType());
    }

    @Test
    public void testWriteWithInvalidChildNodeNames() throws DataValidationFailedException{
        ContainerNode augContainer = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER)).withChild(
                        ImmutableNodes.containerNode(AUG_INNER_CONTAINER)).build();

        DataContainerChild<?, ?> outerNode = outerNode(outerNodeEntry(1, innerNode("one", "two")));
        ContainerNode normalizedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(outerNode).withChild(augContainer).
                            withChild(ImmutableNodes.leafNode(AUG_QNAME, "aug")).
                                withChild(ImmutableNodes.leafNode(NAME_QNAME, "name")).build();

        YangInstanceIdentifier path = TestModel.TEST_PATH;

        pruningDataTreeModification.write(path, normalizedNode);

        dataTree.commit(getCandidate());

        ContainerNode prunedNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(outerNode).
                        withChild(ImmutableNodes.leafNode(NAME_QNAME, "name")).build();

        Optional<NormalizedNode<?, ?>> actual = dataTree.takeSnapshot().readNode(path);
        assertEquals("After pruning present", true, actual.isPresent());
        assertEquals("After pruning", prunedNode, actual.get());
    }

    @Test
    public void testReady(){
        pruningDataTreeModification.ready();

        verify(mockModification).ready();
    }

    @Test
    public void testApplyToCursor(){
        DataTreeModificationCursor dataTreeModificationCursor = mock(DataTreeModificationCursor.class);
        pruningDataTreeModification.applyToCursor(dataTreeModificationCursor);

        verify(mockModification).applyToCursor(dataTreeModificationCursor);
    }

    @Test
    public void testReadNode(){
        pruningDataTreeModification.readNode(CarsModel.BASE_PATH);

        verify(mockModification).readNode(CarsModel.BASE_PATH);
    }

    @Test
    public void testNewModification(){
        realModification.ready();
        DataTreeModification dataTreeModification = pruningDataTreeModification.newModification();

        assertTrue("new modification not of type PruningDataTreeModification", dataTreeModification instanceof PruningDataTreeModification);
    }

    private DataTreeCandidateTip getCandidate() throws DataValidationFailedException {
        pruningDataTreeModification.ready();
        DataTreeModification mod = pruningDataTreeModification.getResultingModification();
        mod = mod == proxyModification ? realModification : mod;
        dataTree.validate(mod);
        DataTreeCandidateTip candidate = dataTree.prepare(mod);
        return candidate;
    }
}