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
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.UntypedPersistentActor;
import org.opendaylight.controller.cluster.raft.behaviors.Candidate;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.AddRaftPeer;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.internal.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.RemoveRaftPeer;
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
 * In Search of an Understandable Consensus Algorithm</a>
 * <p/>
 * RaftActor has 3 states and each state has a certain behavior associated
 * with it. A Raft actor can behave as,
 * <ul>
 * <li> A Leader </li>
 * <li> A Follower (or) </li>
 * <li> A Candidate </li>
 * </ul>
 * <p/>
 * <p/>
 * A RaftActor MUST be a Leader in order to accept requests from clients to
 * change the state of it's encapsulated state machine. Once a RaftActor becomes
 * a Leader it is also responsible for ensuring that all followers ultimately
 * have the same log and therefore the same state machine as itself.
 * <p/>
 * <p/>
 * The current behavior of a RaftActor determines how election for leadership
 * is initiated and how peer RaftActors react to request for votes.
 * <p/>
 * <p/>
 * Each RaftActor also needs to know the current election term. It uses this
 * information for a couple of things. One is to simply figure out who it
 * voted for in the last election. Another is to figure out if the message
 * it received to update it's state is stale.
 * <p/>
 * <p/>
 * The RaftActor uses akka-persistence to store it's replicated log.
 * Furthermore through it's behaviors a Raft Actor determines
 * <p/>
 * <ul>
 * <li> when a log entry should be persisted </li>
 * <li> when a log entry should be applied to the state machine (and) </li>
 * <li> when a snapshot should be saved </li>
 * </ul>
 */
public abstract class RaftActor extends UntypedPersistentActor {
    protected final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    /**
     * The current state determines the current behavior of a RaftActor
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


    public RaftActor(String id, Map<String, String> peerAddresses) {
        final String id1 = getSelf().path().toString();
        context = new RaftActorContextImpl(this.getSelf(),
            this.getContext(),
            id, new ElectionTermImpl(),
            -1, -1, replicatedLog, peerAddresses, LOG);
    }

    @Override public void onReceiveRecover(Object message) {
        if (message instanceof SnapshotOffer) {
            SnapshotOffer offer = (SnapshotOffer) message;
            Snapshot snapshot = (Snapshot) offer.snapshot();

            // Create a replicated log with the snapshot information
            // The replicated log can be used later on to retrieve this snapshot
            // when we need to install it on a peer
            replicatedLog = new ReplicatedLogImpl(snapshot);

            // Apply the snapshot to the actors state
            applySnapshot(snapshot.getState());

        } else if (message instanceof ReplicatedLogEntry) {
            replicatedLog.append((ReplicatedLogEntry) message);
        } else if (message instanceof DeleteEntries) {
            replicatedLog.removeFrom(((DeleteEntries) message).getFromIndex());
        } else if (message instanceof UpdateElectionTerm) {
            context.getTermInformation().update(((UpdateElectionTerm) message).getCurrentTerm(), ((UpdateElectionTerm) message).getVotedFor());
        } else if (message instanceof RecoveryCompleted) {
            LOG.debug(
                "Last index in log : " + replicatedLog.lastIndex());
            currentBehavior = switchBehavior(RaftState.Follower);
        }
    }

    @Override public void onReceiveCommand(Object message) {
        if (message instanceof ApplyState){
            ApplyState applyState = (ApplyState) message;

            LOG.debug("Applying state for log index {} data {}",
                applyState.getReplicatedLogEntry().getIndex(),
                applyState.getReplicatedLogEntry().getData());

            applyState(applyState.getClientActor(), applyState.getIdentifier(),
                applyState.getReplicatedLogEntry().getData());
        } else if(message instanceof ApplySnapshot ) {
            applySnapshot(((ApplySnapshot) message).getSnapshot());
        } else if (message instanceof FindLeader) {
            getSender().tell(new FindLeaderReply(
                    context.getPeerAddress(currentBehavior.getLeaderId())),
                getSelf()
            );
        } else if (message instanceof SaveSnapshotSuccess) {
            SaveSnapshotSuccess success = (SaveSnapshotSuccess) message;

            // TODO: Not sure if we want to be this aggressive with trimming stuff
            trimPersistentData(success.metadata().sequenceNr());

        } else if (message instanceof SaveSnapshotFailure) {

            // TODO: Handle failure in saving the snapshot
            // Maybe do retries on failure

        } else if (message instanceof AddRaftPeer){

            // FIXME : Do not add raft peers like this.
            // When adding a new Peer we have to ensure that the a majority of
            // the peers know about the new Peer. Doing it this way may cause
            // a situation where multiple Leaders may emerge
            AddRaftPeer arp = (AddRaftPeer)message;
           context.addToPeers(arp.getName(), arp.getAddress());

        } else if (message instanceof RemoveRaftPeer){

            RemoveRaftPeer rrp = (RemoveRaftPeer)message;
            context.removePeer(rrp.getName());

        } else {

            RaftState state =
                currentBehavior.handleMessage(getSender(), message);
            currentBehavior = switchBehavior(state);
        }
    }



    /**
     * When a derived RaftActor needs to persist something it must call
     * persistData.
     *
     * @param clientActor
     * @param identifier
     * @param data
     */
    protected void persistData(ActorRef clientActor, String identifier,
        Object data) {

        ReplicatedLogEntry replicatedLogEntry = new ReplicatedLogImplEntry(
            context.getReplicatedLog().lastIndex() + 1,
            context.getTermInformation().getCurrentTerm(), data);

        LOG.debug("Persist data {}", replicatedLogEntry);

        replicatedLog
            .appendAndPersist(clientActor, identifier, replicatedLogEntry);
    }

