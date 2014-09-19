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
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.Candidate;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.AddRaftPeer;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.client.messages.RemoveRaftPeer;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;

import java.io.Serializable;
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
    protected RaftActorContext context;

    /**
     * The in-memory journal
     */
    private ReplicatedLogImpl replicatedLog = new ReplicatedLogImpl();

    private CaptureSnapshot captureSnapshot = null;

    private volatile boolean hasSnapshotCaptureInitiated = false;

    public RaftActor(String id, Map<String, String> peerAddresses) {
        this(id, peerAddresses, Optional.<ConfigParams>absent());
    }

    public RaftActor(String id, Map<String, String> peerAddresses,
         Optional<ConfigParams> configParams) {

        context = new RaftActorContextImpl(this.getSelf(),
            this.getContext(), id, new ElectionTermImpl(),
            -1, -1, replicatedLog, peerAddresses,
            (configParams.isPresent() ? configParams.get(): new DefaultConfigParamsImpl()),
            LOG);
    }

    @Override public void onReceiveRecover(Object message) {
        if (message instanceof SnapshotOffer) {
            LOG.info("SnapshotOffer called..");
            SnapshotOffer offer = (SnapshotOffer) message;
            Snapshot snapshot = (Snapshot) offer.snapshot();

            // Create a replicated log with the snapshot information
            // The replicated log can be used later on to retrieve this snapshot
            // when we need to install it on a peer
            replicatedLog = new ReplicatedLogImpl(snapshot);

            context.setReplicatedLog(replicatedLog);
            context.setLastApplied(snapshot.getLastAppliedIndex());
            context.setCommitIndex(snapshot.getLastAppliedIndex());

            LOG.info("Applied snapshot to replicatedLog. " +
                    "snapshotIndex={}, snapshotTerm={}, journal-size={}",
                replicatedLog.snapshotIndex, replicatedLog.snapshotTerm,
                replicatedLog.size()
            );

            // Apply the snapshot to the actors state
            applySnapshot(ByteString.copyFrom(snapshot.getState()));

        } else if (message instanceof ReplicatedLogEntry) {
            ReplicatedLogEntry logEntry = (ReplicatedLogEntry) message;
            if(LOG.isDebugEnabled()) {
                LOG.debug("Received ReplicatedLogEntry for recovery:{}", logEntry.getIndex());
            }
            replicatedLog.append(logEntry);

        } else if (message instanceof ApplyLogEntries) {
            ApplyLogEntries ale = (ApplyLogEntries) message;

            if(LOG.isDebugEnabled()) {
                LOG.debug("Received ApplyLogEntries for recovery, applying to state:{} to {}",
                    context.getLastApplied() + 1, ale.getToIndex());
            }

            for (long i = context.getLastApplied() + 1; i <= ale.getToIndex(); i++) {
                applyState(null, "recovery", replicatedLog.get(i).getData());
            }
            context.setLastApplied(ale.getToIndex());
            context.setCommitIndex(ale.getToIndex());

        } else if (message instanceof DeleteEntries) {
            replicatedLog.removeFrom(((DeleteEntries) message).getFromIndex());

        } else if (message instanceof UpdateElectionTerm) {
            context.getTermInformation().update(((UpdateElectionTerm) message).getCurrentTerm(),
                ((UpdateElectionTerm) message).getVotedFor());

        } else if (message instanceof RecoveryCompleted) {
            LOG.info(
                "RecoveryCompleted - Switching actor to Follower - " +
                    "Persistence Id =  " + persistenceId() +
                    " Last index in log:{}, snapshotIndex={}, snapshotTerm={}, " +
                    "journal-size={}",
                replicatedLog.lastIndex(), replicatedLog.snapshotIndex,
                replicatedLog.snapshotTerm, replicatedLog.size());
            currentBehavior = switchBehavior(RaftState.Follower);
            onStateChanged();
        }
    }

    @Override public void onReceiveCommand(Object message) {
        if (message instanceof ApplyState){
            ApplyState applyState = (ApplyState) message;

            if(LOG.isDebugEnabled()) {
                LOG.debug("Applying state for log index {} data {}",
                    applyState.getReplicatedLogEntry().getIndex(),
                    applyState.getReplicatedLogEntry().getData());
            }

            applyState(applyState.getClientActor(), applyState.getIdentifier(),
                applyState.getReplicatedLogEntry().getData());

        } else if (message instanceof ApplyLogEntries){
            ApplyLogEntries ale = (ApplyLogEntries) message;
            if(LOG.isDebugEnabled()) {
                LOG.debug("Persisting ApplyLogEntries with index={}", ale.getToIndex());
            }
            persist(new ApplyLogEntries(ale.getToIndex()), new Procedure<ApplyLogEntries>() {
                @Override
                public void apply(ApplyLogEntries param) throws Exception {
                }
            });

        } else if(message instanceof ApplySnapshot ) {
            Snapshot snapshot = ((ApplySnapshot) message).getSnapshot();

            if(LOG.isDebugEnabled()) {
                LOG.debug("ApplySnapshot called on Follower Actor " +
                        "snapshotIndex:{}, snapshotTerm:{}", snapshot.getLastAppliedIndex(),
                    snapshot.getLastAppliedTerm()
                );
            }
            applySnapshot(ByteString.copyFrom(snapshot.getState()));

            //clears the followers log, sets the snapshot index to ensure adjusted-index works
            replicatedLog = new ReplicatedLogImpl(snapshot);
            context.setReplicatedLog(replicatedLog);
            context.setLastApplied(snapshot.getLastAppliedIndex());

        } else if (message instanceof FindLeader) {
            getSender().tell(
                new FindLeaderReply(getLeaderAddress()),
                getSelf()
            );

        } else if (message instanceof SaveSnapshotSuccess) {
            SaveSnapshotSuccess success = (SaveSnapshotSuccess) message;
            LOG.info("SaveSnapshotSuccess received for snapshot");

            context.getReplicatedLog().snapshotCommit();

            // TODO: Not sure if we want to be this aggressive with trimming stuff
            trimPersistentData(success.metadata().sequenceNr());

        } else if (message instanceof SaveSnapshotFailure) {
            SaveSnapshotFailure saveSnapshotFailure = (SaveSnapshotFailure) message;

            LOG.info("saveSnapshotFailure.metadata():{}", saveSnapshotFailure.metadata().toString());
            LOG.error(saveSnapshotFailure.cause(), "SaveSnapshotFailure received for snapshot Cause:");

            context.getReplicatedLog().snapshotRollback();

            LOG.info("Replicated Log rollbacked. Snapshot will be attempted in the next cycle." +
                "snapshotIndex:{}, snapshotTerm:{}, log-size:{}",
                context.getReplicatedLog().getSnapshotIndex(),
                context.getReplicatedLog().getSnapshotTerm(),
                context.getReplicatedLog().size());

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

        } else if (message instanceof CaptureSnapshot) {
            LOG.info("CaptureSnapshot received by actor");
            CaptureSnapshot cs = (CaptureSnapshot)message;
            captureSnapshot = cs;
            createSnapshot();

        } else if (message instanceof CaptureSnapshotReply){
            LOG.info("CaptureSnapshotReply received by actor");
            CaptureSnapshotReply csr = (CaptureSnapshotReply) message;

            ByteString stateInBytes = csr.getSnapshot();
            LOG.info("CaptureSnapshotReply stateInBytes size:{}", stateInBytes.size());
            handleCaptureSnapshotReply(stateInBytes);

        } else {
            if (!(message instanceof AppendEntriesMessages.AppendEntries)
                && !(message instanceof AppendEntriesReply) && !(message instanceof SendHeartBeat)) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("onReceiveCommand: message:" + message.getClass());
                }
            }

            RaftState state =
                currentBehavior.handleMessage(getSender(), message);
            RaftActorBehavior oldBehavior = currentBehavior;
            currentBehavior = switchBehavior(state);
            if(oldBehavior != currentBehavior){
                onStateChanged();
            }

            onLeaderChanged(oldBehavior.getLeaderId(), currentBehavior.getLeaderId());
        }
    }

    public java.util.Set<String> getPeers() {
        return context.getPeerAddresses().keySet();
    }

    protected String getReplicatedLogState() {
        return "snapshotIndex=" + context.getReplicatedLog().getSnapshotIndex()
            + ", snapshotTerm=" + context.getReplicatedLog().getSnapshotTerm()
            + ", im-mem journal size=" + context.getReplicatedLog().size();
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
        Payload data) {

        ReplicatedLogEntry replicatedLogEntry = new ReplicatedLogImplEntry(
            context.getReplicatedLog().lastIndex() + 1,
            context.getTermInformation().getCurrentTerm(), data);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Persist data {}", replicatedLogEntry);
        }

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
        String leaderAddress = getLeaderAddress();

        if(leaderAddress == null){
            return null;
        }

        return context.actorSelection(leaderAddress);
    }

    /**
     *
     * @return the current leader's id
     */
    protected String getLeaderId(){
        return currentBehavior.getLeaderId();
    }

    protected RaftState getRaftState() {
        return currentBehavior.state();
    }

    protected ReplicatedLogEntry getLastLogEntry() {
        return replicatedLog.last();
    }

    protected Long getCurrentTerm(){
        return context.getTermInformation().getCurrentTerm();
    }

    protected Long getCommitIndex(){
        return context.getCommitIndex();
    }

    protected Long getLastApplied(){
        return context.getLastApplied();
    }

    /**
     * setPeerAddress sets the address of a known peer at a later time.
     * <p>
     * This is to account for situations where a we know that a peer
     * exists but we do not know an address up-front. This may also be used in
     * situations where a known peer starts off in a different location and we
     * need to change it's address
     * <p>
     * Note that if the peerId does not match the list of peers passed to
     * this actor during construction an IllegalStateException will be thrown.
     *
     * @param peerId
     * @param peerAddress
     */
    protected void setPeerAddress(String peerId, String peerAddress){
        context.setPeerAddress(peerId, peerAddress);
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
    protected abstract void createSnapshot();

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
    protected abstract void applySnapshot(ByteString snapshot);

    /**
     * This method will be called by the RaftActor when the state of the
     * RaftActor changes. The derived actor can then use methods like
     * isLeader or getLeader to do something useful
     */
    protected abstract void onStateChanged();

    protected void onLeaderChanged(String oldLeader, String newLeader){};

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
        // Trim akka snapshots
        // FIXME : Not sure how exactly the SnapshotSelectionCriteria is applied
        // For now guessing that it is ANDed.
        deleteSnapshots(new SnapshotSelectionCriteria(
            sequenceNumber - context.getConfigParams().getSnapshotBatchCount(), 43200000));

        // Trim akka journal
        deleteMessages(sequenceNumber);
    }

    private String getLeaderAddress(){
        if(isLeader()){
            return getSelf().path().toString();
        }
        String leaderId = currentBehavior.getLeaderId();
        if (leaderId == null) {
            return null;
        }
        String peerAddress = context.getPeerAddress(leaderId);
        if(LOG.isDebugEnabled()) {
            LOG.debug("getLeaderAddress leaderId = " + leaderId + " peerAddress = "
                + peerAddress);
        }

        return peerAddress;
    }

    private void handleCaptureSnapshotReply(ByteString stateInBytes) {
        // create a snapshot object from the state provided and save it
        // when snapshot is saved async, SaveSnapshotSuccess is raised.

        Snapshot sn = Snapshot.create(stateInBytes.toByteArray(),
            context.getReplicatedLog().getFrom(captureSnapshot.getLastAppliedIndex() + 1),
            captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
            captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm());

        saveSnapshot(sn);

        LOG.info("Persisting of snapshot done:{}", sn.getLogMessage());

        //be greedy and remove entries from in-mem journal which are in the snapshot
        // and update snapshotIndex and snapshotTerm without waiting for the success,

        context.getReplicatedLog().snapshotPreCommit(stateInBytes,
            captureSnapshot.getLastAppliedIndex(),
            captureSnapshot.getLastAppliedTerm());

        LOG.info("Removed in-memory snapshotted entries, adjusted snaphsotIndex:{} " +
            "and term:{}", captureSnapshot.getLastAppliedIndex(),
            captureSnapshot.getLastAppliedTerm());

        captureSnapshot = null;
        hasSnapshotCaptureInitiated = false;
    }


    private class ReplicatedLogImpl extends AbstractReplicatedLogImpl {

        public ReplicatedLogImpl(Snapshot snapshot) {
            super(ByteString.copyFrom(snapshot.getState()),
                snapshot.getLastAppliedIndex(), snapshot.getLastAppliedTerm(),
                snapshot.getUnAppliedEntries());
        }

        public ReplicatedLogImpl() {
            super();
        }

        @Override public void removeFromAndPersist(long logEntryIndex) {
            int adjustedIndex = adjustedIndex(logEntryIndex);

            if (adjustedIndex < 0) {
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

        @Override public void appendAndPersist(
            final ReplicatedLogEntry replicatedLogEntry) {
            appendAndPersist(null, null, replicatedLogEntry);
        }

        public void appendAndPersist(final ActorRef clientActor,
            final String identifier,
            final ReplicatedLogEntry replicatedLogEntry) {
            context.getLogger().debug(
                "Append log entry and persist {} ", replicatedLogEntry);
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
                        // when a snaphsot is being taken, captureSnapshot != null
                        if (hasSnapshotCaptureInitiated == false &&
                            journal.size() % context.getConfigParams().getSnapshotBatchCount() == 0) {

                            LOG.info("Initiating Snapshot Capture..");
                            long lastAppliedIndex = -1;
                            long lastAppliedTerm = -1;

                            ReplicatedLogEntry lastAppliedEntry = get(context.getLastApplied());
                            if (lastAppliedEntry != null) {
                                lastAppliedIndex = lastAppliedEntry.getIndex();
                                lastAppliedTerm = lastAppliedEntry.getTerm();
                            }

                            if(LOG.isDebugEnabled()) {
                                LOG.debug("Snapshot Capture logSize: {}", journal.size());
                                LOG.debug("Snapshot Capture lastApplied:{} ",
                                    context.getLastApplied());
                                LOG.debug("Snapshot Capture lastAppliedIndex:{}", lastAppliedIndex);
                                LOG.debug("Snapshot Capture lastAppliedTerm:{}", lastAppliedTerm);
                            }

                            // send a CaptureSnapshot to self to make the expensive operation async.
                            getSelf().tell(new CaptureSnapshot(
                                lastIndex(), lastTerm(), lastAppliedIndex, lastAppliedTerm),
                                null);
                            hasSnapshotCaptureInitiated = true;
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
            if(LOG.isDebugEnabled()) {
                LOG.debug("Set currentTerm={}, votedFor={}", currentTerm, votedFor);
            }
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
