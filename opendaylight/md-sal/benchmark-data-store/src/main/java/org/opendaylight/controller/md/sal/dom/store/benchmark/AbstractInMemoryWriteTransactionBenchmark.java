/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.benchmark;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractInMemoryWriteTransactionBenchmark {
    protected static final int OUTER_LIST_100K = 100000;
    protected static final int OUTER_LIST_50K = 50000;
    protected static final int OUTER_LIST_10K = 10000;

    protected static final YangInstanceIdentifier[] OUTER_LIST_100K_PATHS = initOuterListPaths(OUTER_LIST_100K);
    protected static final YangInstanceIdentifier[] OUTER_LIST_50K_PATHS = initOuterListPaths(OUTER_LIST_50K);
    protected static final YangInstanceIdentifier[] OUTER_LIST_10K_PATHS = initOuterListPaths(OUTER_LIST_10K);

    private static YangInstanceIdentifier[] initOuterListPaths(final int outerListPathsCount) {
        final YangInstanceIdentifier[] paths = new YangInstanceIdentifier[outerListPathsCount];

        for (int outerListKey = 0; outerListKey < outerListPathsCount; ++outerListKey) {
            paths[outerListKey] = YangInstanceIdentifier.builder(BenchmarkModel.OUTER_LIST_PATH)
                .nodeWithKey(BenchmarkModel.OUTER_LIST_QNAME, BenchmarkModel.ID_QNAME, outerListKey)
                .build();
        }
        return paths;
    }

    protected static final int WARMUP_ITERATIONS = 20;
    protected static final int MEASUREMENT_ITERATIONS = 20;

    protected static final MapNode ONE_ITEM_INNER_LIST = initInnerListItems(1);
    protected static final MapNode TWO_ITEM_INNER_LIST = initInnerListItems(2);
    protected static final MapNode TEN_ITEM_INNER_LIST = initInnerListItems(10);

    private static MapNode initInnerListItems(final int count) {
        final CollectionNodeBuilder<MapEntryNode, MapNode> mapEntryBuilder = ImmutableNodes
            .mapNodeBuilder(BenchmarkModel.INNER_LIST_QNAME);

        for (int i = 1; i <= count; ++i) {
            mapEntryBuilder
                .withChild(ImmutableNodes.mapEntry(BenchmarkModel.INNER_LIST_QNAME, BenchmarkModel.NAME_QNAME, i));
        }
        return mapEntryBuilder.build();
    }

    protected static final NormalizedNode<?, ?>[] OUTER_LIST_ONE_ITEM_INNER_LIST = initOuterListItems(OUTER_LIST_100K, ONE_ITEM_INNER_LIST);
    protected static final NormalizedNode<?, ?>[] OUTER_LIST_TWO_ITEM_INNER_LIST = initOuterListItems(OUTER_LIST_50K, TWO_ITEM_INNER_LIST);
    protected static final NormalizedNode<?, ?>[] OUTER_LIST_TEN_ITEM_INNER_LIST = initOuterListItems(OUTER_LIST_10K, TEN_ITEM_INNER_LIST);

    private static NormalizedNode<?,?>[] initOuterListItems(final int outerListItemsCount, final MapNode innerList) {
        final NormalizedNode<?,?>[] outerListItems = new NormalizedNode[outerListItemsCount];

        for (int i = 0; i < outerListItemsCount; ++i) {
            int outerListKey = i;
            outerListItems[i] = ImmutableNodes.mapEntryBuilder(BenchmarkModel.OUTER_LIST_QNAME, BenchmarkModel.ID_QNAME, outerListKey)
                .withChild(innerList).build();
        }
        return outerListItems;
    }

    protected SchemaContext schemaContext;
    abstract public void setUp() throws Exception;
    abstract public void tearDown();

    protected static DataContainerChild<?, ?> provideOuterListNode() {
        return ImmutableContainerNodeBuilder
            .create()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(BenchmarkModel.TEST_QNAME))
            .withChild(
                ImmutableNodes.mapNodeBuilder(BenchmarkModel.OUTER_LIST_QNAME)
                    .build()).build();
    }
}
