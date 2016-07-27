/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.persisted.NoopPayload;

/**
 * The behavior of a RaftActor when it is in the PreLeader state. This state performs all the duties of
 * Leader with the added behavior of attempting to commit all uncommitted entries from the previous leader's
 * term. Raft does not allow a leader to commit entries from a previous term by simply counting replicas -
 * only entries from the leader's current term can be committed (§5.4.2). Rather then waiting for a client
 * interaction to commit a new entry, the PreLeader state immediately appends a no-op entry (NoopPayload) to
 * the log with the leader's current term. Once the no-op entry is committed, all prior entries are committed
 * indirectly. Once all entries are committed, ie commitIndex matches the last log index, it switches to the
 * normal Leader state.
 * <p>
 * The use of a no-op entry in this manner is outlined in the last paragraph in §8 of the
 * <a href="https://raft.github.io/raft.pdf">extended raft version</a>.
 *
 * @author Thomas Pantelis
 */
public class PreLeader extends AbstractLeader {

    public PreLeader(RaftActorContext context) {
        super(context, RaftState.PreLeader);

        context.getActor().tell(NoopPayload.INSTANCE, context.getActor());
    }

    @Override
    protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender, AppendEntriesReply appendEntriesReply) {
        RaftActorBehavior returnBehavior = super.handleAppendEntriesReply(sender, appendEntriesReply);

        if(context.getCommitIndex() >= context.getReplicatedLog().lastIndex()) {
            // We've committed all entries - we can switch to Leader.
            returnBehavior = internalSwitchBehavior(new Leader(context, this));
        }

        return returnBehavior;
    }
}
