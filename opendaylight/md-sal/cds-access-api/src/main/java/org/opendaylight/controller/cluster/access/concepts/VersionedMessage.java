/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.akka.queue.Message;
import org.opendaylight.controller.akka.queue.Message.SerialForm;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
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
 */
public interface VersionedMessage<T extends WritableIdentifier, C extends Message<T, C>> {
    /**
     * Return this message's version.
     *
     * @return this message's version
     */
    @NonNull ABIVersion version();

    /**
     * Return a message which will end up being serialized in the specified {@link ABIVersion}.
     *
     * @param toVersion Request {@link ABIVersion}
     * @return A new message which will use ABIVersion as its serialization.
     */
    default @NonNull C toVersion(final @NonNull ABIVersion toVersion) {
        final var version = version();
        if (version == toVersion) {
            @SuppressWarnings("unchecked")
            final var ret = (C) this;
            return ret;
        }

        return switch (toVersion) {
            case POTASSIUM -> verifyNotNull(cloneAsVersion(toVersion));
            default -> throw new IllegalArgumentException("Unhandled ABI version " + toVersion);
        };
    }

    /**
     * Create a copy of this message which will serialize to a stream corresponding to the specified method. This
     * method should be implemented by the concrete final message class only..
     *
     * @param targetVersion target ABI version
     * @return A message with the specified serialization stream
     * @throws IllegalArgumentException if this message does not support the target ABI
     */
    @NonNull C cloneAsVersion(@NonNull ABIVersion targetVersion);

    /**
     * Instantiate a serialization proxy for this object for the target ABI version. Implementations should return
     * different objects for incompatible {@link ABIVersion}s. This method should never fail, as any compatibility
     * checks should have been done by {@link #cloneAsVersion(ABIVersion)}.
     *
     * @param reqVersion Requested ABI version
     * @return Proxy for this object
     */
    @NonNull SerialForm<T, C> externalizableProxy(@NonNull ABIVersion reqVersion);
}
