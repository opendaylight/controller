/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

/**
 * A {@link TransactionComponentFactory} which produces independent components.
 */
// TODO: this should be integrated into DistributedDataStore
final class SimpleTransactionComponentFactory extends TransactionComponentFactory {
    // We could have opted for a fiend and an Updater, but we do not expect to have
    // many instances and AtomicLong promises to be more resistent to false sharing.
    private static final AtomicLong TX_COUNTER = new AtomicLong();

    protected SimpleTransactionComponentFactory(final ActorContext actorContext) {
        super(actorContext);
    }

    @Override
    protected TransactionIdentifier nextIdentifier() {
        String memberName = getActorContext().getCurrentMemberName();
        if (memberName == null) {
            memberName = "UNKNOWN-MEMBER";
        }

        return new TransactionIdentifier(memberName, TX_COUNTER.getAndIncrement());
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
    protected <T> void onTransactionReady(final TransactionIdentifier transaction, final Collection<Future<T>> cohortFutures) {
        // Transactions are disconnected, this is a no-op
    }
}
