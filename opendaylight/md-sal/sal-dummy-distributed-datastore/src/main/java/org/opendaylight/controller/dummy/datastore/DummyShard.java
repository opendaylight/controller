/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.dummy.datastore;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyShard extends UntypedActor{
    private final Configuration configuration;
    private final String followerId;
    private final Logger LOG = LoggerFactory.getLogger(DummyShard.class);
    private long lastMessageIndex  = -1;
    private long lastMessageSize = 0;
    private Stopwatch appendEntriesWatch;

    public DummyShard(Configuration configuration, String followerId) {
        this.configuration = configuration;
        this.followerId = followerId;
        LOG.info("Creating : {}", followerId);
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if(o instanceof RequestVote){
            RequestVote req = (RequestVote) o;
            sender().tell(new RequestVoteReply(req.getTerm(), true), self());
        } else if(o instanceof AppendEntries) {
            handleAppendEntries((AppendEntries)o);
        } else if(InstallSnapshot.isSerializedType(o)) {
            InstallSnapshot req = InstallSnapshot.fromSerializable(o);
            handleInstallSnapshot(req);
        } else if(o instanceof InstallSnapshot){
            handleInstallSnapshot((InstallSnapshot) o);
        } else {
            LOG.error("Unknown message : {}", o.getClass());
        }
    }

    private void handleInstallSnapshot(InstallSnapshot req) {
        sender().tell(new InstallSnapshotReply(req.getTerm(), followerId, req.getChunkIndex(), true), self());
    }

    protected void handleAppendEntries(AppendEntries req) throws InterruptedException {
        LOG.info("{} - Received AppendEntries message : leader term = {}, index = {}, prevLogIndex = {}, size = {}",
                followerId, req.getTerm(),req.getLeaderCommit(), req.getPrevLogIndex(), req.getEntries().size());

        if(appendEntriesWatch != null){
            long elapsed = appendEntriesWatch.elapsed(TimeUnit.SECONDS);
            if(elapsed >= 5){
                LOG.error("More than 5 seconds since last append entry, elapsed Time = {} seconds" +
                                ", leaderCommit = {}, prevLogIndex = {}, size = {}",
                        elapsed, req.getLeaderCommit(), req.getPrevLogIndex(), req.getEntries().size());
            }
            appendEntriesWatch.reset().start();
        } else {
            appendEntriesWatch = Stopwatch.createStarted();
        }

        if(lastMessageIndex == req.getLeaderCommit() && req.getEntries().size() > 0 && lastMessageSize > 0){
            LOG.error("{} - Duplicate message with leaderCommit = {} prevLogIndex = {} received", followerId, req.getLeaderCommit(), req.getPrevLogIndex());
        }

        lastMessageIndex = req.getLeaderCommit();
        lastMessageSize = req.getEntries().size();

        long lastIndex = req.getLeaderCommit();
        if (req.getEntries().size() > 0) {
            for(ReplicatedLogEntry entry : req.getEntries()) {
                lastIndex = entry.getIndex();
            }
        }

        if (configuration.shouldCauseTrouble() && req.getEntries().size() > 0) {
            boolean ignore = false;

            if (configuration.shouldDropReplies()) {
                ignore = Math.random() > 0.5;
            }

            long delay = (long) (Math.random() * configuration.getMaxDelayInMillis());

            if (!ignore) {
                LOG.info("{} - Randomizing delay : {}", followerId, delay);
                Thread.sleep(delay);
                sender().tell(new AppendEntriesReply(followerId, req.getTerm(), true, lastIndex, req.getTerm(),
                        DataStoreVersions.CURRENT_VERSION), self());
            }
        } else {
            sender().tell(new AppendEntriesReply(followerId, req.getTerm(), true, lastIndex, req.getTerm(),
                    DataStoreVersions.CURRENT_VERSION), self());
        }
    }

    public static Props props(Configuration configuration, final String followerId) {

        return Props.create(new DummyShardCreator(configuration, followerId));
    }

    private static class DummyShardCreator implements Creator<DummyShard> {

        private static final long serialVersionUID = 1L;
        private final Configuration configuration;
        private final String followerId;

        DummyShardCreator(Configuration configuration, String followerId) {
            this.configuration = configuration;
            this.followerId = followerId;
        }

        @Override
        public DummyShard create() throws Exception {
            return new DummyShard(configuration, followerId);
        }
    }

}
