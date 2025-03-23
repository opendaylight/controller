/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.util.Collection;
import java.util.List;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;

/**
 * Extends OnDemandRaftState to add Shard state.
 *
 * @author Thomas Pantelis
 */
public final class OnDemandShardState extends OnDemandRaftState {
    private final List<ActorSelection> treeChangeListenerActors;
    private final List<ActorRef> commitCohortActors;

    private OnDemandShardState(final Builder builder) {
        super(builder);
        treeChangeListenerActors = builder.treeChangeListenerActors;
        commitCohortActors = builder.commitCohortActors;
    }

    public Collection<ActorSelection> getTreeChangeListenerActors() {
        return treeChangeListenerActors;
    }

    public Collection<ActorRef> getCommitCohortActors() {
        return commitCohortActors;
    }

    public static final class Builder extends AbstractBuilder<Builder, OnDemandShardState> {
        private List<ActorSelection> treeChangeListenerActors = List.of();
        private List<ActorRef> commitCohortActors = List.of();

        public @NonNull Builder treeChangeListenerActors(final Collection<ActorSelection> value) {
            treeChangeListenerActors = List.copyOf(value);
            return self();
        }

        public @NonNull Builder commitCohortActors(final Collection<ActorRef> value) {
            commitCohortActors = List.copyOf(value);
            return self();
        }

        @Override
        public OnDemandShardState build() {
            return new OnDemandShardState(this);
        }
    }
}
