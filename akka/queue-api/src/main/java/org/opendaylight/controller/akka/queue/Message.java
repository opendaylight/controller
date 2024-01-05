/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.queue;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * An abstract concept of a Message. This class cannot be instantiated directly, use its specializations {@link Request}
 * and {@link Response}.
 *
 * <p>
 * Messages have a target and a sequence number. Sequence numbers are expected to be assigned monotonically on a
 * per-target basis, hence two targets can observe the same sequence number.
 *
 * <p>
 * Note that this class specifies the {@link Immutable} contract, which means that all subclasses must follow this API
 * contract and be effectively immutable.
 *
 * @param <T> Target identifier type
 * @param <C> Message type
 */
public abstract sealed class Message<T extends WritableIdentifier, C extends Message<T, C>>
        implements Immutable, Serializable permits Request, Response {
    /**
     * Externalizable proxy for use with {@link Message} subclasses.
     *
     * @param <T> Target identifier type
     * @param <C> Message class
     */
    public sealed interface SerialForm<T extends WritableIdentifier, C extends Message<T, C>> extends Externalizable
            permits Request.SerialForm, Response.SerialForm {

        @NonNull C message();

        void setMessage(@NonNull C message);

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            final var message = message();
            message.target().writeTo(out);
            WritableObjects.writeLong(out, message.sequence());
            writeExternal(out, message);
        }

        void writeExternal(@NonNull ObjectOutput out, @NonNull C msg) throws IOException;

        @Override
        default void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final var target = verifyNotNull(readTarget(in));
            final var sequence = WritableObjects.readLong(in);
            setMessage(verifyNotNull(readExternal(in, target, sequence)));
        }

        @NonNull C readExternal(@NonNull ObjectInput in, @NonNull T target, long sequence)
            throws IOException, ClassNotFoundException;

        Object readResolve();

        @NonNull T readTarget(@NonNull DataInput in) throws IOException;
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull T target;
    private final long sequence;

    protected Message(final T target, final long sequence) {
        this.target = requireNonNull(target);
        this.sequence = sequence;
    }

    protected Message(final @NonNull C msg) {
        this(msg.target(), msg.sequence());
    }

    /**
     * Get the target identifier for this message.
     *
     * @return Target identifier
     */
    public final @NonNull T target() {
        return target;
    }

    /**
     * Get the logical sequence number.
     *
     * @return logical sequence number
     */
    public final long sequence() {
        return sequence;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    /**
     * Add attributes to the output of {@link #toString()}. Subclasses wanting to contribute additional information
     * should override this method. Any null attributes will be omitted from the output.
     *
     * @param toStringHelper a {@link ToStringHelper} instance
     * @return The {@link ToStringHelper} passed in as argument
     * @throws NullPointerException if toStringHelper is null
     */
    protected @NonNull ToStringHelper addToStringAttributes(final @NonNull ToStringHelper toStringHelper) {
        return toStringHelper.add("target", target).add("sequence", Long.toUnsignedString(sequence));
    }

    @java.io.Serial
    protected final Object writeReplace() {
        return toSerialForm();
    }

    /**
     * Instantiate a serialization proxy for this object.
     *
     * @return a serialization proxy for this object
     */
    protected abstract @NonNull SerialForm<T, C> toSerialForm();

    protected final void throwNSE() throws NotSerializableException {
        throw new NotSerializableException(getClass().getName());
    }

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
}
