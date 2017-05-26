/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import java.util.Collection;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;

/**
 * Extends OnDemandRaftState to add Shard state.
 *
 * @author Thomas Pantelis
 */
public class OnDemandShardState extends OnDemandRaftState {
    private Collection<ActorSelection> treeChangeListenerActors;
    private Collection<ActorSelection> dataChangeListenerActors;
    private Collection<ActorRef> commitCohortActors;

    public Collection<ActorSelection> getTreeChangeListenerActors() {
        return treeChangeListenerActors;
    }

    public Collection<ActorSelection> getDataChangeListenerActors() {
        return dataChangeListenerActors;
    }

    public Collection<ActorRef> getCommitCohortActors() {
        return commitCohortActors;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder> {
        private final OnDemandShardState state = new OnDemandShardState();

        @Override
        protected OnDemandRaftState state() {
            return state;
        }

        public Builder treeChangeListenerActors(Collection<ActorSelection> actors) {
            state.treeChangeListenerActors = actors;
            return self();
        }

        public Builder dataChangeListenerActors(Collection<ActorSelection> actors) {
            state.dataChangeListenerActors = actors;
            return self();
        }

        public Builder commitCohortActors(Collection<ActorRef> actors) {
            state.commitCohortActors = actors;
            return self();
        }
    }
}
