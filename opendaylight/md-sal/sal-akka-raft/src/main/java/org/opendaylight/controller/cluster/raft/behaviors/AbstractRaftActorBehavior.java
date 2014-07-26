/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;

/**
 * Abstract class that represents the behavior of a RaftActor
 * <p/>
 * All Servers:
 * <ul>
 * <li> If commitIndex > lastApplied: increment lastApplied, apply
 * log[lastApplied] to state machine (§5.3)
 * <li> If RPC request or response contains term T > currentTerm:
 * set currentTerm = T, convert to follower (§5.1)
 */
public abstract class AbstractRaftActorBehavior implements RaftActorBehavior {

    /**
     * Information about the RaftActor whose behavior this class represents
     */
    protected final RaftActorContext context;


    protected AbstractRaftActorBehavior(RaftActorContext context) {
        this.context = context;
    }

    /**
     * Derived classes should not directly handle AppendEntries messages it
     * should let the base class handle it first. Once the base class handles
     * the AppendEntries message and does the common actions that are applicable
     * in all RaftState's it will delegate the handling of the AppendEntries
     * message to the derived class to do more state specific handling by calling
     * this method
     *
     * @param sender         The actor that sent this message
     * @param appendEntries  The AppendEntries message
     * @param suggestedState The state that the RaftActor should be in based
     *                       on the base class's processing of the AppendEntries
     *                       message
     * @return
     */
    protected abstract RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState);

    /**
     * Derived classes should not directly handle AppendEntriesReply messages it
     * should let the base class handle it first. Once the base class handles
     * the AppendEntriesReply message and does the common actions that are
     * applicable in all RaftState's it will delegate the handling of the
     * AppendEntriesReply message to the derived class to do more state specific
     * handling by calling this method
     *
     * @param sender             The actor that sent this message
     * @param appendEntriesReply The AppendEntriesReply message
     * @param suggestedState     The state that the RaftActor should be in based
     *                           on the base class's processing of the
     *                           AppendEntriesReply message
     * @return
     */

    protected abstract RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState);

    /**
     * Derived classes should not directly handle RequestVote messages it
     * should let the base class handle it first. Once the base class handles
     * the RequestVote message and does the common actions that are applicable
     * in all RaftState's it will delegate the handling of the RequestVote
     * message to the derived class to do more state specific handling by calling
     * this method
     *
     * @param sender         The actor that sent this message
     * @param requestVote    The RequestVote message
     * @param suggestedState The state that the RaftActor should be in based
     *                       on the base class's processing of the RequestVote
     *                       message
     * @return
     */
    protected abstract RaftState handleRequestVote(ActorRef sender,
        RequestVote requestVote, RaftState suggestedState);

    /**
     * Derived classes should not directly handle RequestVoteReply messages it
     * should let the base class handle it first. Once the base class handles
     * the RequestVoteReply message and does the common actions that are
     * applicable in all RaftState's it will delegate the handling of the
     * RequestVoteReply message to the derived class to do more state specific
     * handling by calling this method
     *
     * @param sender           The actor that sent this message
     * @param requestVoteReply The RequestVoteReply message
     * @param suggestedState   The state that the RaftActor should be in based
     *                         on the base class's processing of the RequestVote
     *                         message
     * @return
     */

    protected abstract RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState);

    /**
     * @return The derived class should return the state that corresponds to
     * it's behavior
     */
    protected abstract RaftState state();

    @Override
    public RaftState handleMessage(ActorRef sender, Object message) {
        RaftState raftState = state();
        if (message instanceof RaftRPC) {
            raftState = applyTerm((RaftRPC) message);
        }
        if (message instanceof AppendEntries) {
            AppendEntries appendEntries = (AppendEntries) message;
            if (appendEntries.getLeaderCommit() > context.getLastApplied()
                .get()) {
                applyLogToStateMachine(appendEntries.getLeaderCommit());
            }
            raftState = handleAppendEntries(sender, appendEntries, raftState);
        } else if (message instanceof AppendEntriesReply) {
            raftState =
                handleAppendEntriesReply(sender, (AppendEntriesReply) message,
                    raftState);
        } else if (message instanceof RequestVote) {
            raftState =
                handleRequestVote(sender, (RequestVote) message, raftState);
        } else if (message instanceof RequestVoteReply) {
            raftState =
                handleRequestVoteReply(sender, (RequestVoteReply) message,
                    raftState);
        }
        return raftState;
    }

    private RaftState applyTerm(RaftRPC rpc) {
        if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()
            .get()) {
            context.getTermInformation().update(rpc.getTerm(), null);
            return RaftState.Follower;
        }
        return state();
    }

    private void applyLogToStateMachine(long index) {
        context.getLastApplied().set(index);
    }
}
