/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PersistAbortTransactionPayload;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.SnapshotBackedReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class ReadTransactionWrapper implements DOMStoreReadTransaction {
    private final SnapshotBackedReadTransaction<TransactionIdentifier> tx;
    private final ActorSelection leader;

    ReadTransactionWrapper(final SnapshotBackedReadTransaction<TransactionIdentifier> tx,
            final ActorSelection leader) {
        this.tx = requireNonNull(tx);
        this.leader = requireNonNull(leader);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return tx.getIdentifier();
    }

    @Override
    public void close() {
        final boolean hadSnapshot = tx.getSnapshot().isPresent();
        tx.close();
        if (hadSnapshot) {
            leader.tell(new PersistAbortTransactionPayload(getIdentifier()), ActorRef.noSender());
        }
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(final YangInstanceIdentifier path) {
        return tx.read(path);
    }

    @Override
    public FluentFuture<Boolean> exists(final YangInstanceIdentifier path) {
        return tx.exists(path);
    }
}