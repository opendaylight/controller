/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;

/**
 * Actor for a shard read transaction.
 *
 * @author syedbahm
 */
public class ShardReadTransaction extends ShardTransaction {
    private final AbstractShardDataTreeTransaction<?> transaction;

    public ShardReadTransaction(final AbstractShardDataTreeTransaction<?> transaction, final ActorRef shardActor,
            final ShardStats shardStats) {
        super(shardActor, shardStats, transaction.getIdentifier());
        this.transaction = requireNonNull(transaction);
    }

    @Override
    public void handleReceive(final Object message) {
        if (ReadData.isSerializedType(message)) {
            readData(transaction, ReadData.fromSerializable(message));
        } else if (DataExists.isSerializedType(message)) {
            dataExists(transaction, DataExists.fromSerializable(message));
        } else {
            super.handleReceive(message);
        }
    }

    @Override
    protected AbstractShardDataTreeTransaction<?> getDOMStoreTransaction() {
        return transaction;
    }

    @Override
    protected boolean returnCloseTransactionReply() {
        return false;
    }
}
