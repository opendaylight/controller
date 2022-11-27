/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A failure response to a {@link Request}. Contains a {@link RequestException} detailing the cause for this failure.
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
public abstract class RequestFailure<T extends WritableIdentifier, C extends RequestFailure<T, C>>
        extends Response<T, C> {
    /**
     * Externalizable proxy for use with {@link RequestFailure} subclasses.
     *
     * @param <T> Target identifier type
     */
    protected interface SerialForm<T extends WritableIdentifier, C extends RequestFailure<T, C>>
            extends Message.SerialForm<T, C> {
        @Override
        default C readExternal(final ObjectInput in, final T target, final long sequence)
                throws IOException, ClassNotFoundException {
            return createFailure(target, sequence, (RequestException) in.readObject());
        }

        @Override
        default void writeExternal(final ObjectOutput out, final C msg) throws IOException {
            out.writeObject(msg.getCause());
        }

        @NonNull C createFailure(@NonNull T target, long sequence, @NonNull RequestException failureCause);
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull RequestException cause;

    protected RequestFailure(final @NonNull C failure, final @NonNull ABIVersion version) {
        super(failure, version);
        this.cause = requireNonNull(failure.getCause());
    }

    protected RequestFailure(final @NonNull T target, final long sequence, final @NonNull RequestException cause) {
        super(target, sequence);
        this.cause = requireNonNull(cause);
    }

    /**
     * Return the failure cause.
     *
     * @return Failure cause.
     */
    public final @NonNull RequestException getCause() {
        return cause;
    }

    /**
     * Return an indication of whether this a hard failure. Hard failures must not be retried but need to be treated
     * as authoritative response to a request.
     *
     * @return True if this represents a hard failure, false otherwise.
     */
    public final boolean isHardFailure() {
        return !cause.isRetriable();
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("cause", cause);
    }

    @Override
    protected abstract SerialForm<T, C> externalizableProxy(ABIVersion version);
}
