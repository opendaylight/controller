/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.util.concurrent.FluentFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An implementation of {@link DOMStoreReadTransaction} backed by a {@link ClientSnapshot}. Used for standalone
 * transactions.
 */
final class ClientBackedReadTransaction extends ClientBackedTransaction<ClientSnapshot>
        implements DOMStoreReadTransaction {
    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup()
                .findVarHandle(ClientBackedReadTransaction.class, "parent", ClientBackedTransactionChain.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile @Nullable ClientBackedTransactionChain parent;

    @NonNullByDefault
    ClientBackedReadTransaction(final ClientSnapshot delegate, final @Nullable ClientBackedTransactionChain parent,
            final @Nullable Throwable allocationContext) {
        super(delegate, allocationContext);
        this.parent = parent;
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> read(final YangInstanceIdentifier path) {
        return delegate().read(path);
    }

    @Override
    public FluentFuture<Boolean> exists(final YangInstanceIdentifier path) {
        return delegate().exists(path);
    }

    @Override
    public void close() {
        super.close();

        final var local = (ClientBackedTransactionChain) VH.getAndSet(this, null);
        if (local != null) {
            local.snapshotClosed(delegate());
        }
    }
}
