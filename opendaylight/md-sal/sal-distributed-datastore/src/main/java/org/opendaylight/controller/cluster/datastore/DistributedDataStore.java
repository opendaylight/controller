/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 * Implements a distributed DOMStore using Akka Patterns.ask().
 */
public class DistributedDataStore extends AbstractDataStore {

    private final TransactionContextFactory txContextFactory;

    public DistributedDataStore(final ActorSystem actorSystem, final ClusterWrapper cluster,
            final Configuration configuration, final DatastoreContextFactory datastoreContextFactory,
            final DatastoreSnapshot restoreFromSnapshot) {
        super(actorSystem, cluster, configuration, datastoreContextFactory, restoreFromSnapshot);
        this.txContextFactory = new TransactionContextFactory(getActorContext(), getIdentifier());
    }

    @VisibleForTesting
    DistributedDataStore(final ActorContext actorContext, final ClientIdentifier identifier) {
        super(actorContext, identifier);
        this.txContextFactory = new TransactionContextFactory(getActorContext(), getIdentifier());
    }


    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return txContextFactory.createTransactionChain();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new TransactionProxy(txContextFactory, TransactionType.READ_ONLY);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        getActorContext().acquireTxCreationPermit();
        return new TransactionProxy(txContextFactory, TransactionType.WRITE_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        getActorContext().acquireTxCreationPermit();
        return new TransactionProxy(txContextFactory, TransactionType.READ_WRITE);
    }

    @Override
    public void close() {
        txContextFactory.close();
        super.close();
    }
}
