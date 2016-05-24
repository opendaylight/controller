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
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * A cluster-wide unique identifier of a frontend instance. This identifier discerns between individual incarnations
 * of a particular frontend.
 *
 * @author Robert Varga
 */
@Beta
public final class ClientIdentifier implements Identifier, WritableObject {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private FrontendIdentifier frontendId;
        private long generation;

        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final FrontendIdentifier frontendId, final long generation) {
            this.frontendId = Preconditions.checkNotNull(frontendId);
            this.generation = generation;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            frontendId.writeTo(out);
            out.writeLong(generation);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            frontendId = FrontendIdentifier.readFrom(in);
            generation = in.readLong();
        }

        private Object readResolve() {
            return new ClientIdentifier(frontendId, generation);
        }
    }

    private static final long serialVersionUID = 1L;
    private final FrontendIdentifier frontendId;
    private final long generation;

    ClientIdentifier(final FrontendIdentifier frontendId, final long generation) {
        this.frontendId = Preconditions.checkNotNull(frontendId);
        this.generation = generation;
    }

    public static ClientIdentifier create(final FrontendIdentifier frontendId,
            final long generation) {
        return new ClientIdentifier(frontendId, generation);
    }

    public static ClientIdentifier readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
        final FrontendIdentifier frontendId = FrontendIdentifier.readFrom(in);
        return new ClientIdentifier(frontendId, in.readLong());
    }

    @Override
    public void writeTo(ObjectOutput out) throws IOException {
        frontendId.writeTo(out);
        out.writeLong(generation);
    }

    public FrontendIdentifier getFrontendId() {
        return frontendId;
    }

    public long getGeneration() {
        return generation;
    }

    @Override
    public int hashCode() {
        return frontendId.hashCode() * 31 + Long.hashCode(generation);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientIdentifier)) {
            return false;
        }

        final ClientIdentifier other = (ClientIdentifier) o;
        return generation == other.generation && frontendId.equals(other.frontendId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ClientIdentifier.class).add("frontend", frontendId)
                .add("generation", Long.toUnsignedString(generation)).toString();
    }

    private Object writeReplace() {
        return new Proxy(frontendId, generation);
    }
}
