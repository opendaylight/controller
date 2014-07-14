/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.persistence.RecoveryCompleted;
import akka.persistence.UntypedPersistentActor;
import org.opendaylight.controller.cluster.raft.behaviors.Candidate;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.internal.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.internal.messages.Replicate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RaftActor encapsulates a state machine that needs to be kept synchronized
 * in a cluster. It implements the RAFT algorithm as described in the paper
 * <a href='https://ramcloud.stanford.edu/wiki/download/attachments/11370504/raft.pdf'>
 *     In Search of an Understandable Consensus Algorithm</a>
 * <p>
 * RaftActor has 3 states and each state has a certain behavior associated
 * with it. A Raft actor can behave as,
 * <ul>
 *     <li> A Leader </li>
 *     <li> A Follower (or) </li>
 *     <li> A Candidate </li>
 * </ul>
 *
 * <p>
 * A RaftActor MUST be a Leader in order to accept requests from clients to
 * change the state of it's encapsulated state machine. Once a RaftActor becomes
 * a Leader it is also responsible for ensuring that all followers ultimately
 * have the same log and therefore the same state machine as itself.
 *
 * <p>
 * The current behavior of a RaftActor determines how election for leadership
 * is initiated and how peer RaftActors react to request for votes.
 *
 * <p>
 * Each RaftActor also needs to know the current election term. It uses this
 * information for a couple of things. One is to simply figure out who it
 * voted for in the last election. Another is to figure out if the message
 * it received to update it's state is stale.
 *
 * <p>
 * The RaftActor uses akka-persistence to store it's replicated log.
 * Furthermore through it's behaviors a Raft Actor determines
 *
 * <ul>
 * <li> when a log entry should be persisted </li>
 * <li> when a log entry should be applied to the state machine (and) </li>
 * <li> when a snapshot should be saved </li>
 * </ul>
 *
 * <a href="http://doc.akka.io/api/akka/2.3.3/index.html#akka.persistence.UntypedEventsourcedProcessor">UntypeEventSourceProcessor</a>
 */
public abstract class RaftActor extends UntypedPersistentActor {
    protected final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    /**
     *  The current state determines the current behavior of a RaftActor
     * A Raft Actor always starts off in the Follower State
     */
    private RaftActorBehavior currentBehavior;

    /**
     * This context should NOT be passed directly to any other actor it is
     * only to be consumed by the RaftActorBehaviors
     */
    private RaftActorContext context;

    /**
     * The in-memory journal
     */
    private ReplicatedLogImpl replicatedLog = new ReplicatedLogImpl();



    public RaftActor(String id, Map<String, String> peerAddresses){
        context = new RaftActorContextImpl(this.getSelf(),
            this.getContext(),
            id, new ElectionTermImpl(getSelf().path().toString()),
            -1, -1, replicatedLog, peerAddresses, LOG);
        currentBehavior = switchBehavior(RaftState.Follower);
    }

    @Override public void onReceiveRecover(Object message) {
        if(message instanceof ReplicatedLogEntry) {
            replicatedLog.append((ReplicatedLogEntry) message);
        } else if(message instanceof RecoveryCompleted){
            LOG.debug("Log now has messages to index : " + replicatedLog.lastIndex());
        }
    }

    @Override public void onReceiveCommand(Object message) {
        if(message instanceof ApplyState){

            ApplyState applyState = (ApplyState)  message;

            LOG.debug("Applying state for log index {}", applyState.getReplicatedLogEntry().getIndex());

            applyState(applyState.getClientActor(), applyState.getIdentifier(),
                applyState.getReplicatedLogEntry().getData());
        } else if(message instanceof FindLeader){
            getSender().tell(new FindLeaderReply(
                context.getPeerAddress(currentBehavior.getLeaderId())),
                getSelf());
        } else {
            RaftState state =
                currentBehavior.handleMessage(getSender(), message);
            currentBehavior = switchBehavior(state);
        }
    }

    private RaftActorBehavior switchBehavior(RaftState state){
        if(currentBehavior != null) {
            if (currentBehavior.state() == state) {
                return currentBehavior;
            }
            LOG.info("Switching from state " + currentBehavior.state() + " to "
                + state);

            try {
                currentBehavior.close();
            } catch (Exception e) {
                LOG.error(e, "Failed to close behavior : " + currentBehavior.state());
            }

        } else {
            LOG.info("Switching behavior to " + state);
        }
        RaftActorBehavior behavior = null;
        if(state == RaftState.Candidate){
            behavior = new Candidate(context);
        } else if(state == RaftState.Follower){
            behavior = new Follower(context);
        } else {
            behavior = new Leader(context);
        }
        return behavior;
    }

