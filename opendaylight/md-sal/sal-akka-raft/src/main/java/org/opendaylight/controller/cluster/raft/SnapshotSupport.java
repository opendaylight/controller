/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

/**
 * Snapshot support for RaftActor.
 *
 * @author Thomas Pantelis
 */
public interface SnapshotSupport {

    void capture(long lastAppliedTerm, long lastAppliedIndex, long replicatedToAllIndex,
            boolean isInstallSnapshotInitiated);

    void rollback();

    void persist(ByteString stateInBytes, RaftActorBehavior behavior, boolean isLeader);

    void commit(long sequenceNumber);
}
