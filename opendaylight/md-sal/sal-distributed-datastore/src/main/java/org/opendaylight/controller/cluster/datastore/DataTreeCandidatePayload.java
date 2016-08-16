/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.persisted.DataTreeCandidateInputOutput;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * @deprecated Deprecated in Boron in favor of CommitTransactionPayload
 */
@Deprecated
final class DataTreeCandidatePayload extends Payload implements Externalizable, MigratedSerializable {
    private static final long serialVersionUID = 1L;

    private transient byte[] serialized;

    public DataTreeCandidatePayload() {
        // Required by Externalizable
    }

    private DataTreeCandidatePayload(final byte[] serialized) {
        this.serialized = Preconditions.checkNotNull(serialized);
    }

    /**
     * @deprecated Use CommitTransactionPayload instead
     */
    @Deprecated
    static DataTreeCandidatePayload create(final DataTreeCandidate candidate) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            DataTreeCandidateInputOutput.writeDataTreeCandidate(out, candidate);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to serialize candidate %s", candidate), e);
        }

        return new DataTreeCandidatePayload(out.toByteArray());
    }

    public DataTreeCandidate getCandidate() throws IOException {
        return DataTreeCandidateInputOutput.readDataTreeCandidate(ByteStreams.newDataInput(serialized));
    }

    @Override
    public int size() {
        return serialized.length;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeByte((byte)serialVersionUID);
        out.writeInt(serialized.length);
        out.write(serialized);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final long version = in.readByte();
        Preconditions.checkArgument(version == serialVersionUID, "Unsupported serialization version %s", version);

        final int length = in.readInt();
        serialized = new byte[length];
        in.readFully(serialized);
    }

    @Override
    public boolean isMigrated() {
        return true;
    }

    @Deprecated
    @Override
    public Object writeReplace() {
        // this is fine
        return this;
    }
}
