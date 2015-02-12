/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ShardedDOMDataWriteTransaction implements DOMDataWriteTransaction {
    private static final Function<Exception, TransactionCommitFailedException> COMMIT_MAPPER = new Function<Exception, TransactionCommitFailedException>() {
        @Override
        public TransactionCommitFailedException apply(final Exception input) {
            return new TransactionCommitFailedException("Commit failed", input);
        }
    };
    private static final Function<List<Void>, Void> VOID_FUNCTION = new Function<List<Void>, Void>() {
        @Override
        public Void apply(final List<Void> input) {
            return null;
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(ShardedDOMDataWriteTransaction.class);
    private static final AtomicLong COUNTER = new AtomicLong();
    private final Map<DOMDataTreeIdentifier, DOMStoreWriteTransaction> idToTransaction;
    private final ShardedDOMDataTreeProducer producer;
    private final String identifier;
    @GuardedBy("this")
    private boolean closed =  false;

    ShardedDOMDataWriteTransaction(final ShardedDOMDataTreeProducer producer, final Map<DOMDataTreeIdentifier, DOMStoreWriteTransaction> idToTransaction) {
        this.producer = Preconditions.checkNotNull(producer);
        this.idToTransaction = Preconditions.checkNotNull(idToTransaction);
        this.identifier = "SHARDED-DOM-" + COUNTER.getAndIncrement();
    }

    // FIXME: atomics
    @GuardedBy("this")
    private DOMStoreWriteTransaction lookup(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        final DOMDataTreeIdentifier id = new DOMDataTreeIdentifier(store, path);

        for (Entry<DOMDataTreeIdentifier, DOMStoreWriteTransaction> e : idToTransaction.entrySet()) {
            if (e.getKey().contains(id)) {
                return e.getValue();
            }
        }

        throw new IllegalArgumentException(String.format("Path %s is not acessible from transaction %s", id, this));
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public synchronized boolean cancel() {
        if (closed) {
            return false;
        }

        for (DOMStoreWriteTransaction tx : ImmutableSet.copyOf(idToTransaction.values())) {
            tx.close();
        }

        closed = true;
        producer.cancelTransaction(this);
        return true;
    }

    @Override
    public synchronized CheckedFuture<Void, TransactionCommitFailedException> submit() {
        Preconditions.checkState(!closed, "Transaction %s is already closed", identifier);

        final Set<DOMStoreWriteTransaction> txns = ImmutableSet.copyOf(idToTransaction.values());
        final List<DOMStoreThreePhaseCommitCohort> cohorts = new ArrayList<>(txns.size());
        for (DOMStoreWriteTransaction tx : txns) {
            cohorts.add(tx.ready());
        }

        try {
            return Futures.immediateCheckedFuture(new CommitCoordinationTask(this, cohorts, null).call());
        } catch (TransactionCommitFailedException e) {
            return Futures.immediateFailedCheckedFuture(e);
        }
    }

    @Override
    @Deprecated
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return AbstractDataTransaction.convertToLegacyCommitFuture(submit());
    }

    @Override
    public synchronized void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        lookup(store, path).delete(path);
    }

    @Override
    public synchronized void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        lookup(store, path).write(path, data);
    }

    @Override
    public synchronized void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        lookup(store, path).merge(path, data);
    }
}
