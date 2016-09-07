/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardWriteTransaction;
import org.opendaylight.mdsal.dom.spi.shard.ForeignShardModificationContext;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.store.inmemory.ForeignShardThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy {@link DOMDataTreeShardWriteTransaction} that creates a proxy cursor that translates all calls into
 * {@link ClientTransaction} calls.
 */
class ShardProxyTransaction implements DOMDataTreeShardWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(ShardProxyTransaction.class);

    private final DOMDataTreeIdentifier shardRoot;
    private final Collection<DOMDataTreeIdentifier> prefixes;
    private final DistributedShardModification modification;
    private ClientTransaction currentTx;
    private final List<DOMStoreThreePhaseCommitCohort> cohorts = new ArrayList<>();

    private DOMDataTreeWriteCursor cursor = null;

    ShardProxyTransaction(final DOMDataTreeIdentifier shardRoot,
                          final Collection<DOMDataTreeIdentifier> prefixes,
                          final DistributedShardModification modification) {
        this.shardRoot = Preconditions.checkNotNull(shardRoot);
        this.prefixes = Preconditions.checkNotNull(prefixes);
        this.modification = Preconditions.checkNotNull(modification);
    }

    private DOMDataTreeWriteCursor getCursor() {
        if (cursor == null) {
            cursor = new DistributedShardModificationCursor(modification, this);
        }
        return cursor;
    }

    @Nonnull
    @Override
    public DOMDataTreeWriteCursor createCursor(@Nonnull final DOMDataTreeIdentifier prefix) {
        checkAvailable(prefix);
        final YangInstanceIdentifier relativePath = toRelative(prefix.getRootIdentifier());
        final DOMDataTreeWriteCursor ret = getCursor();
        ret.enter(relativePath.getPathArguments());
        return ret;
    }

    void cursorClosed() {
        cursor = null;
        modification.cursorClosed();
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

    private YangInstanceIdentifier toRelative(final YangInstanceIdentifier path) {
        final Optional<YangInstanceIdentifier> relative =
                path.relativeTo(modification.getPrefix().getRootIdentifier());
        Preconditions.checkArgument(relative.isPresent());
        return relative.get();
    }

    @Override
    public void ready() {
        LOG.debug("Readying transaction for shard {}", shardRoot);

        Preconditions.checkNotNull(modification, "Attempting to ready an empty transaction.");

        cohorts.add(modification.seal());
        for (Entry<DOMDataTreeIdentifier, ForeignShardModificationContext> entry
                : modification.getChildShards().entrySet()) {
            cohorts.add(new ForeignShardThreePhaseCommitCohort(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void close() {
        cohorts.forEach(DOMStoreThreePhaseCommitCohort::abort);
        cohorts.clear();

        if (currentTx != null) {
            currentTx.abort();
            currentTx = null;
        }
    }

    @Override
    public ListenableFuture<Void> submit() {
        LOG.debug("Submitting transaction for shard {}", shardRoot);

        Preconditions.checkState(!cohorts.isEmpty(), "Transaction not readied yet");

        final AsyncFunction<Boolean, Void> validateFunction = input -> prepare();
        final AsyncFunction<Void, Void> prepareFunction = input -> commit();

        // transform validate into prepare
        final ListenableFuture<Void> prepareFuture = Futures.transform(validate(), validateFunction);
        // transform prepare into commit and return as submit result
        return Futures.transform(prepareFuture, prepareFunction);
    }

    @Override
    public ListenableFuture<Boolean> validate() {
        LOG.debug("Validating transaction for shard {}", shardRoot);

        Preconditions.checkState(!cohorts.isEmpty(), "Transaction not readied yet");
        final List<ListenableFuture<Boolean>> futures =
                cohorts.stream().map(DOMStoreThreePhaseCommitCohort::canCommit).collect(Collectors.toList());
        final SettableFuture<Boolean> ret = SettableFuture.create();

        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Boolean>>() {
            @Override
            public void onSuccess(final List<Boolean> result) {
                ret.set(true);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                ret.setException(throwable);
            }
        });

        return ret;
    }

    @Override
    public ListenableFuture<Void> prepare() {
        LOG.debug("Preparing transaction for shard {}", shardRoot);

        Preconditions.checkState(!cohorts.isEmpty(), "Transaction not readied yet");
        final List<ListenableFuture<Void>> futures =
                cohorts.stream().map(DOMStoreThreePhaseCommitCohort::preCommit).collect(Collectors.toList());
        final SettableFuture<Void> ret = SettableFuture.create();

        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(final List<Void> result) {
                ret.set(null);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                ret.setException(throwable);
            }
        });

        return ret;
    }

    @Override
    public ListenableFuture<Void> commit() {
        LOG.debug("Committing transaction for shard {}", shardRoot);

        Preconditions.checkState(!cohorts.isEmpty(), "Transaction not readied yet");
        final List<ListenableFuture<Void>> futures =
                cohorts.stream().map(DOMStoreThreePhaseCommitCohort::commit).collect(Collectors.toList());
        final SettableFuture<Void> ret = SettableFuture.create();

        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(final List<Void> result) {
                ret.set(null);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                ret.setException(throwable);
            }
        });

        return ret;
    }
}
