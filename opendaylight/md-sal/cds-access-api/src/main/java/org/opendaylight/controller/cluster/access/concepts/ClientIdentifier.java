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
 * A cluster-wide unique identifier of a frontend instance.
 */
@Beta
public final class ClientIdentifier implements Identifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private Identifier agent;
        private long generation;

        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final Identifier agent, final long generation) {
            this.agent = Preconditions.checkNotNull(agent);
            this.generation = generation;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(agent);
            out.writeLong(generation);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            agent = (Identifier) in.readObject();
            generation = in.readLong();
        }

        private Object readResolve() {
            return new ClientIdentifier(agent, generation);
        }
    }

    private static final long serialVersionUID = 1L;
    private final Identifier agent;
    private final long generation;

    ClientIdentifier(final Identifier agent, final long generation) {
        this.agent = Preconditions.checkNotNull(agent);
        this.generation = generation;
    }

    public static ClientIdentifier create(final Identifier agent, final long generation) {
        return new ClientIdentifier(agent, generation);
    }

    public Identifier getAgent() {
        return agent;
    }

    public long getGeneration() {
        return generation;
    }

    @Override
    public int hashCode() {
        return agent.hashCode() * 31 + Long.hashCode(generation);
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
        return generation == other.generation && agent.equals(other.agent);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ClientIdentifier.class).add("agent", agent)
                .add("generation", Long.toUnsignedString(generation)).toString();
    }

    private Object writeReplace() {
        return new Proxy(agent, generation);
    }
}
