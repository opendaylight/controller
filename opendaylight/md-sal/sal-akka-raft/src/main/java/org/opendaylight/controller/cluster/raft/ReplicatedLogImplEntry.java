/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * A {@link ReplicatedLogEntry} implementation.
 */
public final class ReplicatedLogImplEntry implements ReplicatedLogEntry, Serializable {
    private static final long serialVersionUID = -9085798014576489130L;

    private final long index;
    private final long term;
    private final Payload payload;

    /**
     * Constructs an instance.
     *
     * @param index the index
     * @param term the term
     * @param payload the payload
     */
    public ReplicatedLogImplEntry(final long index, final long term, final Payload payload) {
        this.index = index;
        this.term = term;
        this.payload = Preconditions.checkNotNull(payload);
    }

    @Override
    public Payload getData() {
        return payload;
    }

    @Override
    public long getTerm() {
        return term;
    }

    @Override
    public long getIndex() {
        return index;
    }

    @Override
    public int size() {
        return getData().size();
    }

    @Override
    public String toString() {
        return "Entry{index=" + index + ", term=" + term + '}';
    }
}
