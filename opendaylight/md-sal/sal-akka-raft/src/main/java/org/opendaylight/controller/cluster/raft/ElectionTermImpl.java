/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

public class ElectionTermImpl implements ElectionTerm{
    /**
     * Identifier of the actor whose election term information this is
     */
    private final String id;

    private long currentTerm;

    private String votedFor;

    public ElectionTermImpl(String id) {
        this.id = id;

        // TODO: Read currentTerm from some persistent state
        currentTerm = 0;

        // TODO: Read votedFor from some file
        votedFor = "";
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void update(long currentTerm, String votedFor){
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;

        // TODO : Write to some persistent state
    }
}
