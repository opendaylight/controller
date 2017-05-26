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
 * Internal message that is stored in the akka's persistent journal to delete journal entries.
 *
 * @author Thomas Pantelis
 *
 * @deprecated Use {@link org.opendaylight.controller.cluster.raft.persisted.DeleteEntries} instead.
 */
@Deprecated
public class DeleteEntries implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long fromIndex;

    public DeleteEntries(long fromIndex) {
        this.fromIndex = fromIndex;
    }

    public long getFromIndex() {
        return fromIndex;
    }

    private Object readResolve() {
        return org.opendaylight.controller.cluster.raft.persisted.DeleteEntries.createMigrated(fromIndex);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeleteEntries [fromIndex=").append(fromIndex).append("]");
        return builder.toString();
    }
}
