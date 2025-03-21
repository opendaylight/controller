/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.persisted.NoopPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The behavior of a RaftActor when it is in the PreLeader state. This state performs all the duties of
 * Leader with the added behavior of attempting to commit all uncommitted entries from the previous leader's
 * term. Raft does not allow a leader to commit entries from a previous term by simply counting replicas -
 * only entries from the leader's current term can be committed (§5.4.2). Rather then waiting for a client
 * interaction to commit a new entry, the PreLeader state immediately appends a no-op entry (NoopPayload) to
 * the log with the leader's current term. Once the no-op entry is committed, all prior entries are committed
 * indirectly. Once all entries are committed, ie commitIndex matches the last log index, it switches to the
 * normal Leader state.
 *
 * <p>The use of a no-op entry in this manner is outlined in the last paragraph in §8 of the
 * <a href="https://raft.github.io/raft.pdf">extended raft version</a>.
 *
 * @author Thomas Pantelis
 */
public final class PreLeader extends AbstractLeader {
    private static final Logger LOG = LoggerFactory.getLogger(PreLeader.class);

    PreLeader(final RaftActorContext context) {
        super(context, RaftState.PreLeader);

        context.getActor().tell(NoopPayload.INSTANCE, context.getActor());
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        if (message instanceof ApplyState) {
            final var lastApplied = context.getLastApplied();
            final var lastIndex = context.getReplicatedLog().lastIndex();
            LOG.debug("{}: Received {} - lastApplied: {}, lastIndex: {}", logName, message, lastApplied, lastIndex);
            return lastApplied < lastIndex ? this
                // We've applied all entries - we can switch to Leader.
                : switchBehavior(new Leader(context, this));
        } else {
            return super.handleMessage(sender, message);
        }
    }

    @Override
    PreLeader handleAppendEntriesReply(final ActorRef sender, final AppendEntriesReply appendEntriesReply) {
        processAppendEntriesReply(sender, appendEntriesReply);
        return this;
    }
}
