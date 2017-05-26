/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

public class CanCommitTransactionReply extends VersionedExternalizableMessage {
    private static final CanCommitTransactionReply YES =
            new CanCommitTransactionReply(true, DataStoreVersions.CURRENT_VERSION);
    private static final CanCommitTransactionReply NO =
            new CanCommitTransactionReply(false, DataStoreVersions.CURRENT_VERSION);

    private boolean canCommit;

    public CanCommitTransactionReply() {
    }

    private CanCommitTransactionReply(final boolean canCommit, final short version) {
        super(version);
        this.canCommit = canCommit;
    }

    public boolean getCanCommit() {
        return canCommit;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        canCommit = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeBoolean(canCommit);
    }

    @Override
    public String toString() {
        return "CanCommitTransactionReply [canCommit=" + canCommit + ", version=" + getVersion() + "]";
    }

    public static CanCommitTransactionReply yes(short version) {
        return version == DataStoreVersions.CURRENT_VERSION ? YES : new CanCommitTransactionReply(true, version);
    }

    public static CanCommitTransactionReply no(short version) {
        return version == DataStoreVersions.CURRENT_VERSION ? NO : new CanCommitTransactionReply(false, version);
    }

    public static CanCommitTransactionReply fromSerializable(final Object serializable) {
        Preconditions.checkArgument(serializable instanceof CanCommitTransactionReply);
        return (CanCommitTransactionReply)serializable;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CanCommitTransactionReply;
    }
}
