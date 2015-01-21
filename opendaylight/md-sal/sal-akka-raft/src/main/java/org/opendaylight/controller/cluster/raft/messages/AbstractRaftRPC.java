/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

public class AbstractRaftRPC implements RaftRPC {
    private static final long serialVersionUID = -6061342433962854822L;

    // term
    protected long term;

    protected AbstractRaftRPC(long term){
        this.term = term;
    }

    // added for testing while serialize-messages=on
    public AbstractRaftRPC() {
    }

    @Override
    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }
}
