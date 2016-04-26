/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

final class TransactionContext {
    private final GlobalTransactionIdentifier gtid;
    private final DataTreeModification tx;
    private final long nextRequest = 0;

    TransactionContext(final GlobalTransactionIdentifier gtid, final DataTreeModification tx) {
        this.gtid = Preconditions.checkNotNull(gtid);
        this.tx = Preconditions.checkNotNull(tx);
    }

    long getTransactionId() {
        return gtid.getTransactionId().getTransactionId();
    }

    long lastRequest() {
        return nextRequest - 1;
    }
}