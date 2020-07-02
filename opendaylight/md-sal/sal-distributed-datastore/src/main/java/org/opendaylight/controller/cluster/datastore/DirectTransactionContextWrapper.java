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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import scala.concurrent.Future;

/**
 *  Direct implementation of TransactionContextWrapper.
 */
public class DirectTransactionContextWrapper extends TransactionContextWrapper {

    private TransactionContext transactionContext;

    public DirectTransactionContextWrapper(TransactionIdentifier identifier, ActorUtils actorUtils, String shardName) {
        super(identifier, actorUtils, shardName);
        transactionContext = null;
    }

    @Override
    TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @Override
    void maybeExecuteTransactionOperation(TransactionOperation op) {
        op.invoke(transactionContext, null);
    }

    void setTransactionContext(TransactionContext localTransactionContext) {
        localTransactionContext.operationHandOffComplete();
        transactionContext = requireNonNull(localTransactionContext);
    }

    @Override
    Future<ActorSelection> readyTransaction(Optional<SortedSet<String>> participatingShardNames) {
        return transactionContext.readyTransaction(null, participatingShardNames);
    }


}
