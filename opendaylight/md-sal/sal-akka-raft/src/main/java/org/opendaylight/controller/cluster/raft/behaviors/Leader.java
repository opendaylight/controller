/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.ClientRequestTrackerImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.FollowerLogInformationImpl;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The behavior of a RaftActor when it is in the Leader state
 * <p/>
 * Leaders:
 * <ul>
 * <li> Upon election: send initial empty AppendEntries RPCs
 * (heartbeat) to each server; repeat during idle periods to
 * prevent election timeouts (§5.2)
 * <li> If command received from client: append entry to local log,
 * respond after entry applied to state machine (§5.3)
 * <li> If last log index ≥ nextIndex for a follower: send
 * AppendEntries RPC with log entries starting at nextIndex
 * <ul>
 * <li> If successful: update nextIndex and matchIndex for
 * follower (§5.3)
 * <li> If AppendEntries fails because of log inconsistency:
 * decrement nextIndex and retry (§5.3)
 * </ul>
 * <li> If there exists an N such that N > commitIndex, a majority
 * of matchIndex[i] ≥ N, and log[N].term == currentTerm:
 * set commitIndex = N (§5.3, §5.4).
 */
public class Leader extends AbstractRaftActorBehavior {


    protected final Map<String, FollowerLogInformation> followerToLog =
        new HashMap();
    protected final Map<String, FollowerToSnapshot> mapFollowerToSnapshot = new HashMap<>();

    private final Set<String> followers;

    private Cancellable heartbeatSchedule = null;
    private Cancellable installSnapshotSchedule = null;

    private List<ClientRequestTracker> trackerList = new ArrayList<>();

    private final int minReplicationCount;

    public Leader(RaftActorContext context) {
        super(context);

        followers = context.getPeerAddresses().keySet();

        for (String followerId : followers) {
            FollowerLogInformation followerLogInformation =
                new FollowerLogInformationImpl(followerId,
                    new AtomicLong(context.getCommitIndex()),
                    new AtomicLong(-1));

            followerToLog.put(followerId, followerLogInformation);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Election:Leader has following peers: {}", followers);
        }

        if (followers.size() > 0) {
            minReplicationCount = (followers.size() + 1) / 2 + 1;
        } else {
            minReplicationCount = 0;
        }


        // Immediately schedule a heartbeat
        // Upon election: send initial empty AppendEntries RPCs
        // (heartbeat) to each server; repeat during idle periods to
        // prevent election timeouts (§5.2)
        scheduleHeartBeat(new FiniteDuration(0, TimeUnit.SECONDS));

        scheduleInstallSnapshotCheck(
            new FiniteDuration(context.getConfigParams().getHeartBeatInterval().length() * 1000,
                context.getConfigParams().getHeartBeatInterval().unit())
        );

    }

    @Override protected RaftActorBehavior handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        if(LOG.isDebugEnabled()) {
            LOG.debug(appendEntries.toString());
        }

