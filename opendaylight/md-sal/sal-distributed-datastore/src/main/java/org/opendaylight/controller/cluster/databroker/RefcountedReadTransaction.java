/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Shared reference to a {@link ClientTransaction} reused by multiple {@link ClientBackedChainedReadTransaction}s.
 *
 * @author Robert Varga
 */
final class RefcountedReadTransaction {
    private final ClientBackedTransactionChain parent;

    private ClientTransaction delegate;
    private int refCount;
    private int txCount;

    RefcountedReadTransaction(final ClientBackedTransactionChain parent, final ClientTransaction delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
        this.parent = Preconditions.checkNotNull(parent);
    }

    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegate.read(path), ReadFailedException.MAPPER);
    }

    CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegate.exists(path), ReadFailedException.MAPPER);
    }

    void abort() {
        delegate.abort();
        delegate = null;
    }

    void release() {
        refCount--;
        if (refCount == 0 && delegate != null) {
            delegate.abort();
            delegate = null;
            parent.finishReadTx(this);
        }
    }

    DOMStoreReadTransaction newTransaction() {
        refCount++;
        return new ClientBackedChainedReadTransaction(this, delegate.getIdentifier(), txCount++);
    }

}
