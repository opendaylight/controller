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
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An implementation of {@link DOMStoreReadWriteTransaction} backed by a {@link ClientTransaction}.
 *
 * @author Robert Varga
 */
final class ClientBackedReadWriteTransaction extends ClientBackedWriteTransaction
        implements DOMStoreReadWriteTransaction {

    ClientBackedReadWriteTransaction(final ClientTransaction delegate, @Nullable final Throwable allocationContext) {
        super(delegate, allocationContext);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegate().read(path), ReadFailedException.MAPPER);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return Futures.makeChecked(delegate().exists(path), ReadFailedException.MAPPER);
    }
}
