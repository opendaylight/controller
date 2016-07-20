/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeAggregatorTest {

    @Test
    public void testAggregate() throws InterruptedException, ExecutionException, ReadFailedException, DataValidationFailedException {
        SchemaContext schemaContext = SchemaContextHelper.full();
        NormalizedNode<?, ?> expectedNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        NormalizedNode<?, ?> expectedNode2 = ImmutableNodes.containerNode(CarsModel.CARS_QNAME);

        Optional<NormalizedNode<?, ?>> optional = NormalizedNodeAggregator.aggregate(YangInstanceIdentifier.EMPTY,
                ImmutableList.of(
                        Optional.<NormalizedNode<?, ?>>of(getRootNode(expectedNode1, schemaContext)),
                        Optional.<NormalizedNode<?, ?>>of(getRootNode(expectedNode2, schemaContext))),
                schemaContext, LogicalDatastoreType.CONFIGURATION);


        NormalizedNode<?,?> normalizedNode = optional.get();

        assertTrue("Expect value to be a Collection", normalizedNode.getValue() instanceof Collection);

        Collection<NormalizedNode<?,?>> collection = (Collection<NormalizedNode<?,?>>) normalizedNode.getValue();

        for(NormalizedNode<?,?> node : collection){
            assertTrue("Expected " + node + " to be a ContainerNode", node instanceof ContainerNode);
        }

        assertTrue("Child with QName = " + TestModel.TEST_QNAME + " not found",
                findChildWithQName(collection, TestModel.TEST_QNAME) != null);

        assertEquals(expectedNode1, findChildWithQName(collection, TestModel.TEST_QNAME));

        assertTrue("Child with QName = " + CarsModel.BASE_QNAME + " not found",
                findChildWithQName(collection, CarsModel.BASE_QNAME) != null);

        assertEquals(expectedNode2, findChildWithQName(collection, CarsModel.BASE_QNAME));

    }

    public static NormalizedNode<?, ?> getRootNode(NormalizedNode<?, ?> moduleNode, SchemaContext schemaContext)
            throws ReadFailedException, ExecutionException, InterruptedException {
        try (InMemoryDOMDataStore store = new InMemoryDOMDataStore("test", Executors.newSingleThreadExecutor())) {
            store.onGlobalContextUpdated(schemaContext);

            DOMStoreWriteTransaction writeTransaction = store.newWriteOnlyTransaction();

            writeTransaction.merge(YangInstanceIdentifier.of(moduleNode.getNodeType()), moduleNode);

            DOMStoreThreePhaseCommitCohort ready = writeTransaction.ready();

            ready.canCommit().get();
            ready.preCommit().get();
            ready.commit().get();

            DOMStoreReadTransaction readTransaction = store.newReadOnlyTransaction();

            CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read = readTransaction
                    .read(YangInstanceIdentifier.EMPTY);

            Optional<NormalizedNode<?, ?>> nodeOptional = read.checkedGet();

            return nodeOptional.get();
        }
    }

    public static NormalizedNode<?,?> findChildWithQName(Collection<NormalizedNode<?, ?>> collection, QName qName) {
        for(NormalizedNode<?,?> node : collection){
            if(node.getNodeType().equals(qName)){
                return node;
            }
        }

        return null;
    }

}