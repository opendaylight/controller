/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm;
import org.slf4j.Logger;

/**
 * Implementation of ElectionTerm for the RaftActor.
 */
class ElectionTermImpl implements ElectionTerm {
    private final Procedure<UpdateElectionTerm> updateProcedure = new Procedure<UpdateElectionTerm>() {
        @Override
        public void apply(final UpdateElectionTerm param) {
            update(param.getCurrentTerm(), param.getVotedFor());
        }
    };

    /**
     * Identifier of the actor whose election term information this is
     */
    private long currentTerm = 0;
    private String votedFor = null;

    private final DataPersistenceProvider persistence;

    private final Logger log;
    private final String logId;

    ElectionTermImpl(final DataPersistenceProvider persistence, final String logId, final Logger log) {
        this.persistence = Preconditions.checkNotNull(persistence);
        this.logId = logId;
        this.log = Preconditions.checkNotNull(log);
    }

    @Override
    public long getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public String getVotedFor() {
        return votedFor;
    }

    @Override
    public void update(final long currentTerm, final String votedFor) {
        log.debug("{}: Set currentTerm={}, votedFor={}", logId, currentTerm, votedFor);
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
    }

    @Override
    public void updateAndPersist(final long currentTerm, final String votedFor) {
        log.debug("{}: Persisting currentTerm={}, votedFor={}", logId, currentTerm, votedFor);
        persistence.persist(new UpdateElectionTerm(currentTerm, votedFor), updateProcedure);
    }
}
