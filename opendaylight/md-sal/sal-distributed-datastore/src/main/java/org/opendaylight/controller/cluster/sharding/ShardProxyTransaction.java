/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy {@link DOMDataTreeShardWriteTransaction} that creates a proxy cursor that translates all calls into
 * {@link ClientTransaction} calls.
 */
class ShardProxyTransaction implements DOMDataTreeShardWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(ShardProxyTransaction.class);
    private static final ListenableFuture<Void> NULL_FUTURE = Futures.immediateFuture(null);
    private static final ListenableFuture<Boolean> VALIDATE_FUTURE = Futures.immediateFuture(true);

    private final DOMDataTreeIdentifier shardRoot;
    private final Collection<DOMDataTreeIdentifier> prefixes;
    private final DataStoreClient client;
    private final ClientLocalHistory history;
    private ClientTransaction currentTx;
    private DOMStoreThreePhaseCommitCohort cohort;


    ShardProxyTransaction(final DOMDataTreeIdentifier shardRoot, final Collection<DOMDataTreeIdentifier> prefixes,
                          final DataStoreClient client) {
        this.shardRoot = Preconditions.checkNotNull(shardRoot);
        this.prefixes = Preconditions.checkNotNull(prefixes);
        this.client = Preconditions.checkNotNull(client);
        history = client.createLocalHistory();
        currentTx = history.createTransaction();
    }

    @Nonnull
    @Override
    public DOMDataTreeWriteCursor createCursor(@Nonnull final DOMDataTreeIdentifier prefix) {
        checkAvailable(prefix);

        return currentTx.openCursor();
    }

    private void checkAvailable(final DOMDataTreeIdentifier prefix) {
        for (final DOMDataTreeIdentifier p : prefixes) {
            if (p.contains(prefix)) {
                return;
            }
        }
        throw new IllegalArgumentException("Prefix[" + prefix + "] not available for this transaction. "
                + "Available prefixes: " + prefixes);
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
            cohort = null;
        }
        if (currentTx != null) {
            currentTx.abort();
            currentTx = null;
        }
    }

    @Override
    public ListenableFuture<Void> submit() {
        LOG.debug("Submitting transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return NULL_FUTURE;
    }

    @Override
    public ListenableFuture<Boolean> validate() {
        LOG.debug("Validating transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return VALIDATE_FUTURE;
    }

    @Override
    public ListenableFuture<Void> prepare() {
        LOG.debug("Preparing transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return NULL_FUTURE;
    }

    @Override
    public ListenableFuture<Void> commit() {
        LOG.debug("Committing transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(cohort, "Transaction not readied yet");
        return NULL_FUTURE;
    }
}
