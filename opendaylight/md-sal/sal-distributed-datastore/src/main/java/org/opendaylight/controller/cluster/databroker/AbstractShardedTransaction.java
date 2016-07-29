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
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransaction;

/**
 * Abstract base class for concrete {@link DOMStoreTransaction} implementations. It holds a reference to the associated
 * {@link ClientTransaction}. This abstraction layer is needed to isolate end users, who interact with
 * {@link DOMStoreTransaction} from the internal implementation.
 *
 * @author Robert Varga
 */
abstract class AbstractShardedTransaction implements DOMStoreTransaction {
    private final ClientTransaction tx;

    AbstractShardedTransaction(final ClientTransaction tx) {
        this.tx = Preconditions.checkNotNull(tx);
    }

    @Override
    public final Object getIdentifier() {
        return tx.getIdentifier();
    }

    final ClientTransaction transaction() {
        return tx;
    }
}
