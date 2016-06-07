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
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Proxy implementation of {@link DOMStoreReadTransaction}. It routes all requests to the backing
 * {@link ClientTransaction}. This class is not final to allow further subclassing by
 * {@link ShardedDOMStoreReadWriteTransaction}.
 *
 * @author Robert Varga
 */
class ShardedDOMStoreReadTransaction extends AbstractShardedTransaction implements DOMStoreReadTransaction {
    ShardedDOMStoreReadTransaction(final ClientTransaction tx) {
        super(tx);
    }

    @Override
    public final void close() {
        transaction().abort();
    }

    @Override
    public final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        return transaction().read(path);
    }

    @Override
    public final CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return transaction().exists(path);
    }
}
