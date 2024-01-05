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
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A successful reply to a {@link Request}.
 *
 * @param <T> Target identifier type
 */
public abstract class RequestSuccess<T extends WritableIdentifier, C extends RequestSuccess<T, C>>
        extends org.opendaylight.controller.akka.queue.RequestSuccess<T, C> implements VersionedMessage<T, C> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ABIVersion version;

    protected RequestSuccess(final @NonNull T target, final long sequence) {
        super(target, sequence);
        version = ABIVersion.current();
    }

    protected RequestSuccess(final @NonNull C success, final @NonNull ABIVersion version) {
        super(success);
        this.version = requireNonNull(version);
    }

    @Override
    public final ABIVersion version() {
        return version;
    }

    @Override
    public final SerialForm<T, C> toSerialForm() {
        return externalizableProxy(version);
    }

    @Override
    public abstract SerialForm<T, C> externalizableProxy(@NonNull ABIVersion reqVersion);
}
