/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.akka.queue.RequestException;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A failure response to a {@link Request}. Contains a {@link RequestException} detailing the cause for this failure.
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
public abstract class RequestFailure<T extends WritableIdentifier, C extends RequestFailure<T, C>>
        extends org.opendaylight.controller.akka.queue.RequestFailure<T, C> implements VersionedMessage<T, C> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ABIVersion version;

    protected RequestFailure(final @NonNull T target, final long sequence, final @NonNull RequestException cause) {
        super(target, sequence, cause);
        version = ABIVersion.current();
    }

    protected RequestFailure(final @NonNull C failure, final @NonNull ABIVersion version) {
        super(failure);
        this.version = requireNonNull(version);
    }

    @Override
    public final ABIVersion version() {
        return version;
    }

    /**
     * Return the failure cause.
     *
     * @return Failure cause.
     * @deprecated Use {@link #cause()} instead.
     */
    @Deprecated(since = "9.0.0", forRemoval = true)
    public final @NonNull RequestException getCause() {
        return cause();
    }

    @Override
    public final SerialForm<T, C> toSerialForm() {
        return externalizableProxy(version);
    }

    @Override
    public abstract SerialForm<T, C> externalizableProxy(@NonNull ABIVersion reqVersion);
}
