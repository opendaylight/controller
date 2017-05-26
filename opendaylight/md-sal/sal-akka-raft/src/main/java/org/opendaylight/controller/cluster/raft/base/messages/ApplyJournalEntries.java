/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

/**
 * This is an internal message that is stored in the akka's persistent journal. During recovery, this
 * message is used to apply recovered journal entries to the state whose indexes range from the context's
 * current lastApplied index to "toIndex" contained in the message. This message is sent internally from a
 * behavior to the RaftActor to persist.
 *
 * @author Thomas Pantelis
 *
 * @deprecated Use {@link org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries} instead.
 */
@Deprecated
public class ApplyJournalEntries implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long toIndex;

    public ApplyJournalEntries(long toIndex) {
        this.toIndex = toIndex;
    }

    public long getToIndex() {
        return toIndex;
    }

    private Object readResolve() {
        return org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries.createMigrated(toIndex);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ApplyJournalEntries [toIndex=").append(toIndex).append("]");
        return builder.toString();
    }
}
