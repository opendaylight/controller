/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import akka.actor.ActorSystem;
import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 * Implements a distributed DOMStore using ClientActor.
 */
public class ClientBackedDataStore extends AbstractDataStore {

    public ClientBackedDataStore(final ActorSystem actorSystem, final ClusterWrapper cluster,
            final Configuration configuration, final DatastoreContextFactory datastoreContextFactory,
            final DatastoreSnapshot restoreFromSnapshot) {
        super(actorSystem, cluster, configuration, datastoreContextFactory, restoreFromSnapshot);
    }

    @VisibleForTesting
    ClientBackedDataStore(final ActorContext actorContext, final ClientIdentifier identifier,
                          final DataStoreClient clientActor) {
        super(actorContext, identifier, clientActor);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return new ClientBackedTransactionChain(getClient().createLocalHistory(), debugAllocation());
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new ClientBackedReadTransaction(getClient().createSnapshot(), null, allocationContext());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new ClientBackedWriteTransaction(getClient().createTransaction(), allocationContext());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new ClientBackedReadWriteTransaction(getClient().createTransaction(), allocationContext());
    }

    private boolean debugAllocation() {
        return getActorContext().getDatastoreContext().isTransactionDebugContextEnabled();
    }

    private Throwable allocationContext() {
        return debugAllocation() ? new Throwable("allocated at") : null;
    }
}
