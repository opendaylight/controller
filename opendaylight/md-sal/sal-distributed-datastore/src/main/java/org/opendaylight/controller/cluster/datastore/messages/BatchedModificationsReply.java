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
public class BatchedModificationsReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private int numBatched;

    public BatchedModificationsReply() {
    }

    public BatchedModificationsReply(int numBatched) {
        this.numBatched = numBatched;
    }

    public int getNumBatched() {
        return numBatched;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        numBatched = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(numBatched);
    }

    @Override
    protected Object newLegacySerializedInstance() {
        // no legacy serialized type for this class; return self
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BatchedModificationsReply [numBatched=").append(numBatched).append("]");
        return builder.toString();
    }
}
