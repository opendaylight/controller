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
public final class ClientIdentifier<T extends FrontendType> implements Identifier {
    private static final class Proxy<T extends FrontendType> implements Externalizable {
        private static final long serialVersionUID = 1L;
        private FrontendIdentifier<T> frontendId;
        private long generation;

        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final FrontendIdentifier<T> frontendId, final long generation) {
            this.frontendId = Preconditions.checkNotNull(frontendId);
            this.generation = generation;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(frontendId);
            out.writeLong(generation);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            frontendId = (FrontendIdentifier<T>) in.readObject();
            generation = in.readLong();
        }

        private Object readResolve() {
            return new ClientIdentifier<>(frontendId, generation);
        }
    }

    private static final long serialVersionUID = 1L;
    private final FrontendIdentifier<T> frontendId;
    private final long generation;

    ClientIdentifier(final FrontendIdentifier<T> frontendId, final long generation) {
        this.frontendId = Preconditions.checkNotNull(frontendId);
        this.generation = generation;
    }

    public static <T extends FrontendType> ClientIdentifier<T> create(final FrontendIdentifier<T> frontendId,
            final long generation) {
        return new ClientIdentifier<>(frontendId, generation);
    }

    public FrontendIdentifier<T> getFrontendId() {
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

        final ClientIdentifier<?> other = (ClientIdentifier<?>) o;
        return generation == other.generation && frontendId.equals(other.frontendId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ClientIdentifier.class).add("frontend", frontendId)
                .add("generation", Long.toUnsignedString(generation)).toString();
    }

    private Object writeReplace() {
        return new Proxy<>(frontendId, generation);
    }
}
