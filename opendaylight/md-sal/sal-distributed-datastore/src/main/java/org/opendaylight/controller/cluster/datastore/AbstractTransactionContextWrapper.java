/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import scala.concurrent.Future;

/**
 * A helper class that wraps an eventual TransactionContext instance. We have two specializations:
 * <ul>
 *   <li>{@link DelayedTransactionContextWrapper}, which enqueues operations towards the backend</li>
 *   <li>{@link DirectTransactionContextWrapper}, which sends operations to the backend</li>
 * </ul>
 */
abstract class AbstractTransactionContextWrapper {
    private final TransactionIdentifier identifier;
    private final OperationLimiter limiter;
    private final String shardName;

    AbstractTransactionContextWrapper(@NonNull final TransactionIdentifier identifier,
                                      @NonNull final ActorUtils actorUtils, @NonNull final String shardName) {
        this.identifier = requireNonNull(identifier);
        this.shardName = requireNonNull(shardName);
        limiter = new OperationLimiter(identifier,
            // 1 extra permit for the ready operation
            actorUtils.getDatastoreContext().getShardBatchedModificationCount() + 1,
            TimeUnit.MILLISECONDS.toSeconds(actorUtils.getDatastoreContext().getOperationTimeoutInMillis()));
    }

    final TransactionIdentifier getIdentifier() {
        return identifier;
    }

    final OperationLimiter getLimiter() {
        return limiter;
    }

    final String getShardName() {
        return shardName;
    }

    abstract @Nullable TransactionContext getTransactionContext();

    /**
     * Either enqueue or execute specified operation.
     *
     * @param op Operation to (eventually) execute
     */
    abstract void maybeExecuteTransactionOperation(TransactionOperation op);

    /**
     * Mark the transaction as ready.
     *
     * @param participatingShardNames Shards which participate on the transaction
     * @return Future indicating the transaction has been readied on the backend
     */
    abstract @NonNull Future<ActorSelection> readyTransaction(Optional<SortedSet<String>> participatingShardNames);
}
