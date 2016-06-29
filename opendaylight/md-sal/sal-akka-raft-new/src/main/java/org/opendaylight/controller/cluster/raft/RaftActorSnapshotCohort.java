/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;

/**
 * Interface for a class that participates in raft actor snapshotting.
 *
 * @author Thomas Pantelis
 */
public interface RaftActorSnapshotCohort {

    /**
     * This method is called by the RaftActor when a snapshot needs to be
     * created. The implementation should send a CaptureSnapshotReply to the given actor.
     *
     * @param actorRef the actor to which to respond
     */
    void createSnapshot(ActorRef actorRef);

    /**
     * This method is called to apply a snapshot installed by the leader.
     *
     * @param snapshotBytes a snapshot of the state of the actor
     */
    void applySnapshot(byte[] snapshotBytes);
}
