/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

public abstract class AbstractRaftRPC implements RaftRPC, SerializableMessage {
    @java.io.Serial
    private static final long serialVersionUID = -6061342433962854822L;

    // term
    private final long term;

    AbstractRaftRPC(final long term) {
        this.term = term;
    }

    @Override
    public final long getTerm() {
        return term;
    }

    // All implementations must use Externalizable Proxy pattern
    @java.io.Serial
    abstract Object writeReplace();
}
