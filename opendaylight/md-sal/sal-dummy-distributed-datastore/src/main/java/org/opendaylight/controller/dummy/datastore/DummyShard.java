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
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyShard extends UntypedActor{
    private final Configuration configuration;
    private final String followerId;
    private final Logger LOG = LoggerFactory.getLogger(DummyShard.class);

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
        } else if(AppendEntries.LEGACY_SERIALIZABLE_CLASS.equals(o.getClass()) ) {
            AppendEntries req = AppendEntries.fromSerializable(o);
            handleAppendEntries(req);
        } else if(o instanceof AppendEntries){
            handleAppendEntries((AppendEntries) o);
        } else {
            LOG.error("Unknown message : {}", o.getClass());
        }
    }

    protected void handleAppendEntries(AppendEntries req) throws InterruptedException {
        LOG.info("{} - Received AppendEntries message : leader term, index, size = {}, {}, {}", followerId, req.getTerm(),req.getLeaderCommit(), req.getEntries().size());
        long lastIndex = req.getLeaderCommit();
        if (req.getEntries().size() > 0)
            lastIndex = req.getEntries().get(0).getIndex();

        if (configuration.shouldCauseTrouble()) {
            boolean ignore = false;

            if (configuration.shouldDropReplies()) {
                ignore = Math.random() > 0.5;
            }

            long delay = (long) (Math.random() * configuration.getMaxDelayInMillis());

            if (!ignore) {
                LOG.info("{} - Randomizing delay : {}", followerId, delay);
                Thread.sleep(delay);
                sender().tell(new AppendEntriesReply(followerId, req.getTerm(), true, lastIndex, req.getTerm()), self());
            }
        } else {
            sender().tell(new AppendEntriesReply(followerId, req.getTerm(), true, lastIndex, req.getTerm()), self());
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
