/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PhantomReference that closes remote transactions for a TransactionProxy when it's
 * garbage collected. This is used for read-only transactions as they're not explicitly closed
 * by clients. So the only way to detect that a transaction is no longer in use and it's safe
 * to clean up is when it's garbage collected. It's inexact as to when an instance will be GC'ed
 * but TransactionProxy instances should generally be short-lived enough to avoid being moved
 * to the old generation space and thus should be cleaned up in a timely manner as the GC
 * runs on the young generation (eden, swap1...) space much more frequently.
 */
final class TransactionProxyCleanupPhantomReference
                                       extends FinalizablePhantomReference<TransactionProxy> {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProxyCleanupPhantomReference.class);
    /**
     * Used to enqueue the PhantomReferences for read-only TransactionProxy instances. The
     * FinalizableReferenceQueue is safe to use statically in an OSGi environment as it uses some
     * trickery to clean up its internal thread when the bundle is unloaded.
     */
    private static final FinalizableReferenceQueue phantomReferenceQueue =
                                                                  new FinalizableReferenceQueue();

    /**
     * This stores the TransactionProxyCleanupPhantomReference instances statically, This is
     * necessary because PhantomReferences need a hard reference so they're not garbage collected.
     * Once finalized, the TransactionProxyCleanupPhantomReference removes itself from this map
     * and thus becomes eligible for garbage collection.
     */
    private static final Map<TransactionProxyCleanupPhantomReference,
                             TransactionProxyCleanupPhantomReference> phantomReferenceCache =
                                                                        new ConcurrentHashMap<>();

    private final List<ActorSelection> remoteTransactionActors;
    private final AtomicBoolean remoteTransactionActorsMB;
    private final ActorContext actorContext;
    private final TransactionIdentifier identifier;

    private TransactionProxyCleanupPhantomReference(TransactionProxy referent) {
        super(referent, phantomReferenceQueue);

        // Note we need to cache the relevant fields from the TransactionProxy as we can't
        // have a hard reference to the TransactionProxy instance itself.

        remoteTransactionActors = referent.remoteTransactionActors;
        remoteTransactionActorsMB = referent.remoteTransactionActorsMB;
        actorContext = referent.actorContext;
        identifier = referent.getIdentifier();
    }

    static void track(TransactionProxy referent) {
        final TransactionProxyCleanupPhantomReference ret = new TransactionProxyCleanupPhantomReference(referent);
        phantomReferenceCache.put(ret, ret);
    }

    @Override
    public void finalizeReferent() {
        LOG.trace("Cleaning up {} Tx actors for TransactionProxy {}",
                remoteTransactionActors.size(), identifier);

        phantomReferenceCache.remove(this);

        // Access the memory barrier volatile to ensure all previous updates to the
        // remoteTransactionActors list are visible to this thread.

        if(remoteTransactionActorsMB.get()) {
            for(ActorSelection actor : remoteTransactionActors) {
                LOG.trace("Sending CloseTransaction to {}", actor);
                actorContext.sendOperationAsync(actor, CloseTransaction.INSTANCE.toSerializable());
            }
        }
    }
}