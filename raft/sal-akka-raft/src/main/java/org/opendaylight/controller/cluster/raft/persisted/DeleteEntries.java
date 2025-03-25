/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;

/**
 * Internal message that is stored in the akka's persistent journal to delete journal entries.
 *
 * @author Thomas Pantelis
 */
public final class DeleteEntries implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final long fromIndex;

    public DeleteEntries(final long fromIndex) {
        this.fromIndex = fromIndex;
    }

    public long getFromIndex() {
        return fromIndex;
    }

    @Override
    public String toString() {
        return "DeleteEntries [fromIndex=" + fromIndex + "]";
    }

    @java.io.Serial
    private Object writeReplace() {
        return new DE(this);
    }
}
