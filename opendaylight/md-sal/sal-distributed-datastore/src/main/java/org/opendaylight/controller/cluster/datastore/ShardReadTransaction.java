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
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardReadTransaction extends ShardTransaction {
    private final DOMStoreReadTransaction transaction;

    public ShardReadTransaction(DOMStoreReadTransaction transaction, ActorRef shardActor,
            SchemaContext schemaContext, ShardStats shardStats, String transactionID) {
        super(shardActor, schemaContext, shardStats, transactionID);
        this.transaction = transaction;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if(message instanceof ReadData) {
            readData(transaction, (ReadData) message);

        } else if (message instanceof DataExists) {
            dataExists(transaction, (DataExists) message);

        } else {
            super.handleReceive(message);
        }
    }

    @Override
    protected DOMStoreTransaction getDOMStoreTransaction() {
        return transaction;
    }
}
