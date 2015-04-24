/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * A RaftActorBehavior implementation that delegates to another implementation.
 *
 * @author Thomas Pantelis
 */
public class DelegatingRaftActorBehavior implements RaftActorBehavior {
    private RaftActorBehavior delegate;

    public RaftActorBehavior getDelegate() {
        return delegate;
    }

    public void setDelegate(RaftActorBehavior delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public RaftActorBehavior handleMessage(ActorRef sender, Object message) {
        return delegate.handleMessage(sender, message);
    }

    @Override
    public RaftState state() {
        return delegate.state();
    }

    @Override
    public String getLeaderId() {
        return delegate.getLeaderId();
    }

    @Override
    public void setReplicatedToAllIndex(long replicatedToAllIndex) {
        delegate.setReplicatedToAllIndex(replicatedToAllIndex);
    }

    @Override
    public long getReplicatedToAllIndex() {
        return delegate.getReplicatedToAllIndex();
    }

    @Override
    public short getLeaderPayloadVersion() {
        return delegate.getLeaderPayloadVersion();
    }
}
