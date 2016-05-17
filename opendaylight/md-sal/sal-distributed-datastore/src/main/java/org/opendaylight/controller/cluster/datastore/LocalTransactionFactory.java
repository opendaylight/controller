/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 * A factory for creating local transactions used by {@link AbstractTransactionContextFactory} to instantiate
 * transactions on shards which are co-located with the shard leader.
 *
 * @author Thomas Pantelis
 */
interface LocalTransactionFactory extends LocalTransactionReadySupport {
    DOMStoreReadTransaction newReadOnlyTransaction(TransactionIdentifier<?> identifier);

    DOMStoreReadWriteTransaction newReadWriteTransaction(TransactionIdentifier<?> identifier);

    DOMStoreWriteTransaction newWriteOnlyTransaction(TransactionIdentifier<?> identifier);
}