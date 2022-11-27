/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
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
 * This class includes explicit versioning for forward- and backward- compatibility of serialization format. This is
 * achieved by using the serialization proxy pattern. Subclasses are in complete control of what proxy is used to
 * serialize a particular object on the wire. This class can serve as an explicit version marker, hence no further
 * action is necessary in the deserialization path.
 *
 * <p>
 * For the serialization path an explicit call from the user is required to select the appropriate serialization
 * version. This is done via {@link #toVersion(ABIVersion)} method, which should return a copy of this object with
 * the requested ABI version recorded and should return the appropriate serialization proxy.
 *
 * <p>
 * This workflow allows least disturbance across ABI versions, as all messages not affected by a ABI version bump
 * will remain working with the same serialization format for the new ABI version.
 *
 * <p>
 * Note that this class specifies the {@link Immutable} contract, which means that all subclasses must follow this API
 * contract.
 *
 * @param <T> Target identifier type
 * @param <C> Message type
 */
public abstract class Message<T extends WritableIdentifier, C extends Message<T, C>>
        implements Immutable, Serializable {
    /**
     * Externalizable proxy for use with {@link Message} subclasses.
     *
     * @param <T> Target identifier type
     * @param <C> Message class
     */
    protected interface SerialForm<T extends WritableIdentifier, C extends Message<T, C>> extends Externalizable {

        @NonNull C message();

        void resolveTo(C newMessage);

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            final var message = message();
            message.getTarget().writeTo(out);
            WritableObjects.writeLong(out, message.getSequence());
            writeExternal(out, message);
        }

        void writeExternal(@NonNull ObjectOutput out, @NonNull C msg) throws IOException;

        @Override
        default void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final var target = verifyNotNull(readTarget(in));
            final var sequence = WritableObjects.readLong(in);
            resolveTo(verifyNotNull(readExternal(in, target, sequence)));
        }

        @NonNull C readExternal(@NonNull ObjectInput in, @NonNull T target, long sequence)
            throws IOException, ClassNotFoundException;

        Object readResolve();

        @NonNull T readTarget(@NonNull DataInput in) throws IOException;
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ABIVersion version;
    private final long sequence;
    private final @NonNull T target;

    private Message(final ABIVersion version, final T target, final long sequence) {
        this.target = requireNonNull(target);
        this.version = requireNonNull(version);
        this.sequence = sequence;
    }

    Message(final T target, final long sequence) {
        this(ABIVersion.current(), target, sequence);
    }

    Message(final C msg, final ABIVersion version) {
        this(version, msg.getTarget(), msg.getSequence());
    }

    /**
     * Get the target identifier for this message.
     *
     * @return Target identifier
     */
    public final @NonNull T getTarget() {
        return target;
    }

    /**
     * Get the logical sequence number.
     *
     * @return logical sequence number
     */
    public final long getSequence() {
        return sequence;
    }

    @VisibleForTesting
    public final @NonNull ABIVersion getVersion() {
        return version;
    }

    /**
     * Return a message which will end up being serialized in the specified {@link ABIVersion}.
     *
     * @param toVersion Request {@link ABIVersion}
     * @return A new message which will use ABIVersion as its serialization.
     */
    @SuppressWarnings("unchecked")
    public final @NonNull C toVersion(final @NonNull ABIVersion toVersion) {
        if (this.version == toVersion) {
            return (C)this;
        }

        return switch (toVersion) {
            case BORON, NEON_SR2, SODIUM_SR1, MAGNESIUM -> verifyNotNull(cloneAsVersion(toVersion));
            default -> throw new IllegalArgumentException("Unhandled ABI version " + toVersion);
        };
    }

    /**
     * Create a copy of this message which will serialize to a stream corresponding to the specified method. This
     * method should be implemented by the concrete final message class and should invoke the equivalent of
     * {@link #Message(Message, ABIVersion)}.
     *
     * @param targetVersion target ABI version
     * @return A message with the specified serialization stream
     * @throws IllegalArgumentException if this message does not support the target ABI
     */
    protected abstract @NonNull C cloneAsVersion(@NonNull ABIVersion targetVersion);

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

    /**
     * Instantiate a serialization proxy for this object for the target ABI version. Implementations should return
     * different objects for incompatible {@link ABIVersion}s. This method should never fail, as any compatibility
     * checks should have been done by {@link #cloneAsVersion(ABIVersion)}.
     *
     * @param reqVersion Requested ABI version
     * @return Proxy for this object
     */
    protected abstract @NonNull SerialForm<T, C> externalizableProxy(@NonNull ABIVersion reqVersion);

    @Serial
    protected final Object writeReplace() {
        return externalizableProxy(version);
    }
}
