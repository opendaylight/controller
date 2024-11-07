/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.slf4j.Logger;

/**
 * Implementation of ElectionTerm for the RaftActor.
 */
class ElectionTermImpl implements ElectionTerm {
    private final DataPersistenceProvider persistence;
    private final Logger log;
    private final String logId;

    private @NonNull TermInfo currentTerm;

    ElectionTermImpl(final DataPersistenceProvider persistence, final String logId, final Logger log,
            final TermInfo currentTerm) {
        this.persistence = persistence;
        this.logId = logId;
        this.log = log;
        this.currentTerm = requireNonNull(currentTerm);
    }

    ElectionTermImpl(final DataPersistenceProvider persistence, final String logId, final Logger log) {
        this(persistence, logId, log, new TermInfo(0));
    }

    @Override
    public TermInfo currentTerm() {
        return currentTerm;
    }

    @Override
    public void update(final TermInfo newTerm) {
        currentTerm = requireNonNull(newTerm);
        log.debug("{}: Set currentTerm={}, votedFor={}", logId, newTerm.term(), newTerm.votedFor());
    }

    @Override
    public void updateAndPersist(final TermInfo newTerm) {
        update(newTerm);
        // FIXME : Maybe first persist then update the state
        persistence.persist(new UpdateElectionTerm(newTerm), NoopProcedure.instance());
    }
}
