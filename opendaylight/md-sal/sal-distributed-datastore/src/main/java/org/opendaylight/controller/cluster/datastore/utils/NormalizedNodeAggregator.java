/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NormalizedNodeAggregator {

    private static final ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    private final YangInstanceIdentifier rootIdentifier;
    private final List<Optional<NormalizedNode<?, ?>>> nodes;
    private final InMemoryDOMDataStore dataStore;

    NormalizedNodeAggregator(YangInstanceIdentifier rootIdentifier, List<Optional<NormalizedNode<?, ?>>> nodes,
                             SchemaContext schemaContext){

        this.rootIdentifier = rootIdentifier;
        this.nodes = nodes;
        this.dataStore = new InMemoryDOMDataStore("aggregator", executorService);
        this.dataStore.onGlobalContextUpdated(schemaContext);
    }

    /**
     * Combine data from all the nodes in the list into a tree with root as rootIdentifier
     *
     * @param nodes
     * @param schemaContext
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static Optional<NormalizedNode<?,?>> aggregate(YangInstanceIdentifier rootIdentifier,
                                                          List<Optional<NormalizedNode<?, ?>>> nodes,
                                                          SchemaContext schemaContext)
            throws ExecutionException, InterruptedException {
        return new NormalizedNodeAggregator(rootIdentifier, nodes, schemaContext).aggregate();
    }

    private Optional<NormalizedNode<?,?>> aggregate() throws ExecutionException, InterruptedException {
        return combine().getRootNode();
    }

    private NormalizedNodeAggregator combine() throws InterruptedException, ExecutionException {
        DOMStoreWriteTransaction domStoreWriteTransaction = dataStore.newWriteOnlyTransaction();

        for(Optional<NormalizedNode<?,?>> node : nodes) {
            if(node.isPresent()) {
                domStoreWriteTransaction.merge(rootIdentifier, node.get());
            }
        }
        DOMStoreThreePhaseCommitCohort ready = domStoreWriteTransaction.ready();
        ready.canCommit().get();
        ready.preCommit().get();
        ready.commit().get();

        return this;
    }

    private Optional<NormalizedNode<?, ?>> getRootNode() throws InterruptedException, ExecutionException {
        DOMStoreReadTransaction readTransaction = dataStore.newReadOnlyTransaction();

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
                readTransaction.read(rootIdentifier);

        return read.get();
    }


}