    protected String getId() {
        return context.getId();
    }

    /**
     * Derived actors can call the isLeader method to check if the current
     * RaftActor is the Leader or not
     *
     * @return true it this RaftActor is a Leader false otherwise
     */
    protected boolean isLeader() {
        return context.getId().equals(currentBehavior.getLeaderId());
    }

    /**
     * Derived actor can call getLeader if they need a reference to the Leader.
     * This would be useful for example in forwarding a request to an actor
     * which is the leader
     *
     * @return A reference to the leader if known, null otherwise
     */
    protected ActorSelection getLeader(){
        String leaderId = currentBehavior.getLeaderId();
        if (leaderId == null) {
            return null;
        }
        String peerAddress = context.getPeerAddress(leaderId);
        LOG.debug("getLeader leaderId = " + leaderId + " peerAddress = "
            + peerAddress);
        return context.actorSelection(peerAddress);
    }

    protected RaftState getRaftState() {
        return currentBehavior.state();
    }



    /**
     * The applyState method will be called by the RaftActor when some data
     * needs to be applied to the actor's state
     *
     * @param clientActor A reference to the client who sent this message. This
     *                    is the same reference that was passed to persistData
     *                    by the derived actor. clientActor may be null when
     *                    the RaftActor is behaving as a follower or during
     *                    recovery.
     * @param identifier  The identifier of the persisted data. This is also
     *                    the same identifier that was passed to persistData by
     *                    the derived actor. identifier may be null when
     *                    the RaftActor is behaving as a follower or during
     *                    recovery
     * @param data        A piece of data that was persisted by the persistData call.
     *                    This should NEVER be null.
     */
    protected abstract void applyState(ActorRef clientActor, String identifier,
        Object data);

    /**
     * This method will be called by the RaftActor when a snapshot needs to be
     * created. The derived actor should respond with its current state.
     * <p/>
     * During recovery the state that is returned by the derived actor will
     * be passed back to it by calling the applySnapshot  method
     *
     * @return The current state of the actor
     */
    protected abstract Object createSnapshot();

    /**
     * This method will be called by the RaftActor during recovery to
     * reconstruct the state of the actor.
     * <p/>
     * This method may also be called at any other point during normal
     * operations when the derived actor is out of sync with it's peers
     * and the only way to bring it in sync is by applying a snapshot
     *
     * @param snapshot A snapshot of the state of the actor
     */
    protected abstract void applySnapshot(Object snapshot);

    private RaftActorBehavior switchBehavior(RaftState state) {
        if (currentBehavior != null) {
            if (currentBehavior.state() == state) {
                return currentBehavior;
            }
            LOG.info("Switching from state " + currentBehavior.state() + " to "
                + state);

            try {
                currentBehavior.close();
            } catch (Exception e) {
                LOG.error(e,
                    "Failed to close behavior : " + currentBehavior.state());
            }

        } else {
            LOG.info("Switching behavior to " + state);
        }
        RaftActorBehavior behavior = null;
        if (state == RaftState.Candidate) {
            behavior = new Candidate(context);
        } else if (state == RaftState.Follower) {
            behavior = new Follower(context);
        } else {
            behavior = new Leader(context);
        }
        return behavior;
    }

