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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import scala.concurrent.Future;

/**
 * A helper class that wraps an eventual TransactionContext instance.
 */
abstract class AbstractTransactionContextWrapper {

    private final TransactionIdentifier identifier;
    private final OperationLimiter limiter;
    private final String shardName;

    AbstractTransactionContextWrapper(@NonNull final TransactionIdentifier identifier,
                                      @NonNull final ActorUtils actorUtils, @NonNull final String shardName) {
        this.identifier = requireNonNull(identifier);
        this.limiter = new OperationLimiter(identifier,
                // 1 extra permit for the ready operation
                actorUtils.getDatastoreContext().getShardBatchedModificationCount() + 1,
                TimeUnit.MILLISECONDS.toSeconds(actorUtils.getDatastoreContext().getOperationTimeoutInMillis()));
        this.shardName = requireNonNull(shardName);
    }

    abstract TransactionContext getTransactionContext();

    abstract void maybeExecuteTransactionOperation(TransactionOperation op);

    abstract Future<ActorSelection> readyTransaction(Optional<SortedSet<String>> participatingShardNames);

    protected final TransactionIdentifier getIdentifier() {
        return identifier;
    }

    protected final OperationLimiter getLimiter() {
        return limiter;
    }

    protected final String getShardName() {
        return shardName;
    }
}
