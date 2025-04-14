/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import org.opendaylight.controller.cluster.raft.spi.AbstractRaftCommand;
import org.opendaylight.controller.cluster.raft.spi.AbstractStateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateMachineCommand;

/**
 * An instance of a {@link Payload} class is meant to be used as the Payload for {@link AppendEntries}.
 *
 * <p>When an actor which is derived from RaftActor attempts to persistData it must pass an instance of the Payload
 * class. Similarly when state needs to be applied to the derived RaftActor it will be passed an instance of the
 * Payload class.
 */
// FIXME: This class is geared towards on-heap storage in that it is estimating/knowing its in-memory and serialized
//        footprint -- something that is mirrored in ReplicatedLogEntry's fields.
//        What we really want here is a split between serialized and non-serialized form, because the exact nature of
//        storage concerns are separate. We really have a few serialization strategies:
//        - a leader persistence request, where we want to hang on to unserialized state for the purposes of handing
//          the handle to applyState(). On the leader we start with unserialized state, then store/replicate the entry
//          and call applyState() when consensus is reached.
//        - a follower-received request, where we start with serialized state and deserialize for the purposes of
//          applyState()
//        Preferred data entry storage differs based on mode of operation, based on persistence, presence of peers and
//        perhaps voting status:
//        - enabled persistence means entry data is serialized to durable storage before applyState() is called
//        - disabled persistence with peers means the same, except there may be a number of storage strategies:
//        - ClusterConfig is always durable
//        - serial form is stored in-memory:
//        - currently on-heap (via ByteArray)
//        - we want also off-heap (via NativeByteArray)
//        - we want also memory-mapped file (via MappedByteArray)
//        - we want to compress select entries (marked, opt-in)
//        - disabled persistence without peers does not need any serialization
//        For all of this to happen, though, we need to have well-defined transitions between strategies. There is also
//        interplay with AppendEntries compression/slicing to consider.
public abstract sealed class Payload implements StateMachineCommand, Serializable
        permits AbstractRaftCommand, AbstractStateCommand {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /**
     * Return the estimate of in-memory size of this payload.
     *
     * @return An estimate of the in-memory size of this payload.
     */
    public abstract int size();

    /**
     * Return the estimate of serialized size of this payload when passed through serialization. The estimate needs to
     * be reasonably accurate and should err on the side of caution and report a slightly-higher size in face of
     * uncertainty.
     *
     * @return An estimate of serialized size.
     */
    public abstract int serializedSize();

    @Override
    public final Serializable toSerialForm() {
        return this;
    }

    /**
     * Return the serialization proxy for this object.
     *
     * @return Serialization proxy
     */
    @java.io.Serial
    protected abstract Object writeReplace();

    @java.io.Serial
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        throwNSE();
    }

    @java.io.Serial
    private void readObjectNoData() throws ObjectStreamException {
        throwNSE();
    }

    @java.io.Serial
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        throwNSE();
    }

    private void throwNSE() throws NotSerializableException {
        throw new NotSerializableException(getClass().getName());
    }
}
