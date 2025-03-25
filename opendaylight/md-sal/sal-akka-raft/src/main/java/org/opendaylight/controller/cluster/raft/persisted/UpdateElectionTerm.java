/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.api.TermInfo;

/**
 * Message class to persist election term information.
 *
 * @deprecated This class exists only for backward compatibility and testing thereof. It will be removed in the next
 *             major release.
 */
@Deprecated(since = "11.0.0", forRemoval = true)
public record UpdateElectionTerm(@NonNull TermInfo termInfo) implements Serializable {
    public UpdateElectionTerm {
        requireNonNull(termInfo);
    }

    public UpdateElectionTerm(final long term, final @Nullable String votedFor) {
        this(new TermInfo(term, votedFor));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("currentTerm", termInfo.term())
            .add("votedFor", termInfo.votedFor())
            .toString();
    }

    @java.io.Serial
    private Object writeReplace() {
        return new UT(termInfo);
    }
}