    private void trimPersistentData(long sequenceNumber) {
        // Trim snapshots
        // FIXME : Not sure how exactly the SnapshotSelectionCriteria is applied
        // For now guessing that it is ANDed.
        deleteSnapshots(new SnapshotSelectionCriteria(
            sequenceNumber - 100000, 43200000));

        // Trim journal
        deleteMessages(sequenceNumber);
    }


    private class ReplicatedLogImpl implements ReplicatedLog {
        private final List<ReplicatedLogEntry> journal;
        private final Object snapshot;
        private long snapshotIndex = -1;
        private long snapshotTerm = -1;

        public ReplicatedLogImpl(
            Snapshot snapshot) {
            this.snapshot = snapshot.getState();
            this.snapshotIndex = snapshot.getLastAppliedIndex();
            this.snapshotTerm = snapshot.getLastAppliedTerm();

            this.journal = new ArrayList<>(snapshot.getUnAppliedEntries());
        }

        public ReplicatedLogImpl() {
            this.snapshot = null;
            this.journal = new ArrayList<>();
        }

        @Override public ReplicatedLogEntry get(long index) {
            int adjustedIndex = adjustedIndex(index);

            if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
                return null;
            }

            return journal.get(adjustedIndex);
        }

        @Override public ReplicatedLogEntry last() {
            if (journal.size() == 0) {
                return null;
            }
            return get(journal.size() - 1);
        }

        @Override public long lastIndex() {
            if (journal.size() == 0) {
                return -1;
            }

            return last().getIndex();
        }

        @Override public long lastTerm() {
            if (journal.size() == 0) {
                return -1;
            }

            return last().getTerm();
        }


        @Override public void removeFrom(long index) {
            int adjustedIndex = adjustedIndex(index);

            if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
                return;
            }

