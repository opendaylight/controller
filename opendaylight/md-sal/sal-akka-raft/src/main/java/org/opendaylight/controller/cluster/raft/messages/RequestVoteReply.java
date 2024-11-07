/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.MoreObjects.ToStringHelper;

public final class RequestVoteReply extends RaftRPC {
    @java.io.Serial
    private static final long serialVersionUID = 8427899326488775660L;

    // true means candidate received vote
    private final boolean voteGranted;

    public RequestVoteReply(final long term, final boolean voteGranted) {
        super(term);
        this.voteGranted = voteGranted;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper).add("voteGranted", voteGranted);
    }

    @Override
    Object writeReplace() {
        return new VR(this);
    }
}