        return this;
    }

    @Override protected RaftActorBehavior handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply) {

        if(! appendEntriesReply.isSuccess()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug(appendEntriesReply.toString());
            }
        }

        // Update the FollowerLogInformation
        String followerId = appendEntriesReply.getFollowerId();
        FollowerLogInformation followerLogInformation =
            followerToLog.get(followerId);

        if(followerLogInformation == null){
            LOG.error("Unknown follower {}", followerId);
            return this;
        }

        if (appendEntriesReply.isSuccess()) {
            followerLogInformation
                .setMatchIndex(appendEntriesReply.getLogLastIndex());
            followerLogInformation
                .setNextIndex(appendEntriesReply.getLogLastIndex() + 1);
        } else {

            // TODO: When we find that the follower is out of sync with the
            // Leader we simply decrement that followers next index by 1.
            // Would it be possible to do better than this? The RAFT spec
            // does not explicitly deal with it but may be something for us to
            // think about

            followerLogInformation.decrNextIndex();
        }

        // Now figure out if this reply warrants a change in the commitIndex
        // If there exists an N such that N > commitIndex, a majority
        // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
        // set commitIndex = N (§5.3, §5.4).
        for (long N = context.getCommitIndex() + 1; ; N++) {
            int replicatedCount = 1;

            for (FollowerLogInformation info : followerToLog.values()) {
                if (info.getMatchIndex().get() >= N) {
                    replicatedCount++;
                }
            }

            if (replicatedCount >= minReplicationCount) {
                ReplicatedLogEntry replicatedLogEntry =
                    context.getReplicatedLog().get(N);
                if (replicatedLogEntry != null
                    && replicatedLogEntry.getTerm()
                    == currentTerm()) {
                    context.setCommitIndex(N);
                }
            } else {
                break;
            }
        }

        // Apply the change to the state machine
        if (context.getCommitIndex() > context.getLastApplied()) {
            applyLogToStateMachine(context.getCommitIndex());
        }

        return this;
    }

    protected ClientRequestTracker removeClientRequestTracker(long logIndex) {

        ClientRequestTracker toRemove = findClientRequestTracker(logIndex);
        if(toRemove != null) {
            trackerList.remove(toRemove);
        }

        return toRemove;
    }

    protected ClientRequestTracker findClientRequestTracker(long logIndex) {
        for (ClientRequestTracker tracker : trackerList) {
            if (tracker.getIndex() == logIndex) {
                return tracker;
            }
        }

        return null;
    }

    @Override protected RaftActorBehavior handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply) {
        return this;
    }

    @Override public RaftState state() {
        return RaftState.Leader;
    }

    @Override public RaftActorBehavior handleMessage(ActorRef sender, Object originalMessage) {
        Preconditions.checkNotNull(sender, "sender should not be null");

        Object message = fromSerializableMessage(originalMessage);

        if (message instanceof RaftRPC) {
            RaftRPC rpc = (RaftRPC) message;
            // If RPC request or response contains term T > currentTerm:
            // set currentTerm = T, convert to follower (§5.1)
            // This applies to all RPC messages and responses
            if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
                context.getTermInformation().updateAndPersist(rpc.getTerm(), null);

                return switchBehavior(new Follower(context));
            }
        }

        try {
            if (message instanceof SendHeartBeat) {
                sendHeartBeat();
                return this;
            } else if(message instanceof SendInstallSnapshot) {
                installSnapshotIfNeeded();
            } else if (message instanceof Replicate) {
                replicate((Replicate) message);
            } else if (message instanceof InstallSnapshotReply){
                handleInstallSnapshotReply(
                    (InstallSnapshotReply) message);
            }
        } finally {
            scheduleHeartBeat(context.getConfigParams().getHeartBeatInterval());
        }

        return super.handleMessage(sender, message);
    }

    private void handleInstallSnapshotReply(InstallSnapshotReply reply) {
        String followerId = reply.getFollowerId();
        FollowerToSnapshot followerToSnapshot =
            mapFollowerToSnapshot.get(followerId);

        if (followerToSnapshot != null &&
            followerToSnapshot.getChunkIndex() == reply.getChunkIndex()) {

            if (reply.isSuccess()) {
                if(followerToSnapshot.isLastChunk(reply.getChunkIndex())) {
                    //this was the last chunk reply
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("InstallSnapshotReply received, " +
                                "last chunk received, Chunk:{}. Follower:{} Setting nextIndex:{}",
                            reply.getChunkIndex(), followerId,
                            context.getReplicatedLog().getSnapshotIndex() + 1
                        );
                    }

                    FollowerLogInformation followerLogInformation =
                        followerToLog.get(followerId);
                    followerLogInformation.setMatchIndex(
                        context.getReplicatedLog().getSnapshotIndex());
                    followerLogInformation.setNextIndex(
                        context.getReplicatedLog().getSnapshotIndex() + 1);
                    mapFollowerToSnapshot.remove(followerId);

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("followerToLog.get(followerId).getNextIndex().get()=" +
                            followerToLog.get(followerId).getNextIndex().get());
                    }

                } else {
                    followerToSnapshot.markSendStatus(true);
                }
            } else {
                LOG.info("InstallSnapshotReply received, " +
                        "sending snapshot chunk failed, Will retry, Chunk:{}",
                    reply.getChunkIndex()
                );
                followerToSnapshot.markSendStatus(false);
            }

        } else {
            LOG.error("ERROR!!" +
                    "FollowerId in InstallSnapshotReply not known to Leader" +
                    " or Chunk Index in InstallSnapshotReply not matching {} != {}",
                followerToSnapshot.getChunkIndex(), reply.getChunkIndex()
            );
        }
    }

    private void replicate(Replicate replicate) {
        long logIndex = replicate.getReplicatedLogEntry().getIndex();

        if(LOG.isDebugEnabled()) {
            LOG.debug("Replicate message {}", logIndex);
        }

        // Create a tracker entry we will use this later to notify the
        // client actor
        trackerList.add(
            new ClientRequestTrackerImpl(replicate.getClientActor(),
                replicate.getIdentifier(),
                logIndex)
        );

        if (followers.size() == 0) {
            context.setCommitIndex(logIndex);
            applyLogToStateMachine(logIndex);
        } else {
            sendAppendEntries();
        }
    }

    private void sendAppendEntries() {
        // Send an AppendEntries to all followers
        for (String followerId : followers) {
            ActorSelection followerActor = context.getPeerActorSelection(followerId);

            if (followerActor != null) {
                FollowerLogInformation followerLogInformation = followerToLog.get(followerId);
                long followerNextIndex = followerLogInformation.getNextIndex().get();
                List<ReplicatedLogEntry> entries = Collections.emptyList();

                if (mapFollowerToSnapshot.get(followerId) != null) {
                    if (mapFollowerToSnapshot.get(followerId).canSendNextChunk()) {
                        sendSnapshotChunk(followerActor, followerId);
                    }

                } else {

                    if (context.getReplicatedLog().isPresent(followerNextIndex)) {
                        // FIXME : Sending one entry at a time
                        entries = context.getReplicatedLog().getFrom(followerNextIndex, 1);

                        followerActor.tell(
                            new AppendEntries(currentTerm(), context.getId(),
                                prevLogIndex(followerNextIndex),
                                prevLogTerm(followerNextIndex), entries,
                                context.getCommitIndex()).toSerializable(),
                            actor()
                        );

                    } else {
                        // if the followers next index is not present in the leaders log, then snapshot should be sent
                        long leaderSnapShotIndex = context.getReplicatedLog().getSnapshotIndex();
                        long leaderLastIndex = context.getReplicatedLog().lastIndex();
                        if (followerNextIndex >= 0 && leaderLastIndex >= followerNextIndex ) {
                            // if the follower is just not starting and leader's index
                            // is more than followers index
                            if(LOG.isDebugEnabled()) {
                                LOG.debug("SendInstallSnapshot to follower:{}," +
                                        "follower-nextIndex:{}, leader-snapshot-index:{},  " +
                                        "leader-last-index:{}", followerId,
                                    followerNextIndex, leaderSnapShotIndex, leaderLastIndex
                                );
                            }

                            actor().tell(new SendInstallSnapshot(), actor());
                        } else {
                            followerActor.tell(
                                new AppendEntries(currentTerm(), context.getId(),
                                    prevLogIndex(followerNextIndex),
                                    prevLogTerm(followerNextIndex), entries,
                                    context.getCommitIndex()).toSerializable(),
                                actor()
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * An installSnapshot is scheduled at a interval that is a multiple of
     * a HEARTBEAT_INTERVAL. This is to avoid the need to check for installing
     * snapshots at every heartbeat.
     */
    private void installSnapshotIfNeeded(){
        for (String followerId : followers) {
            ActorSelection followerActor =
                context.getPeerActorSelection(followerId);

            if(followerActor != null) {
                FollowerLogInformation followerLogInformation =
                    followerToLog.get(followerId);

                long nextIndex = followerLogInformation.getNextIndex().get();

                if (!context.getReplicatedLog().isPresent(nextIndex) &&
                    context.getReplicatedLog().isInSnapshot(nextIndex)) {
                    sendSnapshotChunk(followerActor, followerId);
                }
            }
        }
    }

    /**
     *  Sends a snapshot chunk to a given follower
     *  InstallSnapshot should qualify as a heartbeat too.
     */
    private void sendSnapshotChunk(ActorSelection followerActor, String followerId) {
        try {
            followerActor.tell(
                new InstallSnapshot(currentTerm(), context.getId(),
                    context.getReplicatedLog().getSnapshotIndex(),
                    context.getReplicatedLog().getSnapshotTerm(),
                    getNextSnapshotChunk(followerId,
                        context.getReplicatedLog().getSnapshot()),
                    mapFollowerToSnapshot.get(followerId).incrementChunkIndex(),
                    mapFollowerToSnapshot.get(followerId).getTotalChunks()
                ).toSerializable(),
                actor()
            );
            LOG.info("InstallSnapshot sent to follower {}, Chunk: {}/{}",
                followerActor.path(), mapFollowerToSnapshot.get(followerId).getChunkIndex(),
                mapFollowerToSnapshot.get(followerId).getTotalChunks());
        } catch (IOException e) {
            LOG.error(e, "InstallSnapshot failed for Leader.");
        }
    }

    /**
     * Acccepts snaphot as ByteString, enters into map for future chunks
     * creates and return a ByteString chunk
     */
    private ByteString getNextSnapshotChunk(String followerId, ByteString snapshotBytes) throws IOException {
        FollowerToSnapshot followerToSnapshot = mapFollowerToSnapshot.get(followerId);
        if (followerToSnapshot == null) {
            followerToSnapshot = new FollowerToSnapshot(snapshotBytes);
            mapFollowerToSnapshot.put(followerId, followerToSnapshot);
        }
        ByteString nextChunk = followerToSnapshot.getNextChunk();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Leader's snapshot nextChunk size:{}", nextChunk.size());
        }

        return nextChunk;
    }

    private void sendHeartBeat() {
        if (followers.size() > 0) {
            sendAppendEntries();
        }
    }

    private void stopHeartBeat() {
        if (heartbeatSchedule != null && !heartbeatSchedule.isCancelled()) {
            heartbeatSchedule.cancel();
        }
    }

    private void stopInstallSnapshotSchedule() {
        if (installSnapshotSchedule != null && !installSnapshotSchedule.isCancelled()) {
            installSnapshotSchedule.cancel();
        }
    }

    private void scheduleHeartBeat(FiniteDuration interval) {
        if(followers.size() == 0){
            // Optimization - do not bother scheduling a heartbeat as there are
            // no followers
            return;
        }

        stopHeartBeat();

        // Schedule a heartbeat. When the scheduler triggers a SendHeartbeat
        // message is sent to itself.
        // Scheduling the heartbeat only once here because heartbeats do not
        // need to be sent if there are other messages being sent to the remote
        // actor.
        heartbeatSchedule =
            context.getActorSystem().scheduler().scheduleOnce(
                interval,
                context.getActor(), new SendHeartBeat(),
                context.getActorSystem().dispatcher(), context.getActor());
    }


    private void scheduleInstallSnapshotCheck(FiniteDuration interval) {
        if(followers.size() == 0){
            // Optimization - do not bother scheduling a heartbeat as there are
            // no followers
            return;
        }

        stopInstallSnapshotSchedule();

        // Schedule a message to send append entries to followers that can
        // accept an append entries with some data in it
        installSnapshotSchedule =
            context.getActorSystem().scheduler().scheduleOnce(
                interval,
                context.getActor(), new SendInstallSnapshot(),
                context.getActorSystem().dispatcher(), context.getActor());
    }



    @Override public void close() throws Exception {
        stopHeartBeat();
    }

    @Override public String getLeaderId() {
        return context.getId();
    }

    /**
     * Encapsulates the snapshot bytestring and handles the logic of sending
     * snapshot chunks
     */
    protected class FollowerToSnapshot {
        private ByteString snapshotBytes;
        private int offset = 0;
        // the next snapshot chunk is sent only if the replyReceivedForOffset matches offset
        private int replyReceivedForOffset;
        // if replyStatus is false, the previous chunk is attempted
        private boolean replyStatus = false;
        private int chunkIndex;
        private int totalChunks;

        public FollowerToSnapshot(ByteString snapshotBytes) {
            this.snapshotBytes = snapshotBytes;
            replyReceivedForOffset = -1;
            chunkIndex = 1;
            int size = snapshotBytes.size();
            totalChunks = ( size / context.getConfigParams().getSnapshotChunkSize()) +
                ((size % context.getConfigParams().getSnapshotChunkSize()) > 0 ? 1 : 0);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Snapshot {} bytes, total chunks to send:{}",
                    size, totalChunks);
            }
        }

        public ByteString getSnapshotBytes() {
            return snapshotBytes;
        }

        public int incrementOffset() {
            if(replyStatus) {
                // if prev chunk failed, we would want to sent the same chunk again
                offset = offset + context.getConfigParams().getSnapshotChunkSize();
            }
            return offset;
        }

        public int incrementChunkIndex() {
            if (replyStatus) {
                // if prev chunk failed, we would want to sent the same chunk again
                chunkIndex =  chunkIndex + 1;
            }
            return chunkIndex;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public boolean canSendNextChunk() {
            // we only send a false if a chunk is sent but we have not received a reply yet
            return replyReceivedForOffset == offset;
        }

        public boolean isLastChunk(int chunkIndex) {
            return totalChunks == chunkIndex;
        }

        public void markSendStatus(boolean success) {
            if (success) {
                // if the chunk sent was successful
                replyReceivedForOffset = offset;
                replyStatus = true;
            } else {
                // if the chunk sent was failure
                replyReceivedForOffset = offset;
                replyStatus = false;
            }
        }

        public ByteString getNextChunk() {
            int snapshotLength = getSnapshotBytes().size();
            int start = incrementOffset();
            int size = context.getConfigParams().getSnapshotChunkSize();
            if (context.getConfigParams().getSnapshotChunkSize() > snapshotLength) {
                size = snapshotLength;
            } else {
                if ((start + context.getConfigParams().getSnapshotChunkSize()) > snapshotLength) {
                    size = snapshotLength - start;
                }
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("length={}, offset={},size={}",
                    snapshotLength, start, size);
            }
            return getSnapshotBytes().substring(start, start + size);

        }
    }

}
