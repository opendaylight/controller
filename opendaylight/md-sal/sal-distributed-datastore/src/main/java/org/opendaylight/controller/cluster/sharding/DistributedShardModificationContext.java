/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;

/**
 * The context for a single shards modification, keeps a ClientTransaction so it can route requests correctly.
 */
@Deprecated(forRemoval = true)
public class DistributedShardModificationContext {

    private final ClientTransaction transaction;
    private final DOMDataTreeIdentifier identifier;
    private DOMDataTreeWriteCursor cursor;

    public DistributedShardModificationContext(final ClientTransaction transaction,
                                               final DOMDataTreeIdentifier identifier) {
        this.transaction = transaction;
        this.identifier = identifier;
    }

    public DOMDataTreeIdentifier getIdentifier() {
        return identifier;
    }

    DOMDataTreeWriteCursor cursor() {
        if (cursor == null) {
            cursor = transaction.openCursor();
        }

        return cursor;
    }

    DOMStoreThreePhaseCommitCohort ready() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }

        return transaction.ready();
    }

    void closeCursor() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

}
