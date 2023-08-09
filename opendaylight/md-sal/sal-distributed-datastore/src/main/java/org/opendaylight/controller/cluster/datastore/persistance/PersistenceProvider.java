/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persistance;

import akka.actor.ActorRef;
import akka.persistence.DeleteSnapshotsSuccess;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.cluster.DefaultSnapshotPersistenceProvider;
import org.opendaylight.controller.cluster.SnapshotPersistenceProvider;
import org.opendaylight.controller.cluster.datastore.DatastoreContextIntrospector;
import org.opendaylight.controller.cluster.datastore.DatastoreContextIntrospectorFactory;
import org.opendaylight.controller.cluster.persistence.NewLocalSnapshotStore;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves as replacement for Akka persistence.
 * Provides storing and loading of Snapshots fo Shards.
 */
@Singleton
@Component(immediate = true, service = SnapshotPersistenceProvider.class)
public final class PersistenceProvider implements SnapshotPersistenceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceProvider.class);
    public String name = null;
    private final NewLocalSnapshotStore store;
    private final SnapshotPersistenceProvider delegate;

    @Inject
    @Activate
    public PersistenceProvider(@Reference DatastoreContextIntrospectorFactory introspectorFactory) {
        final DatastoreContextIntrospector introspector = introspectorFactory
                .newInstance(LogicalDatastoreType.OPERATIONAL, null);
        final var context = introspector.getContext();
        store = new NewLocalSnapshotStore(context.getLz4BlockSize(), context.getSnapshotsDir(),
                context.getMaxLoadAttempts(), context.isUseLz4Compression());
        LOG.debug("PersistenceProvider started");
        delegate = new DefaultSnapshotPersistenceProvider(store);
    }

    @Override
    public void saveSnapshot(final Object snapshot, final  String persistenceId, final  long snapshotSequenceNr,
            final ActorRef actor) {
        delegate.saveSnapshot(snapshot, persistenceId, snapshotSequenceNr, actor);
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria, final String persistenceId,
            final ActorRef actor) {
        delegate.deleteSnapshots(criteria, persistenceId, actor);
    }

    public ListenableFuture<Optional<SnapshotOffer>> loadSnapshot(final String persistenceId,
            final SnapshotSelectionCriteria criteria) {
        return delegate.loadSnapshot(persistenceId, criteria);
    }

    @Override
    public boolean handleSnapshotResponse(SnapshotProtocol.Response response) {
        return response instanceof DeleteSnapshotsSuccess;
    }
}
