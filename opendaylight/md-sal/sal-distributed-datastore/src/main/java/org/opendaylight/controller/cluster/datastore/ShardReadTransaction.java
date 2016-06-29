/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CreateSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

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
        if (message instanceof CreateSnapshot) {
            createSnapshot();
        } else if(ReadData.isSerializedType(message)) {
            readData(transaction, ReadData.fromSerializable(message));
        } else if(DataExists.isSerializedType(message)) {
            dataExists(transaction, DataExists.fromSerializable(message));
        } else {
            super.handleReceive(message);
        }
    }

    private void createSnapshot() {

        // This is a special message sent by the shard to send back a serialized snapshot of the whole
        // data store tree. This transaction was created for that purpose only so we can
        // self-destruct after sending the reply.

        final ActorRef sender = getSender();
        final ActorRef self = getSelf();
        final Optional<NormalizedNode<?, ?>> result = transaction.getSnapshot().readNode(YangInstanceIdentifier.EMPTY);

        byte[] serialized = SerializationUtils.serializeNormalizedNode(result.get());
        sender.tell(new CaptureSnapshotReply(serialized), self);

        self.tell(PoisonPill.getInstance(), self);
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
