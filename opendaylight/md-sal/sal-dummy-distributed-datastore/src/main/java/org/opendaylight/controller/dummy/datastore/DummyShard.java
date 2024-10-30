/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.dummy.datastore;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
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

public class DummyShard extends UntypedAbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(DummyShard.class);

    private final Configuration configuration;
    private final String followerId;
    private long lastMessageIndex  = -1;
    private long lastMessageSize = 0;
    private Stopwatch appendEntriesWatch;

    public DummyShard(final Configuration configuration, final String followerId) {
        this.configuration = configuration;
        this.followerId = followerId;
        LOG.info("Creating : {}", followerId);
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final ActorRef getSender() {
        return super.getSender();
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        switch (message) {
            case RequestVote msg -> getSender().tell(new RequestVoteReply(msg.getTerm(), true), self());
            case AppendEntries msg -> handleAppendEntries(msg);
            case InstallSnapshot msg -> handleInstallSnapshot(msg);
            default -> LOG.error("Unknown message : {}", message.getClass());
        }
    }

    private void handleInstallSnapshot(final InstallSnapshot req) {
        getSender().tell(new InstallSnapshotReply(req.getTerm(), followerId, req.getChunkIndex(), true), self());
    }

    protected void handleAppendEntries(final AppendEntries req) throws InterruptedException {
        LOG.info("{} - Received AppendEntries message : leader term = {}, index = {}, prevLogIndex = {}, size = {}",
                followerId, req.getTerm(),req.getLeaderCommit(), req.getPrevLogIndex(), req.getEntries().size());

        if (appendEntriesWatch != null) {
            long elapsed = appendEntriesWatch.elapsed(TimeUnit.SECONDS);
            if (elapsed >= 5) {
                LOG.error("More than 5 seconds since last append entry, elapsed Time = {} seconds"
                                + ", leaderCommit = {}, prevLogIndex = {}, size = {}",
                        elapsed, req.getLeaderCommit(), req.getPrevLogIndex(), req.getEntries().size());
            }
            appendEntriesWatch.reset().start();
        } else {
            appendEntriesWatch = Stopwatch.createStarted();
        }

        if (lastMessageIndex == req.getLeaderCommit() && req.getEntries().size() > 0 && lastMessageSize > 0) {
            LOG.error("{} - Duplicate message with leaderCommit = {} prevLogIndex = {} received", followerId,
                    req.getLeaderCommit(), req.getPrevLogIndex());
        }

        lastMessageIndex = req.getLeaderCommit();
        lastMessageSize = req.getEntries().size();

        long lastIndex = req.getLeaderCommit();
        if (req.getEntries().size() > 0) {
            for (ReplicatedLogEntry entry : req.getEntries()) {
                lastIndex = entry.index();
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
                getSender().tell(new AppendEntriesReply(followerId, req.getTerm(), true, lastIndex, req.getTerm(),
                        DataStoreVersions.CURRENT_VERSION), self());
            }
        } else {
            getSender().tell(new AppendEntriesReply(followerId, req.getTerm(), true, lastIndex, req.getTerm(),
                    DataStoreVersions.CURRENT_VERSION), self());
        }
    }

    public static Props props(final Configuration configuration, final String followerId) {
        return Props.create(DummyShard.class, configuration, followerId);
    }
}
