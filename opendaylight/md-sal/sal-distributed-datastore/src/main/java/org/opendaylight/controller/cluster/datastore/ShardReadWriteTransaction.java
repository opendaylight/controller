/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;

/**
 * Actor for a shard read/write transaction.
 *
 * @author syedbahm
 */
public class ShardReadWriteTransaction extends ShardWriteTransaction {
    public ShardReadWriteTransaction(ReadWriteShardDataTreeTransaction transaction, ActorRef shardActor,
            ShardStats shardStats) {
        super(transaction, shardActor, shardStats);
    }

    @Override
    public void handleReceive(Object message) {
        if (ReadData.isSerializedType(message)) {
            readData(ReadData.fromSerializable(message));
        } else if (DataExists.isSerializedType(message)) {
            dataExists((DataExists) message);
        } else {
            super.handleReceive(message);
        }
    }
}
