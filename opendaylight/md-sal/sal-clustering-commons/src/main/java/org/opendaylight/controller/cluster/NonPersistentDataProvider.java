/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DataPersistenceProvider implementation with persistence disabled, essentially a no-op.
 */
public class NonPersistentDataProvider implements DataPersistenceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(NonPersistentDataProvider.class);

    @Override
    public boolean isRecoveryApplicable() {
        return false;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <T> void persist(T entry, Procedure<T> procedure) {
        try {
            procedure.apply(entry);
        } catch (Exception e) {
            LOG.error("An unexpected error occurred", e);
        }
    }

    @Override
    public <T> void persistAsync(T entry, Procedure<T> procedure) {
        persist(entry, procedure);
    }

    @Override
    public void saveSnapshot(Object snapshot) {
        // no-op
    }

    @Override
    public void deleteSnapshots(SnapshotSelectionCriteria criteria) {
        // no-op
    }

    @Override
    public void deleteMessages(long sequenceNumber) {
        // no-op
    }

    @Override
    public long getLastSequenceNumber() {
        return -1;
    }
}
