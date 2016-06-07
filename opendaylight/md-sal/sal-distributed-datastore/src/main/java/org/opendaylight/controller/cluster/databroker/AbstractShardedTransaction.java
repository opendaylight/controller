/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

abstract class AbstractShardedTransaction implements DOMStoreTransaction {
    private final ClientTransaction tx;

    AbstractShardedTransaction(final ClientTransaction tx) {
        this.tx = Preconditions.checkNotNull(tx);
    }

    @Override
    public final Object getIdentifier() {
        return tx.getIdentifier();
    }

    final Long lookupShard(final YangInstanceIdentifier path) {
        // FIXME: implement this
        return 0L;
    }

    final ClientTransaction transaction() {
        return tx;
    }
}
