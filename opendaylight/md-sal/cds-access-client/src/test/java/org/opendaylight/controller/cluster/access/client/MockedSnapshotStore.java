/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.snapshot.japi.SnapshotStore;
import com.google.common.base.Preconditions;
import java.util.Optional;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Instantiated by akka. MockedSnapshotStore forwards method calls as
 * {@link MockedSnapshotStoreMessage} messages to delegate actor. Delegate reference
 * must be sent as a message to this snapshot store.
 */
class MockedSnapshotStore extends SnapshotStore {

    private static final long TIMEOUT = 1000;

    private ActorRef delegate;

    /**
     * Marker interface for messages produced by MockedSnapshotStore.
     */
    interface MockedSnapshotStoreMessage {
    }

    @Override
    public Future<Optional<SelectedSnapshot>> doLoadAsync(final String persistenceId,
                                                          final SnapshotSelectionCriteria criteria) {
        return askDelegate(new LoadRequest(persistenceId, criteria));
    }

    @Override
    public Future<Void> doSaveAsync(final SnapshotMetadata metadata, final Object snapshot) {
        return askDelegate(new SaveRequest(metadata, snapshot));
    }

    @Override
    public Future<Void> doDeleteAsync(final SnapshotMetadata metadata) {
        return askDelegate(new DeleteByMetadataRequest(metadata));
    }

    @Override
    public Future<Void> doDeleteAsync(final String persistenceId, final SnapshotSelectionCriteria criteria) {
        return askDelegate(new DeleteByCriteriaRequest(persistenceId, criteria));
    }

    @Override
    public void unhandled(final Object message) {
        if (message instanceof ActorRef) {
            delegate = (ActorRef) message;
            return;
        }
        super.unhandled(message);
    }

    private <T> Future<T> askDelegate(final MockedSnapshotStoreMessage message) {
        Preconditions.checkNotNull(delegate, "Delegate ref wasn't sent");
        final Future<Object> ask = Patterns.ask(delegate, message, TIMEOUT);
        return transform(ask);
    }

    private <T> Future<T> transform(final Future<Object> future) {
        final Promise<T> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) throws Throwable {
                if (success instanceof Throwable) {
                    promise.failure((Throwable) success);
                    return;
                }
                if (success == Void.TYPE) {
                    promise.success(null);
                    return;
                }
                promise.success((T) success);
            }
        }, context().dispatcher());
        return promise.future();
    }

    class LoadRequest implements MockedSnapshotStoreMessage {
        private final String persistenceId;
        private final SnapshotSelectionCriteria criteria;

        LoadRequest(final String persistenceId, final SnapshotSelectionCriteria criteria) {
            this.persistenceId = persistenceId;
            this.criteria = criteria;
        }

        public String getPersistenceId() {
            return persistenceId;
        }

        public SnapshotSelectionCriteria getCriteria() {
            return criteria;
        }
    }

    class DeleteByCriteriaRequest implements MockedSnapshotStoreMessage {
        private final String persistenceId;
        private final SnapshotSelectionCriteria criteria;

        DeleteByCriteriaRequest(final String persistenceId, final SnapshotSelectionCriteria criteria) {
            this.persistenceId = persistenceId;
            this.criteria = criteria;
        }

        public String getPersistenceId() {
            return persistenceId;
        }

        public SnapshotSelectionCriteria getCriteria() {
            return criteria;
        }
    }

    class DeleteByMetadataRequest implements MockedSnapshotStoreMessage {
        private final SnapshotMetadata metadata;

        DeleteByMetadataRequest(final SnapshotMetadata metadata) {
            this.metadata = metadata;
        }

        public SnapshotMetadata getMetadata() {
            return metadata;
        }
    }

    class SaveRequest implements MockedSnapshotStoreMessage {
        private final SnapshotMetadata metadata;
        private final Object snapshot;

        SaveRequest(final SnapshotMetadata metadata, final Object snapshot) {
            this.metadata = metadata;
            this.snapshot = snapshot;
        }

        public SnapshotMetadata getMetadata() {
            return metadata;
        }

        public Object getSnapshot() {
            return snapshot;
        }
    }
}
