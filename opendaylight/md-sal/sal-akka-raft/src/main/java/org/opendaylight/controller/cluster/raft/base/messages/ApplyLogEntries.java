/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

/**
 * ApplyLogEntries serves as a message which is stored in the akka's persistent
 * journal.
 * During recovery if this message is found, then all in-mem journal entries from
 * context.lastApplied to ApplyLogEntries.toIndex are applied to the state
 *
 * This class is also used as a internal message sent from Behaviour to
 * RaftActor to persist the ApplyLogEntries
 *
 * @deprecated Deprecated in favor of ApplyJournalEntries whose type for toIndex is long instead of int.
 *             This class was kept for backwards compatibility with Helium.
 */
@Deprecated
public class ApplyLogEntries implements Serializable {
    private final int toIndex;

    public ApplyLogEntries(int toIndex) {
        this.toIndex = toIndex;
    }

    public int getToIndex() {
        return toIndex;
    }
}
