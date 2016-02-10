/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeAggregator;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.AbstractDOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A transaction potentially spanning multiple backend shards.
 */
public class TransactionProxy extends AbstractDOMStoreTransaction<TransactionIdentifier> implements DOMStoreReadWriteTransaction {
    private static enum TransactionState {
        OPEN,
        READY,
        CLOSED,
    }
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxy.class);

    private final Map<String, TransactionContextWrapper> txContextWrappers = new HashMap<>();
    private final AbstractTransactionContextFactory<?> txContextFactory;
    private final TransactionType type;
    private TransactionState state = TransactionState.OPEN;

    @VisibleForTesting
    public TransactionProxy(final AbstractTransactionContextFactory<?> txContextFactory, final TransactionType type) {
        super(txContextFactory.nextIdentifier(), txContextFactory.getActorContext().getDatastoreContext()
                .isTransactionDebugContextEnabled());
        this.txContextFactory = txContextFactory;
        this.type = Preconditions.checkNotNull(type);

        LOG.debug("New {} Tx - {}", type, getIdentifier());
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return executeRead(shardNameFromIdentifier(path), new DataExists(path, DataStoreVersions.CURRENT_VERSION));
    }

    private <T> CheckedFuture<T, ReadFailedException> executeRead(String shardName, final AbstractRead<T> readCmd) {
        Preconditions.checkState(type != TransactionType.WRITE_ONLY, "Reads from write-only transactions are not allowed");

        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} {} {}", getIdentifier(), readCmd.getClass().getSimpleName(), readCmd.getPath());
        }

        final SettableFuture<T> proxyFuture = SettableFuture.create();
        TransactionContextWrapper contextWrapper = getContextWrapper(shardName);
        contextWrapper.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                transactionContext.executeRead(readCmd, proxyFuture);
            }
        });

        return MappingCheckedFuture.create(proxyFuture, ReadFailedException.MAPPER);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        Preconditions.checkState(type != TransactionType.WRITE_ONLY, "Reads from write-only transactions are not allowed");

        LOG.debug("Tx {} read {}", getIdentifier(), path);

        if (YangInstanceIdentifier.EMPTY.equals(path)) {
            return readAllData();
        } else {
            return singleShardRead(shardNameFromIdentifier(path), path);
        }
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> singleShardRead(
            final String shardName, final YangInstanceIdentifier path) {
        return executeRead(shardName, new ReadData(path, DataStoreVersions.CURRENT_VERSION));
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readAllData() {
        final Set<String> allShardNames = txContextFactory.getActorContext().getConfiguration().getAllShardNames();
        final Collection<CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>> futures = new ArrayList<>(allShardNames.size());

        for (String shardName : allShardNames) {
            futures.add(singleShardRead(shardName, YangInstanceIdentifier.EMPTY));
        }

        final ListenableFuture<List<Optional<NormalizedNode<?, ?>>>> listFuture = Futures.allAsList(futures);
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> aggregateFuture;

        aggregateFuture = Futures.transform(listFuture, new Function<List<Optional<NormalizedNode<?, ?>>>, Optional<NormalizedNode<?, ?>>>() {
            @Override
            public Optional<NormalizedNode<?, ?>> apply(final List<Optional<NormalizedNode<?, ?>>> input) {
                try {
                    return NormalizedNodeAggregator.aggregate(YangInstanceIdentifier.EMPTY, input, txContextFactory.getActorContext().getSchemaContext());
                } catch (DataValidationFailedException e) {
                    throw new IllegalArgumentException("Failed to aggregate", e);
                }
            }
        });

        return MappingCheckedFuture.create(aggregateFuture, ReadFailedException.MAPPER);
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        executeModification(new DeleteModification(path));
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        executeModification(new MergeModification(path, data));
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        executeModification(new WriteModification(path, data));
    }

    private void executeModification(final AbstractModification modification) {
        checkModificationState();

        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} executeModification {} {}", getIdentifier(), modification.getClass().getSimpleName(),
                    modification.getPath());
        }

        TransactionContextWrapper contextWrapper = getContextWrapper(modification.getPath());
        contextWrapper.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            protected void invoke(TransactionContext transactionContext) {
                transactionContext.executeModification(modification);
            }
        });
    }

    private void checkModificationState() {
        Preconditions.checkState(type != TransactionType.READ_ONLY,
                "Modification operation on read-only transaction is not allowed");
        Preconditions.checkState(state == TransactionState.OPEN,
                "Transaction is sealed - further modifications are not allowed");
    }

    private boolean seal(final TransactionState newState) {
        if (state == TransactionState.OPEN) {
            state = newState;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final void close() {
        if (!seal(TransactionState.CLOSED)) {
            Preconditions.checkState(state == TransactionState.CLOSED, "Transaction %s is ready, it cannot be closed",
                getIdentifier());
            // Idempotent no-op as per AutoCloseable recommendation
            return;
        }

        for (TransactionContextWrapper contextWrapper : txContextWrappers.values()) {
            contextWrapper.maybeExecuteTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.closeTransaction();
                }
            });
        }


        txContextWrappers.clear();
    }

    @Override
    public final AbstractThreePhaseCommitCohort<?> ready() {
        Preconditions.checkState(type != TransactionType.READ_ONLY, "Read-only transactions cannot be readied");

        final boolean success = seal(TransactionState.READY);
        Preconditions.checkState(success, "Transaction %s is %s, it cannot be readied", getIdentifier(), state);

        LOG.debug("Tx {} Readying {} components for commit", getIdentifier(), txContextWrappers.size());

        final AbstractThreePhaseCommitCohort<?> ret;
        switch (txContextWrappers.size()) {
        case 0:
            ret = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
            break;
        case 1:
            final Entry<String, TransactionContextWrapper> e = Iterables.getOnlyElement(txContextWrappers.entrySet());
            ret = createSingleCommitCohort(e.getKey(), e.getValue());
            break;
        default:
            ret = createMultiCommitCohort(txContextWrappers.entrySet());
        }

        txContextFactory.onTransactionReady(getIdentifier(), ret.getCohortFutures());

        final Throwable debugContext = getDebugContext();
        return debugContext == null ? ret : new DebugThreePhaseCommitCohort(getIdentifier(), ret, debugContext);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private AbstractThreePhaseCommitCohort<?> createSingleCommitCohort(final String shardName,
            final TransactionContextWrapper contextWrapper) {

        LOG.debug("Tx {} Readying transaction for shard {}", getIdentifier(), shardName);

        final OperationCallback.Reference operationCallbackRef =
                new OperationCallback.Reference(OperationCallback.NO_OP_CALLBACK);

        final TransactionContext transactionContext = contextWrapper.getTransactionContext();
        final Future future;
        if (transactionContext == null) {
            final Promise promise = akka.dispatch.Futures.promise();
            contextWrapper.maybeExecuteTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    promise.completeWith(getDirectCommitFuture(transactionContext, operationCallbackRef));
                }
            });
            future = promise.future();
        } else {
            // avoid the creation of a promise and a TransactionOperation
            future = getDirectCommitFuture(transactionContext, operationCallbackRef);
        }

        return new SingleCommitCohortProxy(txContextFactory.getActorContext(), future, getIdentifier().toString(),
                operationCallbackRef);
    }

    private Future<?> getDirectCommitFuture(TransactionContext transactionContext,
            OperationCallback.Reference operationCallbackRef) {
        TransactionRateLimitingCallback rateLimitingCallback = new TransactionRateLimitingCallback(
                txContextFactory.getActorContext());
        operationCallbackRef.set(rateLimitingCallback);
        rateLimitingCallback.run();
        return transactionContext.directCommit();
    }

    private AbstractThreePhaseCommitCohort<ActorSelection> createMultiCommitCohort(
            final Set<Entry<String, TransactionContextWrapper>> txContextWrapperEntries) {

        final List<ThreePhaseCommitCohortProxy.CohortInfo> cohorts = new ArrayList<>(txContextWrapperEntries.size());
        for (Entry<String, TransactionContextWrapper> e : txContextWrapperEntries) {
            LOG.debug("Tx {} Readying transaction for shard {}", getIdentifier(), e.getKey());

            final TransactionContextWrapper wrapper = e.getValue();

            // The remote tx version is obtained the via TransactionContext which may not be available yet so
            // we pass a Supplier to dynamically obtain it. Once the ready Future is resolved the
            // TransactionContext is available.
            Supplier<Short> txVersionSupplier = new Supplier<Short>() {
                @Override
                public Short get() {
                    return wrapper.getTransactionContext().getTransactionVersion();
                }
            };

            cohorts.add(new ThreePhaseCommitCohortProxy.CohortInfo(wrapper.readyTransaction(), txVersionSupplier));
        }

        return new ThreePhaseCommitCohortProxy(txContextFactory.getActorContext(), cohorts,
                getIdentifier().toString());
    }

    private String shardNameFromIdentifier(final YangInstanceIdentifier path) {
        return txContextFactory.getActorContext().getShardStrategyFactory().getStrategy(path).findShard(path);
    }

    private TransactionContextWrapper getContextWrapper(final YangInstanceIdentifier path) {
        return getContextWrapper(shardNameFromIdentifier(path));
    }

    private TransactionContextWrapper getContextWrapper(final String shardName) {
        final TransactionContextWrapper existing = txContextWrappers.get(shardName);
        if (existing != null) {
            return existing;
        }

        final TransactionContextWrapper fresh = txContextFactory.newTransactionContextWrapper(this, shardName);
        txContextWrappers.put(shardName, fresh);
        return fresh;
    }

    TransactionType getType() {
        return type;
    }

    boolean isReady() {
        return state != TransactionState.OPEN;
    }

    ActorContext getActorContext() {
        return txContextFactory.getActorContext();
    }
}
