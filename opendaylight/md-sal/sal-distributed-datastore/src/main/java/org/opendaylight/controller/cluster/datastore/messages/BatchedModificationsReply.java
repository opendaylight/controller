/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The reply for the BatchedModifications message.
 *
 * @author Thomas Pantelis
 */
@Deprecated(since = "9.0.0", forRemoval = true)
public final class BatchedModificationsReply extends VersionedExternalizableMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private int numBatched;

    public BatchedModificationsReply() {
    }

    public BatchedModificationsReply(final int numBatched) {
        this.numBatched = numBatched;
    }

    public int getNumBatched() {
        return numBatched;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        numBatched = in.readInt();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(numBatched);
    }

    @Override
    public String toString() {
        return "BatchedModificationsReply [numBatched=" + numBatched + "]";
    }
}