    /**
     * When a derived RaftActor needs to persist something it must call
     * persistData.
     *
     * @param clientActor
     * @param identifier
     * @param data
     */
    protected void persistData(ActorRef clientActor, String identifier, Object data){
        LOG.debug("Persist data " + identifier);
        ReplicatedLogEntry replicatedLogEntry = new ReplicatedLogImplEntry(
            context.getReplicatedLog().lastIndex() + 1,
            context.getTermInformation().getCurrentTerm(), data);

        replicatedLog.appendAndPersist(clientActor, identifier, replicatedLogEntry);
    }

    protected abstract void applyState(ActorRef clientActor, String identifier, Object data);

    protected String getId(){
        return context.getId();
    }

    protected boolean isLeader(){
        return context.getId().equals(currentBehavior.getLeaderId());
    }

    protected ActorSelection getLeader(){
        String leaderId = currentBehavior.getLeaderId();
        String peerAddress = context.getPeerAddress(leaderId);
        LOG.debug("getLeader leaderId = " + leaderId + " peerAddress = " + peerAddress);
        return context.actorSelection(peerAddress);
    }

    private class ReplicatedLogImpl implements ReplicatedLog {
        private final List<ReplicatedLogEntry> journal = new ArrayList();
        private long snapshotIndex = 0;
        private Object snapShot = null;


        @Override public ReplicatedLogEntry get(long index) {
            if(index < 0 || index >= journal.size()){
                return null;
            }

            return journal.get((int) (index - snapshotIndex));
        }

        @Override public ReplicatedLogEntry last() {
            if(journal.size() == 0){
                return null;
            }
            return get(journal.size() - 1);
        }

        @Override public long lastIndex() {
            if(journal.size() == 0){
                return -1;
            }

            return last().getIndex();
        }

        @Override public long lastTerm() {
            if(journal.size() == 0){
                return -1;
            }

            return last().getTerm();
        }


        @Override public void removeFrom(long index) {
            if(index < 0 || index >= journal.size()){
                return;
            }
            for(int i= (int) (index - snapshotIndex) ; i < journal.size() ; i++){
                deleteMessage(i);
                journal.remove(i);
            }
        }

        @Override public void append(final ReplicatedLogEntry replicatedLogEntry) {
            journal.add(replicatedLogEntry);
        }

        @Override public List<ReplicatedLogEntry> getFrom(long index) {
            List<ReplicatedLogEntry> entries = new ArrayList<>(100);
            if(index < 0 || index >= journal.size()){
                return entries;
            }
            for(int i= (int) (index - snapshotIndex); i < journal.size() ; i++){
                entries.add(journal.get(i));
            }
            return entries;
        }

        @Override public void appendAndPersist(final ReplicatedLogEntry replicatedLogEntry){
            appendAndPersist(null, null, replicatedLogEntry);
        }

        public void appendAndPersist(final ActorRef clientActor, final String identifier, final ReplicatedLogEntry replicatedLogEntry){
            context.getLogger().debug("Append log entry and persist" + replicatedLogEntry.getIndex());
            // FIXME : By adding the replicated log entry to the in-memory journal we are not truly ensuring durability of the logs
            journal.add(replicatedLogEntry);
            persist(replicatedLogEntry,
                new Procedure<ReplicatedLogEntry>() {
                    public void apply(ReplicatedLogEntry evt) throws Exception {
                        // Send message for replication
                        if(clientActor != null) {
                            currentBehavior.handleMessage(getSelf(),
                                new Replicate(clientActor, identifier,
                                    replicatedLogEntry));
                        }
                    }
                });
        }

        @Override public long size() {
            return journal.size() + snapshotIndex;
        }
    }

    private static class ReplicatedLogImplEntry implements ReplicatedLogEntry,
        Serializable {

        private final long index;
        private final long term;
        private final Object payload;

        public ReplicatedLogImplEntry(long index, long term, Object payload){

            this.index = index;
            this.term = term;
            this.payload = payload;
        }

        @Override public Object getData() {
            return payload;
        }

        @Override public long getTerm() {
            return term;
        }

        @Override public long getIndex() {
            return index;
        }
    }


}
