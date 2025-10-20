/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHandle;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link DOMStoreTransactionChain} backed by a {@link ClientLocalHistory}.
 *
 * @author Robert Varga
 */
final class ClientBackedTransactionChain implements DOMStoreTransactionChain {
    private static final Logger LOG = LoggerFactory.getLogger(ClientBackedTransactionChain.class);

    private final @GuardedBy("this") Map<AbstractClientHandle<?>, Boolean> openSnapshots = new WeakHashMap<>();

    private final ClientLocalHistory history;
    private final boolean debugAllocation;

    ClientBackedTransactionChain(final ClientLocalHistory history, final boolean debugAllocation) {
        this.history = requireNonNull(history);
        this.debugAllocation = debugAllocation;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new ClientBackedReadTransaction(createSnapshot(), this, allocationContext());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new ClientBackedReadWriteTransaction(createTransaction(), allocationContext());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new ClientBackedWriteTransaction(createTransaction(), allocationContext());
    }

    @Override
    public synchronized void close() {
        final List<TransactionIdentifier> abortedSnapshots = new ArrayList<>();
        for (AbstractClientHandle<?> snap : openSnapshots.keySet()) {
            final TransactionIdentifier id = snap.getIdentifier();
            LOG.debug("Aborting recorded transaction {}", id);
            if (snap.abort()) {
                abortedSnapshots.add(id);
            }
        }
        openSnapshots.clear();

        if (!abortedSnapshots.isEmpty()) {
            LOG.warn("Aborted unclosed transactions {}", abortedSnapshots, new Throwable("at"));
        }
        history.close();
    }

    synchronized void snapshotClosed(final ClientSnapshot clientTransaction) {
        openSnapshots.remove(clientTransaction);
    }

    private ClientSnapshot createSnapshot() {
        return recordSnapshot(history.takeSnapshot());
    }

    private ClientTransaction createTransaction() {
        return recordSnapshot(history.createTransaction());
    }

    private Throwable allocationContext() {
        return debugAllocation ? new Throwable("allocated at") : null;
    }

    private synchronized <T extends AbstractClientHandle<?>> T recordSnapshot(final T snapshot) {
        openSnapshots.put(snapshot, Boolean.TRUE);
        return snapshot;
    }
}
