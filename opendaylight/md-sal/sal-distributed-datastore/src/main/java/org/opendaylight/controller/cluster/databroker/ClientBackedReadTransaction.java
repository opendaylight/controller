/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An implementation of {@link DOMStoreReadTransaction} backed by a {@link ClientSnapshot}. Used for standalone
 * transactions.
 *
 * @author Robert Varga
 */
final class ClientBackedReadTransaction extends ClientBackedTransaction<ClientSnapshot>
        implements DOMStoreReadTransaction {
    private static final AtomicReferenceFieldUpdater<ClientBackedReadTransaction, ClientBackedTransactionChain>
        PARENT_UPDATER = AtomicReferenceFieldUpdater.newUpdater(ClientBackedReadTransaction.class,
            ClientBackedTransactionChain.class, "parent");

    @SuppressWarnings("unused")
    private volatile ClientBackedTransactionChain parent;

    ClientBackedReadTransaction(final ClientSnapshot delegate, @Nullable final ClientBackedTransactionChain parent,
        @Nullable final Throwable allocationContext) {
        super(delegate, allocationContext);
        this.parent = parent;
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegate().read(path), ReadFailedException.MAPPER);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegate().exists(path), ReadFailedException.MAPPER);
    }

    @Override
    public void close() {
        super.close();

        final ClientBackedTransactionChain local = PARENT_UPDATER.getAndSet(this, null);
        if (local != null) {
            local.snapshotClosed(delegate());
        }
    }
}
