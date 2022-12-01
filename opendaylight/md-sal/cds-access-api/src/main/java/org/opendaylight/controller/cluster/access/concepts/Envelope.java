/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObjects;

public abstract class Envelope<T extends Message<?, ?>> implements Immutable, Serializable {
    interface SerialForm<T extends Message<?, ?>, E extends Envelope<T>> extends Externalizable {

        @NonNull E envelope();

        void setEnvelope(@NonNull E envelope);

        @java.io.Serial
        Object readResolve();

        @Override
        default void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final byte header = WritableObjects.readLongHeader(in);
            final var sessionId = WritableObjects.readFirstLong(in, header);
            final var txSequence = WritableObjects.readSecondLong(in, header);
            @SuppressWarnings("unchecked")
            final var message = (T) in.readObject();
            setEnvelope(readExternal(in, sessionId, txSequence, message));
        }

        E readExternal(ObjectInput in, long sessionId, long txSequence, T message) throws IOException;

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            writeExternal(out, envelope());
        }

        default void writeExternal(final ObjectOutput out, final @NonNull E envelope) throws IOException {
            WritableObjects.writeLongs(out, envelope.getSessionId(), envelope.getTxSequence());
            out.writeObject(envelope.getMessage());
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull T message;
    private final long txSequence;
    private final long sessionId;

    Envelope(final T message, final long sessionId, final long txSequence) {
        this.message = requireNonNull(message);
        this.sessionId = sessionId;
        this.txSequence = txSequence;
    }

    /**
     * Get the enclosed message.
     *
     * @return enclose message
     */
    public @NonNull T getMessage() {
        return message;
    }

    /**
     * Get the message transmission sequence of this envelope.
     *
     * @return Message sequence
     */
    public long getTxSequence() {
        return txSequence;
    }

    /**
     * Get the session identifier.
     *
     * @return Session identifier
     */
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Envelope.class).add("sessionId", Long.toHexString(sessionId))
                .add("txSequence", Long.toHexString(txSequence)).add("message", message).toString();
    }

    @java.io.Serial
    final Object writeReplace() {
        return ABIVersion.MAGNESIUM.lt(message.getVersion()) ? createProxy() : legacyProxy();
    }

    abstract @NonNull SerialForm<T, ?> createProxy();

    @Deprecated(since = "7.0.0", forRemoval = true)
    abstract @NonNull AbstractEnvelopeProxy<T, ?> legacyProxy();
}
