/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Leader which is termed as isolated.
 *
 * <p>If the reply from the majority of the followers is not received then the leader changes its behavior
 * to IsolatedLeader. An isolated leader may have followers and they would continue to receive replicated messages.
 *
 * <p>A schedule is run, at an interval of (10 * Heartbeat-time-interval), in the Leader
 * to check if its isolated or not.
 *
 * <p>In the Isolated Leader , on every AppendEntriesReply, we aggressively check if the leader is isolated.
 * If no, then the state is switched back to Leader.
 */
public final class IsolatedLeader extends AbstractLeader {
    private static final Logger LOG = LoggerFactory.getLogger(IsolatedLeader.class);

    IsolatedLeader(final RaftActorContext context, final Leader initializeFromLeader) {
        super(context, RaftState.IsolatedLeader, requireNonNull(initializeFromLeader));
    }

    IsolatedLeader(final RaftActorContext context) {
        super(context, RaftState.IsolatedLeader);
    }

    // we received an Append Entries reply, we should switch the Behavior to Leader
    @Override
    RaftActorBehavior handleAppendEntriesReply(final ActorRef sender, final AppendEntriesReply appendEntriesReply) {
        processAppendEntriesReply(sender, appendEntriesReply);

        // it can happen that this isolated leader interacts with a new leader in the cluster and changes its state to
        // Follower, hence we only need to switch to Leader if the state is still Isolated
        if (isLeaderIsolated()) {
            return this;
        }

        LOG.info("{}: IsolatedLeader {} switching from IsolatedLeader to Leader", getId(), getLeaderId());
        return switchBehavior(new Leader(context, this));
    }
}
