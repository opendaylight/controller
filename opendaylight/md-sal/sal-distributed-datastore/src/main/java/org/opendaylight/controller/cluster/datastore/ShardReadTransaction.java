/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardReadTransaction extends ShardTransaction {
    private final AbstractShardDataTreeTransaction<?> transaction;

    public ShardReadTransaction(AbstractShardDataTreeTransaction<?> transaction, ActorRef shardActor,
            ShardStats shardStats) {
        super(shardActor, shardStats, transaction.getIdentifier());
        this.transaction = Preconditions.checkNotNull(transaction);
    }

    @Override
    public void handleReceive(Object message) {
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