            journal.subList(adjustedIndex , journal.size()).clear();
        }


        @Override public void removeFromAndPersist(long index) {
            int adjustedIndex = adjustedIndex(index);

            if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
                return;
            }

            // FIXME: Maybe this should be done after the command is saved
            journal.subList(adjustedIndex , journal.size()).clear();

            persist(new DeleteEntries(adjustedIndex), new Procedure<DeleteEntries>(){

                @Override public void apply(DeleteEntries param)
                    throws Exception {
                    //FIXME : Doing nothing for now
                }
            });


        }

        @Override public void append(
            final ReplicatedLogEntry replicatedLogEntry) {
            journal.add(replicatedLogEntry);
        }

        @Override public List<ReplicatedLogEntry> getFrom(long index) {
            int adjustedIndex = adjustedIndex(index);

            List<ReplicatedLogEntry> entries = new ArrayList<>(100);
            if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
                return entries;
            }


            for (int i = adjustedIndex;
                 i < journal.size(); i++) {
                entries.add(journal.get(i));
            }
            return entries;
        }

        @Override public void appendAndPersist(
            final ReplicatedLogEntry replicatedLogEntry) {
            appendAndPersist(null, null, replicatedLogEntry);
        }

        public void appendAndPersist(final ActorRef clientActor,
            final String identifier,
            final ReplicatedLogEntry replicatedLogEntry) {
            context.getLogger().debug(
                "Append log entry and persist " + replicatedLogEntry);
            // FIXME : By adding the replicated log entry to the in-memory journal we are not truly ensuring durability of the logs
            journal.add(replicatedLogEntry);

            // When persisting events with persist it is guaranteed that the
            // persistent actor will not receive further commands between the
            // persist call and the execution(s) of the associated event
            // handler. This also holds for multiple persist calls in context
            // of a single command.
            persist(replicatedLogEntry,
                new Procedure<ReplicatedLogEntry>() {
                    public void apply(ReplicatedLogEntry evt) throws Exception {
                        // FIXME : Tentatively create a snapshot every hundred thousand entries. To be tuned.
                        if (size() > 100000) {
                            ReplicatedLogEntry lastAppliedEntry =
                                get(context.getLastApplied());
                            long lastAppliedIndex = -1;
                            long lastAppliedTerm = -1;
                            if (lastAppliedEntry != null) {
                                lastAppliedIndex = lastAppliedEntry.getIndex();
                                lastAppliedTerm = lastAppliedEntry.getTerm();
                            }

                            saveSnapshot(Snapshot.create(createSnapshot(),
                                getFrom(context.getLastApplied() + 1),
                                lastIndex(), lastTerm(), lastAppliedIndex,
                                lastAppliedTerm));
                        }
                        // Send message for replication
                        if (clientActor != null) {
                            currentBehavior.handleMessage(getSelf(),
                                new Replicate(clientActor, identifier,
                                    replicatedLogEntry)
                            );
                        }
                    }
                }
            );
        }

        @Override public long size() {
            return journal.size() + snapshotIndex + 1;
        }

        @Override public boolean isPresent(long index) {
            int adjustedIndex = adjustedIndex(index);

            if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
                return false;
            }
            return true;
        }

        @Override public boolean isInSnapshot(long index) {
            return index <= snapshotIndex;
        }

        @Override public Object getSnapshot() {
            return snapshot;
        }

        @Override public long getSnapshotIndex() {
            return snapshotIndex;
        }

        @Override public long getSnapshotTerm() {
            return snapshotTerm;
        }

        private int adjustedIndex(long index) {
            if(snapshotIndex < 0){
                return (int) index;
            }
            return (int) (index - snapshotIndex);
        }
    }


    private static class ReplicatedLogImplEntry implements ReplicatedLogEntry,
        Serializable {

        private final long index;
        private final long term;
        private final Object payload;

        public ReplicatedLogImplEntry(long index, long term, Object payload) {

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

        @Override public String toString() {
            return "Entry{" +
                "index=" + index +
                ", term=" + term +
                '}';
        }
    }

    private static class DeleteEntries implements Serializable {
        private final int fromIndex;


        public DeleteEntries(int fromIndex) {
            this.fromIndex = fromIndex;
        }

        public int getFromIndex() {
            return fromIndex;
        }
    }


    private static class Snapshot implements Serializable {
        private final Object state;
        private final List<ReplicatedLogEntry> unAppliedEntries;
        private final long lastIndex;
        private final long lastTerm;
        private final long lastAppliedIndex;
        private final long lastAppliedTerm;

        private Snapshot(Object state,
            List<ReplicatedLogEntry> unAppliedEntries, long lastIndex,
            long lastTerm, long lastAppliedIndex, long lastAppliedTerm) {
            this.state = state;
            this.unAppliedEntries = unAppliedEntries;
            this.lastIndex = lastIndex;
            this.lastTerm = lastTerm;
            this.lastAppliedIndex = lastAppliedIndex;
            this.lastAppliedTerm = lastAppliedTerm;
        }


        public static Snapshot create(Object state,
            List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm) {
            return new Snapshot(state, entries, lastIndex, lastTerm,
                lastAppliedIndex, lastAppliedTerm);
        }

        public Object getState() {
            return state;
        }

        public List<ReplicatedLogEntry> getUnAppliedEntries() {
            return unAppliedEntries;
        }

        public long getLastTerm() {
            return lastTerm;
        }

        public long getLastAppliedIndex() {
            return lastAppliedIndex;
        }

        public long getLastAppliedTerm() {
            return lastAppliedTerm;
        }
    }

    private class ElectionTermImpl implements ElectionTerm {
        /**
         * Identifier of the actor whose election term information this is
         */
        private long currentTerm = 0;
        private String votedFor = null;

        public long getCurrentTerm() {
            return currentTerm;
        }

        public String getVotedFor() {
            return votedFor;
        }

        @Override public void update(long currentTerm, String votedFor) {
            LOG.info("Set currentTerm={}, votedFor={}", currentTerm, votedFor);

            this.currentTerm = currentTerm;
            this.votedFor = votedFor;
        }

        @Override
        public void updateAndPersist(long currentTerm, String votedFor){
            update(currentTerm, votedFor);
            // FIXME : Maybe first persist then update the state
            persist(new UpdateElectionTerm(this.currentTerm, this.votedFor), new Procedure<UpdateElectionTerm>(){

                @Override public void apply(UpdateElectionTerm param)
                    throws Exception {

                }
            });
        }
    }

    private static class UpdateElectionTerm implements Serializable {
        private final long currentTerm;
        private final String votedFor;

        public UpdateElectionTerm(long currentTerm, String votedFor) {
            this.currentTerm = currentTerm;
            this.votedFor = votedFor;
        }

        public long getCurrentTerm() {
            return currentTerm;
        }

        public String getVotedFor() {
            return votedFor;
        }
    }

}
