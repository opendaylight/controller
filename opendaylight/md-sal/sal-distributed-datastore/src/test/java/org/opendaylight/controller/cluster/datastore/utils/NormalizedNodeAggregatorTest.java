/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration.DEFAULT_CONFIGURATION;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.dom.store.inmemory.testlib.TestDOMStoreFactory;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeFactory;
import org.opendaylight.yangtools.yang.data.tree.dagger.ReferenceDataTreeFactoryModule;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

class NormalizedNodeAggregatorTest {
    private static final DataTreeFactory DATA_TREE_FACTORY = ReferenceDataTreeFactoryModule.provideDataTreeFactory();
    private static final TestDOMStoreFactory DOM_STORE_FACTORY = TestDOMStoreFactory.builder(DATA_TREE_FACTORY).build();

    @Test
    void testAggregate() throws Exception {
        final var modelContext = SchemaContextHelper.full();
        final var expectedNode1 = TestModel.EMPTY_TEST;
        final var expectedNode2 = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(CarsModel.CARS_QNAME))
            .build();

        final var normalizedNode = NormalizedNodeAggregator.aggregate(
            YangInstanceIdentifier.of(), DATA_TREE_FACTORY.create(DEFAULT_CONFIGURATION, modelContext),
            List.of(
                Optional.<NormalizedNode>of(getRootNode(expectedNode1, modelContext)),
                Optional.<NormalizedNode>of(getRootNode(expectedNode2, modelContext))))
            .orElseThrow();
        final var collection = assertInstanceOf(ContainerNode.class, normalizedNode).body();

        for (var node : collection) {
            assertInstanceOf(ContainerNode.class, node);
        }

        assertNotNull(findChild(collection, TestModel.TEST_QNAME));
        assertEquals(expectedNode1, findChild(collection, TestModel.TEST_QNAME));
        assertNotNull(findChild(collection, CarsModel.BASE_QNAME));
        assertEquals(expectedNode2, findChild(collection, CarsModel.BASE_QNAME));
    }

    private static NormalizedNode getRootNode(final NormalizedNode moduleNode,
            final EffectiveModelContext modelContext) throws ExecutionException, InterruptedException {
        try (var store = DOM_STORE_FACTORY.newDOMStore("test", DEFAULT_CONFIGURATION, modelContext,
                Executors.newSingleThreadExecutor())) {
            var writeTransaction = store.newWriteOnlyTransaction();

            writeTransaction.merge(YangInstanceIdentifier.of(moduleNode.name().getNodeType()), moduleNode);

            var ready = writeTransaction.ready();

            ready.canCommit().get();
            ready.preCommit().get();
            ready.commit().get();

            var readTransaction = store.newReadOnlyTransaction();

            return readTransaction.read(YangInstanceIdentifier.of()).get().orElseThrow();
        }
    }

    private static DataContainerChild findChild(final Collection<DataContainerChild> collection, final QName qname) {
        for (var node : collection) {
            if (node.name().getNodeType().equals(qname)) {
                return node;
            }
        }
        return null;
    }
}
