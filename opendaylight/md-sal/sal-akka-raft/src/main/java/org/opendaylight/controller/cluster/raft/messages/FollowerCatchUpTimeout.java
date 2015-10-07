/*
 * Copyright (c) 2014 Dell Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

/**
 * Local message sent to self when catch-up of a new follower doesn't complete in a timely manner
 */

public class FollowerCatchUpTimeout {
    private final String newServerId;

    public FollowerCatchUpTimeout(String serverId){
       this.newServerId = serverId;
    }
    public String getNewServerId() {
        return newServerId;
    }

}
