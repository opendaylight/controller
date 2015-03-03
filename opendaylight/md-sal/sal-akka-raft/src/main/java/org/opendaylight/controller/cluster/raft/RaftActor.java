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
import akka.japi.Procedure;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractRaftActorBehavior;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class RaftActor extends AbstractUntypedPersistentActor {

    private static final long APPLY_STATE_DELAY_THRESHOLD_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(50L); // 50 millis

    private static final String COMMIT_SNAPSHOT = "commit_snapshot";

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * The current state determines the current behavior of a RaftActor
     * A Raft Actor always starts off in the Follower State
     */
    private RaftActorBehavior currentBehavior;

    /**
     * This context should NOT be passed directly to any other actor it is
     * only to be consumed by the RaftActorBehaviors
     */
    private final RaftActorContext context;

    private final Procedure<Void> createSnapshotProcedure = new CreateSnapshotProcedure();

    /**
     * The in-memory journal
     */
    private ReplicatedLogImpl replicatedLog = new ReplicatedLogImpl();

    private Stopwatch recoveryTimer;

    private int currentRecoveryBatchCount;

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

    private void initRecoveryTimer() {
        if(recoveryTimer == null) {
            recoveryTimer = Stopwatch.createStarted();
        }
    }

    @Override
    public void preStart() throws Exception {
        LOG.info("Starting recovery for {} with journal batch size {}", persistenceId(),
                context.getConfigParams().getJournalRecoveryLogBatchSize());

        super.preStart();
    }

    @Override
    public void handleRecover(Object message) {
        if(persistence().isRecoveryApplicable()) {
            if (message instanceof SnapshotOffer) {
                onRecoveredSnapshot((SnapshotOffer) message);
            } else if (message instanceof ReplicatedLogEntry) {
                onRecoveredJournalLogEntry((ReplicatedLogEntry) message);
            } else if (message instanceof ApplyLogEntries) {
                onRecoveredApplyLogEntries((ApplyLogEntries) message);
            } else if (message instanceof DeleteEntries) {
                replicatedLog.removeFrom(((DeleteEntries) message).getFromIndex());
            } else if (message instanceof UpdateElectionTerm) {
                context.getTermInformation().update(((UpdateElectionTerm) message).getCurrentTerm(),
                        ((UpdateElectionTerm) message).getVotedFor());
            } else if (message instanceof RecoveryCompleted) {
                onRecoveryCompletedMessage();
            }
        } else {
            if (message instanceof RecoveryCompleted) {
                // Delete all the messages from the akka journal so that we do not end up with consistency issues
                // Note I am not using the dataPersistenceProvider and directly using the akka api here
                deleteMessages(lastSequenceNr());

                // Delete all the akka snapshots as they will not be needed
                deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(), scala.Long.MaxValue()));

                onRecoveryComplete();

                initializeBehavior();
            }
        }
    }

    private void onRecoveredSnapshot(SnapshotOffer offer) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: SnapshotOffer called..", persistenceId());
        }

        initRecoveryTimer();

        Snapshot snapshot = (Snapshot) offer.snapshot();

        // Create a replicated log with the snapshot information
        // The replicated log can be used later on to retrieve this snapshot
        // when we need to install it on a peer
        replicatedLog = new ReplicatedLogImpl(snapshot);

        context.setReplicatedLog(replicatedLog);
        context.setLastApplied(snapshot.getLastAppliedIndex());
        context.setCommitIndex(snapshot.getLastAppliedIndex());

        Stopwatch timer = Stopwatch.createStarted();

        // Apply the snapshot to the actors state
        applyRecoverySnapshot(snapshot.getState());

        timer.stop();
        LOG.info("Recovery snapshot applied for {} in {}: snapshotIndex={}, snapshotTerm={}, journal-size=" +
                replicatedLog.size(), persistenceId(), timer.toString(),
                replicatedLog.getSnapshotIndex(), replicatedLog.getSnapshotTerm());
    }

    private void onRecoveredJournalLogEntry(ReplicatedLogEntry logEntry) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Received ReplicatedLogEntry for recovery: {}", persistenceId(), logEntry.getIndex());
        }

        replicatedLog.append(logEntry);
    }

    private void onRecoveredApplyLogEntries(ApplyLogEntries ale) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Received ApplyLogEntries for recovery, applying to state: {} to {}",
                    persistenceId(), context.getLastApplied() + 1, ale.getToIndex());
        }

        for (long i = context.getLastApplied() + 1; i <= ale.getToIndex(); i++) {
            batchRecoveredLogEntry(replicatedLog.get(i));
        }

        context.setLastApplied(ale.getToIndex());
        context.setCommitIndex(ale.getToIndex());
    }

    private void batchRecoveredLogEntry(ReplicatedLogEntry logEntry) {
        initRecoveryTimer();

        int batchSize = context.getConfigParams().getJournalRecoveryLogBatchSize();
        if(currentRecoveryBatchCount == 0) {
            startLogRecoveryBatch(batchSize);
        }

        appendRecoveredLogEntry(logEntry.getData());

        if(++currentRecoveryBatchCount >= batchSize) {
            endCurrentLogRecoveryBatch();
        }
    }

    private void endCurrentLogRecoveryBatch() {
        applyCurrentLogRecoveryBatch();
        currentRecoveryBatchCount = 0;
    }

    private void onRecoveryCompletedMessage() {
        if(currentRecoveryBatchCount > 0) {
            endCurrentLogRecoveryBatch();
        }

        onRecoveryComplete();

        String recoveryTime = "";
        if(recoveryTimer != null) {
            recoveryTimer.stop();
            recoveryTime = " in " + recoveryTimer.toString();
            recoveryTimer = null;
        }

        LOG.info(
            "Recovery completed" + recoveryTime + " - Switching actor to Follower - " +
                "Persistence Id =  " + persistenceId() +
                " Last index in log={}, snapshotIndex={}, snapshotTerm={}, " +
                "journal-size={}",
            replicatedLog.lastIndex(), replicatedLog.getSnapshotIndex(),
            replicatedLog.getSnapshotTerm(), replicatedLog.size());

        initializeBehavior();
    }

    protected void initializeBehavior(){
        changeCurrentBehavior(new Follower(context));
    }

    protected void changeCurrentBehavior(RaftActorBehavior newBehavior){
        RaftActorBehavior oldBehavior = currentBehavior;
        currentBehavior = newBehavior;
        handleBehaviorChange(oldBehavior, currentBehavior);
    }

    @Override public void handleCommand(Object message) {
        if (message instanceof ApplyState){
            ApplyState applyState = (ApplyState) message;

            long elapsedTime = (System.nanoTime() - applyState.getStartTime());
            if(elapsedTime >= APPLY_STATE_DELAY_THRESHOLD_IN_NANOS){
                LOG.warn("ApplyState took more time than expected. Elapsed Time = {} ms ApplyState = {}",
                        TimeUnit.NANOSECONDS.toMillis(elapsedTime), applyState);
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Applying state for log index {} data {}",
                    persistenceId(), applyState.getReplicatedLogEntry().getIndex(),
                    applyState.getReplicatedLogEntry().getData());
            }

            applyState(applyState.getClientActor(), applyState.getIdentifier(),
                applyState.getReplicatedLogEntry().getData());

        } else if (message instanceof ApplyLogEntries){
            ApplyLogEntries ale = (ApplyLogEntries) message;
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Persisting ApplyLogEntries with index={}", persistenceId(), ale.getToIndex());
            }
            persistence().persist(new ApplyLogEntries(ale.getToIndex()), new Procedure<ApplyLogEntries>() {
                @Override
                public void apply(ApplyLogEntries param) throws Exception {
                }
            });

        } else if(message instanceof ApplySnapshot ) {
            Snapshot snapshot = ((ApplySnapshot) message).getSnapshot();

            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: ApplySnapshot called on Follower Actor " +
                        "snapshotIndex:{}, snapshotTerm:{}", persistenceId(), snapshot.getLastAppliedIndex(),
                    snapshot.getLastAppliedTerm()
                );
            }

            applySnapshot(snapshot.getState());

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
            LOG.info("{}: SaveSnapshotSuccess received for snapshot", persistenceId());

            long sequenceNumber = success.metadata().sequenceNr();

            commitSnapshot(sequenceNumber);

        } else if (message instanceof SaveSnapshotFailure) {
            SaveSnapshotFailure saveSnapshotFailure = (SaveSnapshotFailure) message;

            LOG.error("{}: SaveSnapshotFailure received for snapshot Cause:",
                    persistenceId(), saveSnapshotFailure.cause());

            context.getSnapshotManager().rollback();

        } else if (message instanceof CaptureSnapshot) {
            LOG.info("{}: CaptureSnapshot received by actor", persistenceId());

            context.getSnapshotManager().create(createSnapshotProcedure);

        } else if (message instanceof CaptureSnapshotReply) {
            handleCaptureSnapshotReply(((CaptureSnapshotReply) message).getSnapshot());

        } else if (message.equals(COMMIT_SNAPSHOT)) {
            commitSnapshot(-1);

        } else {
            RaftActorBehavior oldBehavior = currentBehavior;
            currentBehavior = currentBehavior.handleMessage(getSender(), message);

            handleBehaviorChange(oldBehavior, currentBehavior);
        }
    }

    private void handleBehaviorChange(RaftActorBehavior oldBehavior, RaftActorBehavior currentBehavior) {
        if (oldBehavior != currentBehavior){
            onStateChanged();
        }

        String oldBehaviorLeaderId = oldBehavior == null? null : oldBehavior.getLeaderId();
        String oldBehaviorState = oldBehavior == null? null : oldBehavior.state().name();

        // it can happen that the state has not changed but the leader has changed.
        onLeaderChanged(oldBehaviorLeaderId, currentBehavior.getLeaderId());

        if (getRoleChangeNotifier().isPresent() &&
                (oldBehavior == null || (oldBehavior.state() != currentBehavior.state()))) {
            getRoleChangeNotifier().get().tell(
                    new RoleChanged(getId(), oldBehaviorState , currentBehavior.state().name()),
                    getSelf());
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
    protected void persistData(final ActorRef clientActor, final String identifier,
        final Payload data) {

        ReplicatedLogEntry replicatedLogEntry = new ReplicatedLogImplEntry(
            context.getReplicatedLog().lastIndex() + 1,
            context.getTermInformation().getCurrentTerm(), data);

        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Persist data {}", persistenceId(), replicatedLogEntry);
        }

        final RaftActorContext raftContext = getRaftActorContext();

        replicatedLog
                .appendAndPersist(replicatedLogEntry, new Procedure<ReplicatedLogEntry>() {
                    @Override
                    public void apply(ReplicatedLogEntry replicatedLogEntry) throws Exception {
                        if(!hasFollowers()){
                            // Increment the Commit Index and the Last Applied values
                            raftContext.setCommitIndex(replicatedLogEntry.getIndex());
                            raftContext.setLastApplied(replicatedLogEntry.getIndex());

                            // Apply the state immediately
                            applyState(clientActor, identifier, data);

                            // Send a ApplyLogEntries message so that we write the fact that we applied
                            // the state to durable storage
                            self().tell(new ApplyLogEntries((int) replicatedLogEntry.getIndex()), self());

                            context.getSnapshotManager().trimLog(context.getLastApplied());

                        } else if (clientActor != null) {
                            // Send message for replication
                            currentBehavior.handleMessage(getSelf(),
                                    new Replicate(clientActor, identifier,
                                            replicatedLogEntry)
                            );
                        }

                    }
                });    }

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

    protected RaftActorContext getRaftActorContext() {
        return context;
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

    protected void commitSnapshot(long sequenceNumber) {
        context.getSnapshotManager().commit(persistence(), sequenceNumber);
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
     * This method is called during recovery at the start of a batch of state entries. Derived
     * classes should perform any initialization needed to start a batch.
     */
    protected abstract void startLogRecoveryBatch(int maxBatchSize);

    /**
     * This method is called during recovery to append state data to the current batch. This method
     * is called 1 or more times after {@link #startLogRecoveryBatch}.
     *
     * @param data the state data
     */
    protected abstract void appendRecoveredLogEntry(Payload data);

    /**
     * This method is called during recovery to reconstruct the state of the actor.
     *
     * @param snapshotBytes A snapshot of the state of the actor
     */
    protected abstract void applyRecoverySnapshot(byte[] snapshotBytes);

    /**
     * This method is called during recovery at the end of a batch to apply the current batched
     * log entries. This method is called after {@link #appendRecoveredLogEntry}.
     */
    protected abstract void applyCurrentLogRecoveryBatch();

    /**
     * This method is called when recovery is complete.
     */
    protected abstract void onRecoveryComplete();

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
     * This method can be called at any other point during normal
     * operations when the derived actor is out of sync with it's peers
     * and the only way to bring it in sync is by applying a snapshot
     *
     * @param snapshotBytes A snapshot of the state of the actor
     */
    protected abstract void applySnapshot(byte[] snapshotBytes);

    /**
     * This method will be called by the RaftActor when the state of the
     * RaftActor changes. The derived actor can then use methods like
     * isLeader or getLeader to do something useful
     */
    protected abstract void onStateChanged();

    protected abstract DataPersistenceProvider persistence();

    /**
     * Notifier Actor for this RaftActor to notify when a role change happens
     * @return ActorRef - ActorRef of the notifier or Optional.absent if none.
     */
    protected abstract Optional<ActorRef> getRoleChangeNotifier();

    protected void onLeaderChanged(String oldLeader, String newLeader){};

    private void trimPersistentData(long sequenceNumber) {
        // Trim akka snapshots
        // FIXME : Not sure how exactly the SnapshotSelectionCriteria is applied
        // For now guessing that it is ANDed.
        persistence().deleteSnapshots(new SnapshotSelectionCriteria(
            sequenceNumber - context.getConfigParams().getSnapshotBatchCount(), 43200000));

        // Trim akka journal
        persistence().deleteMessages(sequenceNumber);
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
            LOG.debug("{}: getLeaderAddress leaderId = {} peerAddress = {}",
                    persistenceId(), leaderId, peerAddress);
        }

        return peerAddress;
    }

    private void handleCaptureSnapshotReply(byte[] snapshotBytes) {
        LOG.info("{}: CaptureSnapshotReply received by actor: snapshot size {}", persistenceId(), snapshotBytes.length);
        context.getSnapshotManager().persist(persistence(), snapshotBytes, currentBehavior);
    }

    protected boolean hasFollowers(){
        return getRaftActorContext().getPeerAddresses().keySet().size() > 0;
    }

    private class ReplicatedLogImpl extends AbstractReplicatedLogImpl {
        private static final int DATA_SIZE_DIVIDER = 5;
        private long dataSizeSinceLastSnapshot = 0L;


        public ReplicatedLogImpl(Snapshot snapshot) {
            super(snapshot.getLastAppliedIndex(), snapshot.getLastAppliedTerm(),
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

            persistence().persist(new DeleteEntries(adjustedIndex), new Procedure<DeleteEntries>() {

                @Override
                public void apply(DeleteEntries param)
                        throws Exception {
                    //FIXME : Doing nothing for now
                    dataSize = 0;
                    for (ReplicatedLogEntry entry : journal) {
                        dataSize += entry.size();
                    }
                }
            });
        }

        @Override public void appendAndPersist(
            final ReplicatedLogEntry replicatedLogEntry) {
            appendAndPersist(replicatedLogEntry, null);
        }

        public void appendAndPersist(
            final ReplicatedLogEntry replicatedLogEntry,
            final Procedure<ReplicatedLogEntry> callback)  {

            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Append log entry and persist {} ", persistenceId(), replicatedLogEntry);
            }

            // FIXME : By adding the replicated log entry to the in-memory journal we are not truly ensuring durability of the logs
            journal.add(replicatedLogEntry);

            // When persisting events with persist it is guaranteed that the
            // persistent actor will not receive further commands between the
            // persist call and the execution(s) of the associated event
            // handler. This also holds for multiple persist calls in context
            // of a single command.
            persistence().persist(replicatedLogEntry,
                new Procedure<ReplicatedLogEntry>() {
                    @Override
                    public void apply(ReplicatedLogEntry evt) throws Exception {
                        int logEntrySize = replicatedLogEntry.size();

                        dataSize += logEntrySize;
                        long dataSizeForCheck = dataSize;

                        dataSizeSinceLastSnapshot += logEntrySize;

                        if (!hasFollowers()) {
                            // When we do not have followers we do not maintain an in-memory log
                            // due to this the journalSize will never become anything close to the
                            // snapshot batch count. In fact will mostly be 1.
                            // Similarly since the journal's dataSize depends on the entries in the
                            // journal the journal's dataSize will never reach a value close to the
                            // memory threshold.
                            // By maintaining the dataSize outside the journal we are tracking essentially
                            // what we have written to the disk however since we no longer are in
                            // need of doing a snapshot just for the sake of freeing up memory we adjust
                            // the real size of data by the DATA_SIZE_DIVIDER so that we do not snapshot as often
                            // as if we were maintaining a real snapshot
                            dataSizeForCheck = dataSizeSinceLastSnapshot / DATA_SIZE_DIVIDER;
                        }
                        long journalSize = replicatedLogEntry.getIndex() + 1;
                        long dataThreshold = Runtime.getRuntime().totalMemory() *
                                context.getConfigParams().getSnapshotDataThresholdPercentage() / 100;

                        if ((journalSize % context.getConfigParams().getSnapshotBatchCount() == 0
                                || dataSizeForCheck > dataThreshold)) {

                            context.getSnapshotManager().capture(replicatedLogEntry,
                                    currentBehavior.getReplicatedToAllIndex());
                        }

                        if (callback != null){
                            callback.apply(replicatedLogEntry);
                        }
                    }
                }
            );
        }

    }

    static class DeleteEntries implements Serializable {
        private static final long serialVersionUID = 1L;
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

        @Override
        public long getCurrentTerm() {
            return currentTerm;
        }

        @Override
        public String getVotedFor() {
            return votedFor;
        }

        @Override public void update(long currentTerm, String votedFor) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Set currentTerm={}, votedFor={}", persistenceId(), currentTerm, votedFor);
            }
            this.currentTerm = currentTerm;
            this.votedFor = votedFor;
        }

        @Override
        public void updateAndPersist(long currentTerm, String votedFor){
            update(currentTerm, votedFor);
            // FIXME : Maybe first persist then update the state
            persistence().persist(new UpdateElectionTerm(this.currentTerm, this.votedFor), new Procedure<UpdateElectionTerm>(){

                @Override public void apply(UpdateElectionTerm param)
                    throws Exception {

                }
            });
        }
    }

    static class UpdateElectionTerm implements Serializable {
        private static final long serialVersionUID = 1L;
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

    protected class NonPersistentRaftDataProvider extends NonPersistentDataProvider {

        public NonPersistentRaftDataProvider(){

        }

        /**
         * The way snapshotting works is,
         * <ol>
         * <li> RaftActor calls createSnapshot on the Shard
         * <li> Shard sends a CaptureSnapshotReply and RaftActor then calls saveSnapshot
         * <li> When saveSnapshot is invoked on the akka-persistence API it uses the SnapshotStore to save the snapshot.
         * The SnapshotStore sends SaveSnapshotSuccess or SaveSnapshotFailure. When the RaftActor gets SaveSnapshot
         * success it commits the snapshot to the in-memory journal. This commitSnapshot is mimicking what is done
         * in SaveSnapshotSuccess.
         * </ol>
         * @param o
         */
        @Override
        public void saveSnapshot(Object o) {
            // Make saving Snapshot successful
            // Committing the snapshot here would end up calling commit in the creating state which would
            // be a state violation. That's why now we send a message to commit the snapshot.
            self().tell(COMMIT_SNAPSHOT, self());
        }
    }


    private class CreateSnapshotProcedure implements Procedure<Void> {

        @Override
        public void apply(Void aVoid) throws Exception {
            createSnapshot();
        }
    }

    @VisibleForTesting
    void setCurrentBehavior(AbstractRaftActorBehavior behavior) {
        currentBehavior = behavior;
    }

    protected RaftActorBehavior getCurrentBehavior() {
        return currentBehavior;
    }

}
