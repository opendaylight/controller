/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;
import org.apache.pekko.dispatch.ControlMessage;

/**
 * This is an internal message that is stored in the akka's persistent journal. During recovery, this
 * message is used to apply recovered journal entries to the state whose indexes range from the context's
 * current lastApplied index to "toIndex" contained in the message. This message is sent internally from a
 * behavior to the RaftActor to persist.
 *
 * @author Thomas Pantelis
 */
public final class ApplyJournalEntries implements Serializable, ControlMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final long toIndex;

    public ApplyJournalEntries(final long toIndex) {
        this.toIndex = toIndex;
    }

    public long getToIndex() {
        return toIndex;
    }

    @Override
    public String toString() {
        return "ApplyJournalEntries [toIndex=" + toIndex + "]";
    }

    @java.io.Serial
    private Object writeReplace() {
        return new AJE(this);
    }
}
