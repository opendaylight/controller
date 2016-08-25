/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.store.inmemory.DOMDataTreeShardWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.ShardCanCommitCoordinationTask;
import org.opendaylight.mdsal.dom.store.inmemory.ShardCommitCoordinationTask;
import org.opendaylight.mdsal.dom.store.inmemory.ShardPreCommitCoordinationTask;
import org.opendaylight.mdsal.dom.store.inmemory.ShardSubmitCoordinationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ShardProxyTransaction implements DOMDataTreeShardWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(ShardProxyTransaction.class);

    private final DOMDataTreeIdentifier shardRoot;
    private final Collection<DOMDataTreeIdentifier> prefixes;
    private final DistributedDataStoreClient client;
    private final ListeningExecutorService executorService;
    private final ClientLocalHistory history;
    private ClientTransaction currentTx;
    private DOMStoreThreePhaseCommitCohort cohort;


    ShardProxyTransaction(final DOMDataTreeIdentifier shardRoot, final Collection<DOMDataTreeIdentifier> prefixes, final DistributedDataStoreClient client, final ListeningExecutorService executorService) {
        this.shardRoot = shardRoot;
        this.prefixes = prefixes;
        this.client = client;
        this.executorService = executorService;
        history = client.createLocalHistory();
        currentTx = history.createTransaction();
    }

    @Nonnull
    @Override
    public DOMDataTreeWriteCursor createCursor(@Nonnull final DOMDataTreeIdentifier prefix) {
        checkAvailable(prefix);
        return new ShardProxyCursor(prefix, currentTx);
    }

    private void checkAvailable(final DOMDataTreeIdentifier prefix) {
        for (final DOMDataTreeIdentifier p : prefixes) {
            if (p.contains(prefix)) {
                return;
            }
        }
        throw new IllegalArgumentException("Prefix[" + prefix + "] not available for this transaction. Available prefixes: " + prefixes);
    }

    @Override
    public void ready() {
        LOG.debug("Readying transaction for shard {}", shardRoot);

        Preconditions.checkState(cohort == null, "Transaction was readied already");
        cohort = currentTx.ready();
        currentTx = null;
    }

    @Override
    public void close() {
        if (cohort != null) {
            cohort.abort();
        }
        if (currentTx != null) {
            currentTx.abort();
        }
    }

    @Override
    public ListenableFuture<Void> submit() {
        LOG.debug("Submitting transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return executorService.submit(new ShardSubmitCoordinationTask(shardRoot, Collections.singletonList(cohort)));
    }

    @Override
    public ListenableFuture<Boolean> validate() {
        LOG.debug("Validating transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return executorService.submit(new ShardCanCommitCoordinationTask(shardRoot, Collections.singletonList(cohort)));
    }

    @Override
    public ListenableFuture<Void> prepare() {
        LOG.debug("Preparing transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return executorService.submit(new ShardPreCommitCoordinationTask(shardRoot, Collections.singletonList(cohort)));
    }

    @Override
    public ListenableFuture<Void> commit() {
        LOG.debug("Committing transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return executorService.submit(new ShardCommitCoordinationTask(shardRoot, Collections.singletonList(cohort)));
    }
}
