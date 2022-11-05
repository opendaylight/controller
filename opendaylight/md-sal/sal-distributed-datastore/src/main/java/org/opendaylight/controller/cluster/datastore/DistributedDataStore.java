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
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;

/**
 * Implements a distributed DOMStore using Akka {@code Patterns.ask()}.
 *
 * @deprecated This implementation is destined for removal,
 */
@Deprecated(since = "7.0.0", forRemoval = true)
public class DistributedDataStore extends AbstractDataStore {
    private final TransactionContextFactory txContextFactory;

    public DistributedDataStore(final ActorSystem actorSystem, final ClusterWrapper cluster,
            final Configuration configuration, final DatastoreContextFactory datastoreContextFactory,
            final DatastoreSnapshot restoreFromSnapshot) {
        super(actorSystem, cluster, configuration, datastoreContextFactory, restoreFromSnapshot);
        txContextFactory = new TransactionContextFactory(getActorUtils(), getIdentifier());
    }

    @VisibleForTesting
    DistributedDataStore(final ActorUtils actorUtils, final ClientIdentifier identifier) {
        super(actorUtils, identifier);
        txContextFactory = new TransactionContextFactory(getActorUtils(), getIdentifier());
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
        getActorUtils().acquireTxCreationPermit();
        return new TransactionProxy(txContextFactory, TransactionType.WRITE_ONLY);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        getActorUtils().acquireTxCreationPermit();
        return new TransactionProxy(txContextFactory, TransactionType.READ_WRITE);
    }

    @Override
    public void close() {
        txContextFactory.close();
        super.close();
    }
}
