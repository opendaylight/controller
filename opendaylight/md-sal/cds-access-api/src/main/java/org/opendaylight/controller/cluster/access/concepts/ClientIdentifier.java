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

import com.google.common.annotations.Beta;
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
 */
@Beta
public final class ClientIdentifier implements WritableIdentifier {
    interface SerialForm extends Externalizable {
        @NonNull ClientIdentifier identifier();

        void setIdentifier(@NonNull ClientIdentifier identifier);

        Object readResolve();

        @Override
        default void readExternal(final ObjectInput in) throws IOException {
            setIdentifier(new ClientIdentifier(FrontendIdentifier.readFrom(in), WritableObjects.readLong(in)));
        }

        @Override
        default void writeExternal(final ObjectOutput out) throws IOException {
            final var id = identifier();
            id.getFrontendId().writeTo(out);
            WritableObjects.writeLong(out, id.getGeneration());
        }
    }

    private static final class Proxy implements SerialForm {
        private static final long serialVersionUID = 1L;

        private ClientIdentifier identifier;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final ClientIdentifier identifier) {
            this.identifier = requireNonNull(identifier);
        }

        @Override
        public ClientIdentifier identifier() {
            return verifyNotNull(identifier);
        }

        @Override
        public void setIdentifier(final ClientIdentifier identifier) {
            this.identifier = requireNonNull(identifier);
        }

        @Override
        public Object readResolve() {
            return identifier();
        }
    }

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
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClientIdentifier)) {
            return false;
        }

        final ClientIdentifier other = (ClientIdentifier) obj;
        return generation == other.generation && frontendId.equals(other.frontendId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ClientIdentifier.class).add("frontend", frontendId)
                .add("generation", Long.toUnsignedString(generation)).toString();
    }

    private Object writeReplace() {
        return new Proxy(this);
    }
}
