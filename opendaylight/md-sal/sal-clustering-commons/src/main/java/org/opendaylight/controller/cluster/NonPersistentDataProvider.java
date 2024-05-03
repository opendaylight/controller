/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import static java.util.Objects.requireNonNull;

import akka.persistence.JournalProtocol;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotSelectionCriteria;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DataPersistenceProvider implementation with persistence disabled, essentially a no-op.
 */
public class NonPersistentDataProvider implements DataPersistenceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(NonPersistentDataProvider.class);

    private final ExecuteInSelfActor actor;

    public NonPersistentDataProvider(final ExecuteInSelfActor actor) {
        this.actor = requireNonNull(actor);
    }

    @Override
    public boolean isRecoveryApplicable() {
        return false;
    }

    @Override
    public <T extends PersistentData> void persist(final T entry, final Consumer<T> procedure) {
        invokeCallback(procedure, entry);
    }

    @Override
    public <T extends PersistentData> void persistAsync(final T entry, final Consumer<T> procedure) {
        actor.executeInSelf(() -> invokeCallback(procedure, entry));
    }

    @Override
    public void saveSnapshot(final Object snapshot) {
        // no-op
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        // no-op
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        // no-op
    }

    @Override
    public long getLastSequenceNumber() {
        return -1;
    }

    @NonNullByDefault
    @SuppressWarnings("checkstyle:IllegalCatch")
    static <T extends PersistentData> void invokeCallback(final Consumer<T> callback, final T argument) {
        try {
            callback.accept(argument);
        } catch (Exception e) {
            LOG.error("An unexpected error occurred", e);
        }
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return false;
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return false;
    }
}
