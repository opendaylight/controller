/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An implementation of {@link DOMStoreReadWriteTransaction} backed by a {@link ClientTransaction}.
 *
 * @author Robert Varga
 */
final class ClientBackedReadWriteTransaction extends ClientBackedWriteTransaction
        implements DOMStoreReadWriteTransaction {

    ClientBackedReadWriteTransaction(final ClientTransaction delegate, final @Nullable Throwable allocationContext) {
        super(delegate, allocationContext);
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> read(final YangInstanceIdentifier path) {
        return delegate().read(path);
    }

    @Override
    public FluentFuture<Boolean> exists(final YangInstanceIdentifier path) {
        return delegate().exists(path);
    }
}
