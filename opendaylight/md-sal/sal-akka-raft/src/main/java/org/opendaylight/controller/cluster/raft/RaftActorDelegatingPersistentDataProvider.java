/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.DelegatingPersistentDataProvider;
import org.opendaylight.controller.cluster.PersistentData;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.messages.PersistentPayload;

/**
 * The DelegatingPersistentDataProvider used by RaftActor to override the configured persistent provider to
 * persist ReplicatedLogEntry's based on whether or not the payload is a PersistentPayload instance.
 *
 * @author Thomas Pantelis
 */
class RaftActorDelegatingPersistentDataProvider extends DelegatingPersistentDataProvider {
    private final PersistentDataProvider persistentProvider;

    RaftActorDelegatingPersistentDataProvider(final DataPersistenceProvider delegate,
            final PersistentDataProvider persistentProvider) {
        super(delegate);
        this.persistentProvider = requireNonNull(persistentProvider);
    }

    @Override
    public <T extends PersistentData> void persist(final T entry, final Consumer<T> callback) {
        doPersist(entry, callback, false);
    }

    @Override
    public <T extends PersistentData> void persistAsync(final T entry, final Consumer<T> callback) {
        doPersist(entry, callback, true);
    }

    @NonNullByDefault
    private <T extends PersistentData> void doPersist(final T entry, final Consumer<T> callback, final boolean async) {
        if (!getDelegate().isRecoveryApplicable() && entry instanceof ReplicatedLogEntry replicatedLogEntry
            && replicatedLogEntry.getData() instanceof PersistentPayload payload) {
            // We persist the Payload but not the ReplicatedLogEntry to avoid gaps in the journal indexes on recovery
            // if data persistence is later enabled.
            if (async) {
                persistentProvider.persistAsync(payload, p -> callback.accept(entry));
            } else {
                persistentProvider.persist(payload, p -> callback.accept(entry));
            }
        } else if (async) {
            super.persistAsync(entry, callback);
        } else {
            super.persist(entry, callback);
        }
    }
}
