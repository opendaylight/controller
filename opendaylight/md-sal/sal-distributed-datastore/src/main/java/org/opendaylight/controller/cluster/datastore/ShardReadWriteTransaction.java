/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardReadWriteTransaction extends ShardWriteTransaction {
    public ShardReadWriteTransaction(ReadWriteShardDataTreeTransaction transaction, ActorRef shardActor,
            ShardStats shardStats, String transactionID, short clientTxVersion) {
        super(transaction, shardActor, shardStats, transactionID, clientTxVersion);
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (message instanceof ReadData) {
            readData((ReadData) message, !SERIALIZED_REPLY);

        } else if (message instanceof DataExists) {
            dataExists((DataExists) message, !SERIALIZED_REPLY);

        } else if(ReadData.SERIALIZABLE_CLASS.equals(message.getClass())) {
            readData(ReadData.fromSerializable(message), SERIALIZED_REPLY);

        } else if(DataExists.SERIALIZABLE_CLASS.equals(message.getClass())) {
            dataExists(DataExists.fromSerializable(message), SERIALIZED_REPLY);
        } else {
            super.handleReceive(message);
        }
    }
}
