/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.actor.ActorRef;
import akka.persistence.DeleteSnapshotsSuccess;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.opendaylight.controller.cluster.persistence.NewLocalSnapshotStore;

public class DefaultSnapshotPersistenceProvider implements SnapshotPersistenceProvider {
    private final NewLocalSnapshotStore store;

    @VisibleForTesting
    public DefaultSnapshotPersistenceProvider() {
        store = new NewLocalSnapshotStore("256KB", "snapshots", 3, false);
    }

    public DefaultSnapshotPersistenceProvider(NewLocalSnapshotStore store) {
        this.store = store;
    }

    @Override
    public void saveSnapshot(Object snapshot, String persistenceId, long snapshotSequenceNr, ActorRef actor) {
        Futures.addCallback(store.doSaveAsync(new SnapshotMetadata(persistenceId, snapshotSequenceNr, 0L), snapshot),
                new FutureCallback<SnapshotMetadata>() {
                    @Override
                    public void onSuccess(final SnapshotMetadata result) {
                        actor.tell(new SaveSnapshotSuccess(result), ActorRef.noSender());
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        actor.tell(new SaveSnapshotFailure(null, failure), ActorRef.noSender());
                    }
                }, MoreExecutors.directExecutor());
        //store.doSaveAsync(new SnapshotMetadata(persistenceId, snapshotSequenceNr, 0L), snapshot);
    }

    @Override
    public void deleteSnapshots(SnapshotSelectionCriteria criteria, String persistenceId, ActorRef actor) {
        store.doDeleteAsync(persistenceId, criteria);
    }

    public ListenableFuture<Optional<SnapshotOffer>> loadSnapshot(final String persistenceId,
                                                                  final SnapshotSelectionCriteria criteria) {
        return store.doLoadAsync(persistenceId, criteria);
    }

    @Override
    public boolean handleSnapshotResponse(SnapshotProtocol.Response response) {
        return response instanceof DeleteSnapshotsSuccess;
    }
}
