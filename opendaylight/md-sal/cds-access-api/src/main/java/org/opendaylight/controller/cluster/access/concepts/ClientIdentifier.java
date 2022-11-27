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
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.cds.types.rev191024.ClientGeneration;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * A cluster-wide unique identifier of a frontend instance. This identifier discerns between individual incarnations
 * of a particular frontend.
 *
 * @author Robert Varga
 */
public final class ClientIdentifier implements WritableIdentifier {
    private static final class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private FrontendIdentifier frontendId;
        private long generation;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final FrontendIdentifier frontendId, final long generation) {
            this.frontendId = requireNonNull(frontendId);
            this.generation = generation;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            frontendId.writeTo(out);
            WritableObjects.writeLong(out, generation);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            frontendId = FrontendIdentifier.readFrom(in);
            generation = WritableObjects.readLong(in);
        }

        @java.io.Serial
        private Object readResolve() {
            return new ClientIdentifier(frontendId, generation);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull FrontendIdentifier frontendId;
    private final long generation;

    ClientIdentifier(final FrontendIdentifier frontendId, final long generation) {
        this.frontendId = requireNonNull(frontendId);
        this.generation = generation;
    }

    public static @NonNull ClientIdentifier create(final FrontendIdentifier frontendId,
            final long generation) {
        return new ClientIdentifier(frontendId, generation);
    }

    public static @NonNull ClientIdentifier readFrom(final DataInput in) throws IOException {
        final FrontendIdentifier frontendId = FrontendIdentifier.readFrom(in);
        return new ClientIdentifier(frontendId, WritableObjects.readLong(in));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        frontendId.writeTo(out);
        WritableObjects.writeLong(out, generation);
    }

    public @NonNull FrontendIdentifier getFrontendId() {
        return frontendId;
    }

    public long getGeneration() {
        return generation;
    }

    public @NonNull ClientGeneration getYangGeneration() {
        return new ClientGeneration(Uint64.fromLongBits(generation));
    }

    @Override
    public int hashCode() {
        return frontendId.hashCode() * 31 + Long.hashCode(generation);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof ClientIdentifier other && generation == other.generation
            && frontendId.equals(other.frontendId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ClientIdentifier.class)
            .add("frontend", frontendId)
            .add("generation", Long.toUnsignedString(generation))
            .toString();
    }

    @java.io.Serial
    private Object writeReplace() {
        return new Proxy(frontendId, generation);
    }
}
