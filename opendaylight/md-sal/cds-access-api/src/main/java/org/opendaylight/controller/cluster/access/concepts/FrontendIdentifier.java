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
public final class FrontendIdentifier implements Comparable<FrontendIdentifier>, Identifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private MemberName memberName;
        private long generation;

        public Proxy() {
            // Needed for Externalizable
        }

        Proxy(final MemberName memberName, final long generation) {
            this.memberName = Preconditions.checkNotNull(memberName);
            this.generation = generation;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(memberName.getName());
            out.writeLong(generation);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            final String str = (String) in.readObject();
            memberName = MemberName.forName(str);
            generation = in.readLong();
        }

        @SuppressWarnings("unused")
        private FrontendIdentifier readResolve() {
            return new FrontendIdentifier(memberName, generation);
        }
    }

    private static final long serialVersionUID = 1L;
    private final MemberName memberName;
    private final long generation;

    FrontendIdentifier(final MemberName memberName, long generation) {
        this.memberName = Preconditions.checkNotNull(memberName);
        this.generation = generation;
    }

    public static FrontendIdentifier create(final MemberName memberName, final long generation) {
        return new FrontendIdentifier(memberName, generation);
    }

    public MemberName getMemberName() {
        return memberName;
    }

    public long getGeneration() {
        return generation;
    }

    @Override
    public int hashCode() {
        return memberName.hashCode() * 31 + Long.hashCode(generation);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontendIdentifier)) {
            return false;
        }

        final FrontendIdentifier other = (FrontendIdentifier) o;
        return generation == other.generation && memberName.equals(other.memberName);
    }

    @Override
    public int compareTo(final FrontendIdentifier o) {
        final int cmp = memberName.compareTo(o.memberName);
        return cmp == 0 ? Long.compareUnsigned(generation, o.generation) : cmp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendIdentifier.class).add("memberName", memberName)
                .add("generation", Long.toUnsignedString(generation)).toString();
    }

    @SuppressWarnings("unused")
    private Proxy writeReplace() {
        return new Proxy(memberName, generation);
    }
}
