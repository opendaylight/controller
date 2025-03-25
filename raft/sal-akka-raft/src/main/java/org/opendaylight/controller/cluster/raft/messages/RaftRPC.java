/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.Serializable;
import org.apache.pekko.dispatch.ControlMessage;

/**
 * Interface implemented by all requests exchanged in the Raft protocol.
 */
public abstract sealed class RaftRPC implements Serializable, ControlMessage
        permits AppendEntries, AppendEntriesReply,
                InstallSnapshot, InstallSnapshotReply,
                RequestVote, RequestVoteReply {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // TODO: signed long, e.g. we only support up to 63 bit terms. We really should support the full range, but that
    //       requires a complete audit of users. The safest way to do that is to make 'UnsignedLong getTerm()' and
    //       have a more efficient 'long term()', safe users are migrated to? Needs some careful experimentation.
    private final long term;

    RaftRPC(final long term) {
        this.term = term;
    }

    /**
     * Return the term in which this call is being made.
     *
     * @return The term ID
     */
    public final long getTerm() {
        return term;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("term", term);
    }

    // All implementations must use Externalizable Proxy pattern
    @java.io.Serial
    abstract Object writeReplace();
}
