/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingObject;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * Utility class for implementing wrapper {@link RaftActorBehavior}s. This class should be used by RaftActor
 * subclasses to implement their specific behaviors.
 */
@Beta
public abstract class ForwardingRaftActorBehavior extends ForwardingObject implements RaftActorBehavior {
    @Override
    protected abstract @Nonnull RaftActorBehavior delegate();

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        return delegate().handleMessage(sender, message);
    }

    @Override
    public RaftState state() {
        return delegate().state();
    }

    @Override
    public String getLeaderId() {
        return delegate().getLeaderId();
    }

    @Override
    public void setReplicatedToAllIndex(final long replicatedToAllIndex) {
        delegate().setReplicatedToAllIndex(replicatedToAllIndex);
    }

    @Override
    public long getReplicatedToAllIndex() {
        return delegate().getReplicatedToAllIndex();
    }

    @Override
    public short getLeaderPayloadVersion() {
        return delegate().getLeaderPayloadVersion();
    }

    @Override
    public RaftActorBehavior switchBehavior(final RaftActorBehavior behavior) {
        return delegate().switchBehavior(behavior);
    }

    @Override
    public final AbstractRaftActorBehavior getBaseBehavior() {
        return delegate().getBaseBehavior();
    }
}
