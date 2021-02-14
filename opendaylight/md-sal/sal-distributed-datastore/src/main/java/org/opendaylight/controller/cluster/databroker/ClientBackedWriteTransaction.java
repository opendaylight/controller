/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An implementation of {@link DOMStoreWriteTransaction} backed by a {@link ClientTransaction}.
 *
 * @author Robert Varga
 */
class ClientBackedWriteTransaction extends ClientBackedTransaction<ClientTransaction>
        implements DOMStoreWriteTransaction {
    ClientBackedWriteTransaction(final ClientTransaction delegate, final @Nullable Throwable allocationContext) {
        super(delegate, allocationContext);
    }

    @Override
    public final void write(final YangInstanceIdentifier path, final NormalizedNode data) {
        delegate().write(path, data);
    }

    @Override
    public final void merge(final YangInstanceIdentifier path, final NormalizedNode data) {
        delegate().merge(path, data);
    }

    @Override
    public final void delete(final YangInstanceIdentifier path) {
        delegate().delete(path);
    }

    @Override
    public final DOMStoreThreePhaseCommitCohort ready() {
        return delegate().ready();
    }
}
