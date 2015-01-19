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
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardReadWriteTransaction extends ShardWriteTransaction {
    private final DOMStoreReadWriteTransaction transaction;

    public ShardReadWriteTransaction(DOMStoreReadWriteTransaction transaction, ActorRef shardActor,
            SchemaContext schemaContext, ShardStats shardStats, String transactionID,
            short clientTxVersion) {
        super(transaction, shardActor, schemaContext, shardStats, transactionID, clientTxVersion);
        this.transaction = transaction;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (message instanceof ReadData) {
            readData(transaction, (ReadData) message, !SERIALIZED_REPLY);

        } else if (message instanceof DataExists) {
            dataExists(transaction, (DataExists) message, !SERIALIZED_REPLY);

        } else if(ReadData.SERIALIZABLE_CLASS.equals(message.getClass())) {
            readData(transaction, ReadData.fromSerializable(message), SERIALIZED_REPLY);

        } else if(DataExists.SERIALIZABLE_CLASS.equals(message.getClass())) {
            dataExists(transaction, DataExists.fromSerializable(message), SERIALIZED_REPLY);

        } else {
            super.handleReceive(message);
        }
    }
}
