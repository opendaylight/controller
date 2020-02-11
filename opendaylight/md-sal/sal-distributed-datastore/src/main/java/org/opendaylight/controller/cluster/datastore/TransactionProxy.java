/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeAggregator;
import org.opendaylight.mdsal.dom.spi.store.AbstractDOMStoreTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
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
public class TransactionProxy extends AbstractDOMStoreTransaction<TransactionIdentifier>
        implements DOMStoreReadWriteTransaction {
    private enum TransactionState {
        OPEN,
        READY,
        CLOSED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxy.class);

    private final Map<String, TransactionContextWrapper> txContextWrappers = new TreeMap<>();
    private final AbstractTransactionContextFactory<?> txContextFactory;
    private final TransactionType type;
    private TransactionState state = TransactionState.OPEN;

    @VisibleForTesting
    public TransactionProxy(final AbstractTransactionContextFactory<?> txContextFactory, final TransactionType type) {
        super(txContextFactory.nextIdentifier(), txContextFactory.getActorUtils().getDatastoreContext()
                .isTransactionDebugContextEnabled());
        this.txContextFactory = txContextFactory;
        this.type = requireNonNull(type);

        LOG.debug("New {} Tx - {}", type, getIdentifier());
    }

    @Override
    public FluentFuture<Boolean> exists(final YangInstanceIdentifier path) {
        return executeRead(shardNameFromIdentifier(path), new DataExists(path, DataStoreVersions.CURRENT_VERSION));
    }

    private <T> FluentFuture<T> executeRead(final String shardName, final AbstractRead<T> readCmd) {
        checkState(type != TransactionType.WRITE_ONLY, "Reads from write-only transactions are not allowed");

        LOG.trace("Tx {} {} {}", getIdentifier(), readCmd.getClass().getSimpleName(), readCmd.getPath());

        final SettableFuture<T> proxyFuture = SettableFuture.create();
        TransactionContextWrapper contextWrapper = getContextWrapper(shardName);
        contextWrapper.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(final TransactionContext transactionContext, final Boolean havePermit) {
                transactionContext.executeRead(readCmd, proxyFuture, havePermit);
            }
        });

        return FluentFuture.from(proxyFuture);
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(final YangInstanceIdentifier path) {
        checkState(type != TransactionType.WRITE_ONLY, "Reads from write-only transactions are not allowed");
        requireNonNull(path, "path should not be null");

        LOG.trace("Tx {} read {}", getIdentifier(), path);
        return path.isEmpty() ? readAllData() :  singleShardRead(shardNameFromIdentifier(path), path);
    }

    private FluentFuture<Optional<NormalizedNode<?, ?>>> singleShardRead(
            final String shardName, final YangInstanceIdentifier path) {
        return executeRead(shardName, new ReadData(path, DataStoreVersions.CURRENT_VERSION));
    }

    private FluentFuture<Optional<NormalizedNode<?, ?>>> readAllData() {
        final Set<String> allShardNames = txContextFactory.getActorUtils().getConfiguration().getAllShardNames();
        final Collection<FluentFuture<Optional<NormalizedNode<?, ?>>>> futures = new ArrayList<>(allShardNames.size());

        for (String shardName : allShardNames) {
            futures.add(singleShardRead(shardName, YangInstanceIdentifier.empty()));
        }

        final ListenableFuture<List<Optional<NormalizedNode<?, ?>>>> listFuture = Futures.allAsList(futures);
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> aggregateFuture;

        aggregateFuture = Futures.transform(listFuture, input -> {
            try {
                return NormalizedNodeAggregator.aggregate(YangInstanceIdentifier.empty(), input,
                        txContextFactory.getActorUtils().getSchemaContext(),
                        txContextFactory.getActorUtils().getDatastoreContext().getLogicalStoreType());
            } catch (DataValidationFailedException e) {
                throw new IllegalArgumentException("Failed to aggregate", e);
            }
        }, MoreExecutors.directExecutor());

        return FluentFuture.from(aggregateFuture);
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
        if (modification instanceof WriteModification
                && ((WriteModification) modification).getData().toString()
                .contains("flow-table-statistics]}, value=[]")) {
            LOG.info("Tx {} executeModification {} {} for data {} and the call stack {}", getIdentifier(),
                    modification.getClass().getSimpleName(), modification.getPath(),
                    ((WriteModification) modification).getData(),
                    Arrays.toString(Thread.currentThread().getStackTrace()));
        }
        checkModificationState();

        LOG.trace("Tx {} executeModification {} {}", getIdentifier(), modification.getClass().getSimpleName(),
                modification.getPath());

        TransactionContextWrapper contextWrapper = getContextWrapper(modification.getPath());
        contextWrapper.maybeExecuteTransactionOperation(new TransactionOperation() {
            @Override
            protected void invoke(final TransactionContext transactionContext, final Boolean havePermit) {
                transactionContext.executeModification(modification, havePermit);
            }
        });
    }

    private void checkModificationState() {
        checkState(type != TransactionType.READ_ONLY, "Modification operation on read-only transaction is not allowed");
        checkState(state == TransactionState.OPEN, "Transaction is sealed - further modifications are not allowed");
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
            checkState(state == TransactionState.CLOSED, "Transaction %s is ready, it cannot be closed",
                getIdentifier());
            // Idempotent no-op as per AutoCloseable recommendation
            return;
        }

        for (TransactionContextWrapper contextWrapper : txContextWrappers.values()) {
            contextWrapper.maybeExecuteTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(final TransactionContext transactionContext, final Boolean havePermit) {
                    transactionContext.closeTransaction();
                }
            });
        }


        txContextWrappers.clear();
    }

    @Override
    public final AbstractThreePhaseCommitCohort<?> ready() {
        checkState(type != TransactionType.READ_ONLY, "Read-only transactions cannot be readied");

        final boolean success = seal(TransactionState.READY);
        checkState(success, "Transaction %s is %s, it cannot be readied", getIdentifier(), state);

        LOG.debug("Tx {} Readying {} components for commit", getIdentifier(), txContextWrappers.size());

        final AbstractThreePhaseCommitCohort<?> ret;
        switch (txContextWrappers.size()) {
            case 0:
                ret = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
                break;
            case 1:
                final Entry<String, TransactionContextWrapper> e = Iterables.getOnlyElement(
                        txContextWrappers.entrySet());
                ret = createSingleCommitCohort(e.getKey(), e.getValue());
                break;
            default:
                ret = createMultiCommitCohort();
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
                public void invoke(final TransactionContext newTransactionContext, final Boolean havePermit) {
                    promise.completeWith(getDirectCommitFuture(newTransactionContext, operationCallbackRef,
                        havePermit));
                }
            });
            future = promise.future();
        } else {
            // avoid the creation of a promise and a TransactionOperation
            future = getDirectCommitFuture(transactionContext, operationCallbackRef, null);
        }

        return new SingleCommitCohortProxy(txContextFactory.getActorUtils(), future, getIdentifier(),
            operationCallbackRef);
    }

    private Future<?> getDirectCommitFuture(final TransactionContext transactionContext,
            final OperationCallback.Reference operationCallbackRef, final Boolean havePermit) {
        TransactionRateLimitingCallback rateLimitingCallback = new TransactionRateLimitingCallback(
                txContextFactory.getActorUtils());
        operationCallbackRef.set(rateLimitingCallback);
        rateLimitingCallback.run();
        return transactionContext.directCommit(havePermit);
    }

    private AbstractThreePhaseCommitCohort<ActorSelection> createMultiCommitCohort() {

        final List<ThreePhaseCommitCohortProxy.CohortInfo> cohorts = new ArrayList<>(txContextWrappers.size());
        final Optional<SortedSet<String>> shardNames = Optional.of(new TreeSet<>(txContextWrappers.keySet()));
        for (Entry<String, TransactionContextWrapper> e : txContextWrappers.entrySet()) {
            LOG.debug("Tx {} Readying transaction for shard {}", getIdentifier(), e.getKey());

            final TransactionContextWrapper wrapper = e.getValue();

            // The remote tx version is obtained the via TransactionContext which may not be available yet so
            // we pass a Supplier to dynamically obtain it. Once the ready Future is resolved the
            // TransactionContext is available.
            cohorts.add(new ThreePhaseCommitCohortProxy.CohortInfo(wrapper.readyTransaction(shardNames),
                () -> wrapper.getTransactionContext().getTransactionVersion()));
        }

        return new ThreePhaseCommitCohortProxy(txContextFactory.getActorUtils(), cohorts, getIdentifier());
    }

    private String shardNameFromIdentifier(final YangInstanceIdentifier path) {
        return txContextFactory.getActorUtils().getShardStrategyFactory().getStrategy(path).findShard(path);
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

    ActorUtils getActorUtils() {
        return txContextFactory.getActorUtils();
    }
}
