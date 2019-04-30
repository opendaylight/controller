/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import akka.dispatch.ControlMessage;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;

public class InstallSnapshotReset implements ControlMessage {

    private FollowerLogInformation followerLogInfo;

    public InstallSnapshotReset(FollowerLogInformation followerLogInfo) {
        this.followerLogInfo = followerLogInfo;
    }

    public FollowerLogInformation getFollowerLogInfo() {
        return followerLogInfo;
    }
}
