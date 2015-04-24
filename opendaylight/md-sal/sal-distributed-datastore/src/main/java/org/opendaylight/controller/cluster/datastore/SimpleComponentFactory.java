/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.List;
import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

// FIXME: this should be integrated into DistributedDataStore
final class SimpleComponentFactory extends TransactionComponentFactory {
    protected SimpleComponentFactory(final ActorContext actorContext) {
        super(actorContext);
    }

    TransactionIdentifier nextIdentifier() {
        // FIXME: implement this
        return null;
    }

    @Override
    protected DOMStoreTransactionFactory factoryForShard(final String shardName, final ActorSelection shardLeader, final DataTree dataTree) {
        return new LocalTransactionFactory(this, shardLeader, dataTree);
    }

    @Override
    protected Future<PrimaryShardInfo> findPrimaryShard(final String shardName) {
        return getActorContext().findPrimaryShardAsync(shardName);
    }

    @Override
    protected <T> void onTransactionReady(final List<Future<T>> cohortFutures) {
        // Transactions are disconnected, this is a no-op
    }
}
