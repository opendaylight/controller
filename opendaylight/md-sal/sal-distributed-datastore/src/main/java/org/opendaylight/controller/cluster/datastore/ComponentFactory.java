/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.dispatch.OnComplete;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import scala.concurrent.Future;

/**
 * Factory for creating components to a transaction.
 * 
 * FIXME: this is thrown together, needs to be cleaned up a bit
 */
abstract class ComponentFactory {
    // FIXME: this needs to be highly scalable/concurrent
    private final Map<String, PrimaryShardInfo> knownShards = new HashMap<>();
    private final ActorContext actorContext;
    
    protected ComponentFactory(final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }
    
    final ActorContext getActorContext() {
        return actorContext;
    }

    final AbstractTransactionComponent newTransactionComponent(ShardedTransaction parent, String shardName) {
        final PrimaryShardInfo shard = knownShards.get(shardName);
        
        if (shard != null) {
            final Optional<DataTree> maybeDataTree = shard.getLocalShardDataTree();
            if (maybeDataTree.isPresent()) {
                return createLocalComponent(parent, shardName, maybeDataTree.get());
            }

            // TODO: we could create the component here, which would be already backed
            //       by the shard info (as opposed to going through a completion).
        }
        
        Future<PrimaryShardInfo> findPrimaryFuture = actorContext.findPrimaryShardAsync(shardName);

        // FIXME: parent's throttler?
        final TransactionFutureCallback callback = new TransactionFutureCallback(null, shardName);
        findPrimaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(Throwable failure, PrimaryShardInfo primaryShardInfo) {
                if(failure != null) {
                    callback.createTransactionContext(failure, null);
                } else {
                    callback.setPrimaryShard(primaryShardInfo.getPrimaryShardActor());
                }
            }
        }, actorContext.getClientDispatcher());

        return null;
    }
    
    protected abstract DOMStoreTransactionFactory factoryForShard(String shardName, DataTree dataTree);

    private AbstractTransactionComponent createLocalComponent(ShardedTransaction parent, String shardName, DataTree dataTree) {
        final DOMStoreTransactionFactory factory = factoryForShard(shardName, dataTree);
        
        switch (parent.getType()) {
        case READ_ONLY:
            return new LocalReadTransactionComponent(factory.newReadOnlyTransaction());
        case READ_WRITE:
            // FIXME: readyImpl?
            return new LocalReadWriteTransactionComponent(factory.newReadWriteTransaction());
        case WRITE_ONLY:
            return new LocalWriteTransactionComponent(factory.newWriteOnlyTransaction());
        default:
          throw new IllegalArgumentException("Unhandled transaction type " + parent.getType());
        }        
    }
}
