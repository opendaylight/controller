/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * ApplyLogEntries serves as a message which is stored in the akka's persistent
 * journal.
 * During recovery if this message is found, then all in-mem journal entries from
 * context.lastApplied to ApplyLogEntries.toIndex are applied to the state
 *
 * This class is also used as a internal message sent from Behaviour to
 * RaftActor to persist the ApplyLogEntries
 *
 */
public class ApplyLogEntries implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient int toIndex;

    public ApplyLogEntries() {
    }

    public ApplyLogEntries(int toIndex) {
        this.toIndex = toIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        toIndex = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(toIndex);
    }
}
