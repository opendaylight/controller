/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import scala.concurrent.Future;

/**
 *  Direct implementation of TransactionContextWrapper.
 */
final class DirectTransactionContextWrapper extends TransactionContextWrapper {

    private final TransactionContext transactionContext;

    public DirectTransactionContextWrapper(@NonNull TransactionIdentifier identifier, @NonNull ActorUtils actorUtils,
                                           @NonNull String shardName, @NonNull TransactionContext transactionContext) {
        super(identifier, actorUtils, shardName);
        this.transactionContext = requireNonNull(transactionContext);
    }

    @Override
    TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @Override
    void maybeExecuteTransactionOperation(final TransactionOperation op) {
        op.invoke(transactionContext, null);
    }

    @Override
    Future<ActorSelection> readyTransaction(final Optional<SortedSet<String>> participatingShardNames) {
        return transactionContext.readyTransaction(null, participatingShardNames);
    }

}
