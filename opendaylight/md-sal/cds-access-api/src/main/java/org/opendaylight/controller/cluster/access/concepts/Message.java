/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.io.Serializable;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * An abstract concept of a Message. This class cannot be instantiated directly, use its specializations {@link Request}
 * and {@link Response}.
 *
 * Messages have a target and a sequence number. Sequence numbers are expected to be assigned monotonically on a
 * per-target basis, hence two targets can observe the same sequence number.
 *
 * This class includes explicit versioning for forward- and backward- compatibility of serialization format. This is
 * achieved by using the serialization proxy pattern. Subclasses are in complete control of what proxy is used to
 * serialize a particular object on the wire. This class can serve as an explicit version marker, hence no further
 * action is necessary in the deserialization path.
 *
 * For the serialization path an explicit call from the user is required to select the appropriate serialization
 * version. This is done via {@link #toABIVersion(ABIVersion)} method, which should return a copy of this object with
 * the requested ABI version recorded and should return the appropriate serialization proxy.
 *
 * This workflow allows least disturbance across ABI versions, as all messages not affected by a ABI version bump
 * will remain working with the same serialization format for the new ABI version.
 *
 * Note that this class specifies the {@link Immutable} contract, which means that all subclasses must follow this API
 * contract.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message class type
 */
@Beta
public abstract class Message<T extends Identifier & WritableObject, C extends Message<T, C>> implements Immutable,
        Serializable {
    private static final long serialVersionUID = 1L;
    private final T target;
    private final long sequence;

    Message(final T identifier, final long sequence) {
        // Hidden to force use of subclasses
        this.target = Preconditions.checkNotNull(identifier);
        this.sequence = sequence;
    }

    /**
     * Get the target identifier for this message.
     *
     * @return Target identifier
     */
    public final @Nonnull T getTarget() {
        return target;
    }

    /**
     * Get the message sequence of this message.
     *
     * @return Message sequence
     */
    public final long getSequence() {
        return sequence;
    }

    /**
     * Return a message which will end up being serialized in the specified {@link ABIVersion}.
     *
     * @param version Request {@link ABIVersion}
     * @return A new message which will use ABIVersion as its serialization.
     */
    public final Message<T, C> toABIVersion(final @Nonnull ABIVersion version) {
        switch (version) {
            case BORON:
                return Verify.verifyNotNull(toBoronVersion());
            case TEST_PAST_VERSION:
            case TEST_FUTURE_VERSION:
            default:
                throw new IllegalArgumentException("Unhandled ABI version " + version);
        }
    }

    /**
     * Create a copy of this message which will serialize to a stream corresponding to {@link ABIVersion#BORON}. The
     * default implementation returns this object and is appropriate for messages which have only a single stream.
     *
     * Subclasses with more than one ABI version are expected to override this method.
     *
     * @return A message with {@link ABIVersion#BORON} serialization stream
     * @throws IllegalArgumentException if this message does not support the target ABI
     */
    protected Message<T, C> toBoronVersion() {
        return this;
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
    protected @Nonnull ToStringHelper addToStringAttributes(final @Nonnull ToStringHelper toStringHelper) {
        return toStringHelper.add("target", target).add("sequence", Long.toUnsignedString(sequence, 16));
    }

    /**
     * Instantiate a an Externalizable proxy to serializa this object.
     *
     * @return Proxy for this object
     */
    abstract @Nonnull AbstractMessageProxy<T, C> externalizableProxy();

    protected final Object writeReplace() {
        return externalizableProxy();
    }
}
