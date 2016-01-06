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
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm;
import org.slf4j.Logger;

/**
 * Implementation of ElectionTerm for the RaftActor.
 */
class ElectionTermImpl implements ElectionTerm {
    private final DataPersistenceProvider persistence;
    
    /**
     * Identifier of the actor whose election term information this is
     */
    private final long currentTerm;
    private final String votedFor;

    private final Logger log;
    private final String logId;

    private ElectionTermImpl(DataPersistenceProvider persistence, String logId, Logger log, long currentTerm,
            String votedFor) {
        this.persistence = Preconditions.checkNotNull(persistence);
        this.logId = logId;
        this.log = Preconditions.checkNotNull(log);
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
    }

    ElectionTermImpl(final DataPersistenceProvider persistence, final String logId, final Logger log) {
        this(persistence, logId, log, 0, null);
    }

    @Override
    public long getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public String getVotedFor() {
        return votedFor;
    }

    /**
     * To be called mainly when we are recovering in-memory election state from
     * persistent storage
     *
     * @param currentTerm
     * @param votedFor
     */
    public ElectionTermImpl nextTerm(final long currentTerm, final String votedFor) {
        log.debug("{}: Set currentTerm={}, votedFor={}", logId, currentTerm, votedFor);
        return new ElectionTermImpl(persistence, logId, log, currentTerm, votedFor);
    }

    /**
     * FIXME: update javadoc to note that the new term will be passed back
     *
     * To be called when we need to update the current term either because we
     * received a message from someone with a more up-to-date term or because we
     * just voted for someone
     * <p>
     * This information needs to be persisted so that on recovery the replica
     * can start itself in the right term and know if it has already voted in
     * that term or not.
     *
     * Note that the update will be asynchronous and observed only once persistence
     * has had a chance to run. For that to happen, the caller is required to
     * return from actor processing. If further actions need to be taken once
     * the update has completed, provide a callback containing the required code.
     *
     * @param currentTerm
     * @param votedFor
     * @param callback callback to be invoked once update has completed
     */
    void nextPersistentTerm(final long currentTerm, final String votedFor,
            @Nonnull final Procedure<ElectionTermImpl> callback) {
        log.debug("{}: Persisting currentTerm={}, votedFor={}", logId, currentTerm, votedFor);
        persistence.persist(new UpdateElectionTerm(currentTerm, votedFor), new Procedure<UpdateElectionTerm>() {
            @Override
            public void apply(final UpdateElectionTerm param) throws Exception {
                callback.apply(nextTerm(param.getCurrentTerm(), param.getVotedFor()));
            }
        });
    }
}
