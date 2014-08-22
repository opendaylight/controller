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

import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardReadWriteTransaction extends ShardTransaction {
    private final DOMStoreReadWriteTransaction transaction;

    public ShardReadWriteTransaction(DOMStoreReadWriteTransaction transaction, ActorRef shardActor,
            SchemaContext schemaContext) {
        super(shardActor, schemaContext);
        this.transaction = transaction;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if(ReadData.SERIALIZABLE_CLASS.equals(message.getClass())) {
            readData(transaction, ReadData.fromSerializable(message));
        } else if(WriteData.SERIALIZABLE_CLASS.equals(message.getClass())) {
            writeData(transaction, WriteData.fromSerializable(message, schemaContext));
        } else if(MergeData.SERIALIZABLE_CLASS.equals(message.getClass())) {
            mergeData(transaction, MergeData.fromSerializable(message, schemaContext));
        } else if(DeleteData.SERIALIZABLE_CLASS.equals(message.getClass())) {
            deleteData(transaction, DeleteData.fromSerializable(message));
        } else if(ReadyTransaction.SERIALIZABLE_CLASS.equals(message.getClass())) {
            readyTransaction(transaction, new ReadyTransaction());
        } else if(DataExists.SERIALIZABLE_CLASS.equals(message.getClass())) {
            dataExists(transaction, DataExists.fromSerializable(message));
        } else {
            super.handleReceive(message);
        }
    }

    @Override
    protected DOMStoreTransaction getDOMStoreTransaction() {
        return transaction;
    }
}
