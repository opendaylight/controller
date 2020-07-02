/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

import static java.util.Objects.requireNonNull;

public class NoQueueTransactionContextWrapper extends TransactionContextWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(NoQueueTransactionContextWrapper.class);

    private final TransactionIdentifier identifier;
    private final OperationLimiter limiter;
    private final String shardName;
    private TransactionContext transactionContext;

    public NoQueueTransactionContextWrapper(TransactionIdentifier identifier, ActorUtils actorUtils, String shardName) {
        this.identifier = requireNonNull(identifier);
        this.limiter = new OperationLimiter(identifier,
                // 1 extra permit for the ready operation
                actorUtils.getDatastoreContext().getShardBatchedModificationCount() + 1,
                TimeUnit.MILLISECONDS.toSeconds(actorUtils.getDatastoreContext().getOperationTimeoutInMillis()));
        this.shardName = requireNonNull(shardName);
    }

    @Override
    TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @Override
    TransactionIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    OperationLimiter getLimiter() {
        return limiter;
    }

    @Override
    void maybeExecuteTransactionOperation(TransactionOperation op) {
        op.invoke(transactionContext, null);
    }

    @Override
    void executePriorTransactionOperations(TransactionContext localTransactionContext) {
        localTransactionContext.operationHandOffComplete();
        transactionContext = requireNonNull(localTransactionContext);
    }

    @Override
    Future<ActorSelection> readyTransaction(Optional<SortedSet<String>> participatingShardNames) {
        return transactionContext.readyTransaction(null, participatingShardNames);
    }


}
