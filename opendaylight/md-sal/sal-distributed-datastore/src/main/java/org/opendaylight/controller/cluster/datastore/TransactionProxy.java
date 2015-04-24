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
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
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

/**
 * A transaction potentially spanning multiple backend shards.
 */
@VisibleForTesting
public class TransactionProxy extends AbstractDOMStoreTransaction<TransactionIdentifier> implements DOMStoreReadWriteTransaction {
    private static enum TransactionState {
        OPEN,
        READY,
        CLOSED,
    }
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxy.class);
    private static final AtomicLong TX_COUNTER = new AtomicLong();

    private final Map<String, AbstractTransactionComponent> components = new HashMap<>();
    private final TransactionComponentFactory componentFactory;
    private final TransactionType type;
    private TransactionState state = TransactionState.OPEN;
    private volatile RemoteTransactionContext remoteContext;

    @VisibleForTesting
    public TransactionProxy(final TransactionComponentFactory componentFactory, final TransactionType type) {
        super(createIdentifier(componentFactory.getActorContext()), false);
        this.componentFactory = Preconditions.checkNotNull(componentFactory);
        this.type = Preconditions.checkNotNull(type);
    }

    private static TransactionIdentifier createIdentifier(final ActorContext actorContext) {
        String memberName = actorContext.getCurrentMemberName();
        if (memberName == null) {
            memberName = "UNKNOWN-MEMBER";
        }

        return new TransactionIdentifier(memberName, TX_COUNTER.getAndIncrement());
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return getComponent(path).exists(path);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        if (YangInstanceIdentifier.EMPTY.equals(path)) {
            return readAllData();
        } else {
            return getComponent(path).read(path);
        }
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readAllData() {
        final Set<String> allShardNames = componentFactory.getActorContext().getConfiguration().getAllShardNames();
        final Collection<CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>> futures = new ArrayList<>(allShardNames.size());

        for (String shardName : allShardNames) {
            futures.add(getComponent(shardName).read(YangInstanceIdentifier.EMPTY));
        }

        final ListenableFuture<List<Optional<NormalizedNode<?, ?>>>> listFuture = Futures.allAsList(futures);
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> aggregateFuture;

        aggregateFuture = Futures.transform(listFuture, new Function<List<Optional<NormalizedNode<?, ?>>>, Optional<NormalizedNode<?, ?>>>() {
            @Override
            public Optional<NormalizedNode<?, ?>> apply(final List<Optional<NormalizedNode<?, ?>>> input) {
                try {
                    return NormalizedNodeAggregator.aggregate(YangInstanceIdentifier.EMPTY, input, componentFactory.getActorContext().getSchemaContext());
                } catch (DataValidationFailedException e) {
                    throw new IllegalArgumentException("Failed to aggregate", e);
                }
            }
        });

        return MappingCheckedFuture.create(aggregateFuture, ReadFailedException.MAPPER);
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        checkModificationState();
        getComponent(path).delete(path);
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkModificationState();
        getComponent(path).merge(path, data);
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkModificationState();
        getComponent(path).write(path, data);
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

        for (AbstractTransactionComponent component : components.values()) {
            component.close();
        }
        components.clear();
    }

    @Override
    public final AbstractThreePhaseCommitCohort<?> ready() {
        Preconditions.checkState(type != TransactionType.READ_ONLY, "Read-only transactions cannot be readied");

        final boolean success = seal(TransactionState.READY);
        Preconditions.checkState(success, "Transaction %s is %s, it cannot be readied", getIdentifier(), state);

        LOG.debug("Tx {} Readying {} components for commit", getIdentifier(), components.size());
        final AbstractThreePhaseCommitCohort<?> ret;
        switch (components.size()) {
        case 0:
            TransactionRateLimitingCallback.adjustRateLimitForUnusedTransaction(componentFactory.getActorContext());
            ret = NoOpDOMStoreThreePhaseCommitCohort.INSTANCE;
            break;
        case 1:
            final Entry<String, AbstractTransactionComponent> e = Iterables.getOnlyElement(components.entrySet());
            ret = createSingleCommitCohort(e.getKey(), e.getValue());
            break;
        default:
            ret = createMultiCommitCohort(components.entrySet());
        }

        componentFactory.onTransactionReady(getIdentifier(), ret.getCohortFutures());
        return ret;
    }

    private AbstractThreePhaseCommitCohort<?> createSingleCommitCohort(final String shardName, final AbstractTransactionComponent component) {
        LOG.debug("Tx {} Readying transaction for shard {}", getIdentifier(), shardName);
        return component.uncoordinatedCommit(componentFactory.getActorContext());
    }

    private AbstractThreePhaseCommitCohort<ActorSelection> createMultiCommitCohort(final Set<Entry<String, AbstractTransactionComponent>> components) {
        final List<Future<ActorSelection>> cohortFutures = new ArrayList<>(components.size());
        for (Entry<String, AbstractTransactionComponent> e : components) {
            LOG.debug("Tx {} Readying transaction for shard {}", getIdentifier(), e.getKey());
            cohortFutures.add(e.getValue().coordinatedCommit());
        }

        return new ThreePhaseCommitCohortProxy(componentFactory.getActorContext(), cohortFutures, getIdentifier().toString());
    }

    private static String shardNameFromIdentifier(final YangInstanceIdentifier path) {
        return ShardStrategyFactory.getStrategy(path).findShard(path);
    }

    private AbstractTransactionComponent getComponent(final YangInstanceIdentifier path) {
        return getComponent(shardNameFromIdentifier(path));
    }

    private AbstractTransactionComponent getComponent(final String shardName) {
        final AbstractTransactionComponent existing = components.get(shardName);
        if (existing != null) {
            return existing;
        }

        final AbstractTransactionComponent fresh = componentFactory.newTransactionComponent(this, shardName);
        components.put(shardName, fresh);
        return fresh;
    }

    TransactionType getType() {
        return type;
    }

    boolean isReady() {
        return state != TransactionState.OPEN;
    }

    RemoteTransactionContext getRemoteContext() {
        RemoteTransactionContext ret = remoteContext;
        if (ret == null) {
            ret = new RemoteTransactionContext(this);
            remoteContext = ret;
        }

        return ret;
    }

    ActorContext getActorContext() {
        return componentFactory.getActorContext();
    }
}
