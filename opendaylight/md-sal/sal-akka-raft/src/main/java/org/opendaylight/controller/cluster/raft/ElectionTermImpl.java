/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.slf4j.Logger;

/**
 * Implementation of ElectionTerm for the RaftActor.
 */
class ElectionTermImpl implements ElectionTerm {
    /**
     * Identifier of the actor whose election term information this is
     */
    private long currentTerm = 0;
    private String votedFor = null;

    private final DataPersistenceProvider persistence;

    private final Logger log;
    private final String logId;

    ElectionTermImpl(DataPersistenceProvider persistence, String logId, Logger log) {
        this.persistence = persistence;
        this.logId = logId;
        this.log = log;
    }

    @Override
    public long getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public String getVotedFor() {
        return votedFor;
    }

    @Override public void update(long currentTerm, String votedFor) {
        if(log.isDebugEnabled()) {
            log.debug("{}: Set currentTerm={}, votedFor={}", logId, currentTerm, votedFor);
        }
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
    }

    @Override
    public void updateAndPersist(long currentTerm, String votedFor){
        update(currentTerm, votedFor);
        // FIXME : Maybe first persist then update the state
        persistence.persist(new UpdateElectionTerm(this.currentTerm, this.votedFor), NoopProcedure.instance());
    }
}